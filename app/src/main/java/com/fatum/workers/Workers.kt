package com.fatum.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.fatum.data.db.FatumDatabase
import com.fatum.data.db.dao.HabitDao
import com.fatum.data.repository.HabitRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// ─────────────────────────────────────────────────────────────────────────────
// BackupWorker  –  RNF-3 / RNF-4 / RNF-5
//
// Runs nightly at 03:00.
// 1. Copies Room's SQLite file to a temporary path.
// 2. Compresses it with ZIP.
// 3. Uploads the ZIP to Google Drive appDataFolder.
// ─────────────────────────────────────────────────────────────────────────────
@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "fatum_nightly_backup"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val dbFile = applicationContext.getDatabasePath(FatumDatabase.DATABASE_NAME)
            if (!dbFile.exists()) return@withContext Result.failure()

            // ── Step 1: Copy DB to cache dir ───────────────────────────
            val backupDir = File(applicationContext.cacheDir, "backup").also { it.mkdirs() }
            val zipFile   = File(backupDir, "fatum_backup.zip")

            // ── Step 2: Compress (RNF-4) ───────────────────────────────
            ZipOutputStream(zipFile.outputStream().buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(FatumDatabase.DATABASE_NAME))
                FileInputStream(dbFile).copyTo(zip)
                zip.closeEntry()

                // Also include WAL and SHM if present
                listOf("-wal", "-shm").forEach { suffix ->
                    val extra = File(dbFile.path + suffix)
                    if (extra.exists()) {
                        zip.putNextEntry(ZipEntry(extra.name))
                        FileInputStream(extra).copyTo(zip)
                        zip.closeEntry()
                    }
                }
            }

            // ── Step 3: Upload to Google Drive appDataFolder (RNF-5) ───
            // NOTE: Actual Drive upload requires a valid Google account token.
            // The DriveUploader helper (below) handles the REST call.
            // If no account is signed in, the backup is kept locally only.
            DriveUploader.uploadBackup(applicationContext, zipFile)

            Result.success()
        } catch (e: Exception) {
            // Retry up to 3 times with exponential back-off
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DriveUploader  –  RNF-5
// Helper that uses the Google Drive REST API v3 to upload / overwrite
// the backup file in the app's private appDataFolder.
// ─────────────────────────────────────────────────────────────────────────────
object DriveUploader {

    /**
     * Uploads [zipFile] to Google Drive appDataFolder.
     * Overwrites the previous backup to preserve storage quota (RNF-5).
     *
     * Requires [com.google.android.gms.auth.api.signin.GoogleSignIn] to be
     * completed in the app before this is called.
     */
    suspend fun uploadBackup(context: Context, zipFile: File) = withContext(Dispatchers.IO) {
        // ── Retrieve stored OAuth token via AccountManager ─────────────
        val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)?.email
            ?: return@withContext  // Not signed in; skip cloud backup

        val credential = com.google.api.client.googleapis.extensions.android.gms.auth
            .GoogleAccountCredential.usingOAuth2(
                context,
                listOf("https://www.googleapis.com/auth/drive.appdata")
            ).also { it.selectedAccountName = account }

        val transport  = com.google.api.client.http.javanet.NetHttpTransport()
        val jsonFactory = com.google.api.client.json.gson.GsonFactory.getDefaultInstance()

        val driveService = com.google.api.services.drive.Drive.Builder(transport, jsonFactory, credential)
            .setApplicationName("FATUM")
            .build()

        val fileMetadata = com.google.api.services.drive.model.File().apply {
            name    = "fatum_backup.zip"
            parents = listOf("appDataFolder")
        }

        val mediaContent = com.google.api.client.http.FileContent("application/zip", zipFile)

        // Check if a previous backup exists and delete it first (overwrite)
        val existing = driveService.files().list()
            .setSpaces("appDataFolder")
            .setQ("name='fatum_backup.zip'")
            .setFields("files(id)")
            .execute()
            .files
            .firstOrNull()

        if (existing != null) {
            driveService.files().delete(existing.id).execute()
        }

        driveService.files().create(fileMetadata, mediaContent)
            .setFields("id")
            .execute()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// StreakCheckWorker  –  RF-2.4
//
// Runs at 22:00. Checks which DAILY habits have NOT been completed today
// and fires a local push notification for each one.
// ─────────────────────────────────────────────────────────────────────────────
@HiltWorker
class StreakCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val habitDao: HabitDao
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME    = "fatum_streak_check"
        const val CHANNEL_ID   = "fatum_streak_channel"
    }

    override suspend fun doWork(): Result {
        val today   = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val habits  = habitDao.observeActive().first()

        createNotificationChannel()

        habits.filter { it.frequencyType == "DAILY" }.forEach { habit ->
            val done = habitDao.getExecution(habit.id, today)
            if (done == null && habit.currentStreak > 0) {
                // Streak in danger – fire notification (RF-2.4)
                sendStreakNotification(habit.id, habit.name, habit.currentStreak)
            }
        }
        return Result.success()
    }

    private fun sendStreakNotification(habitId: Int, habitName: String, streak: Int) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("⚠️ Racha en peligro: $habitName")
            .setContentText("Llevas $streak días seguidos. ¡No pierdas tu racha hoy!")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(habitId + 2000, notif)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas de racha",
            NotificationManager.IMPORTANCE_HIGH
        )
        (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BootReceiver
// Re-schedules WorkManager periodic jobs after device reboot.
// ─────────────────────────────────────────────────────────────────────────────
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // FatumApplication.scheduleRecurringWork() uses KEEP policy,
            // so calling it again after reboot is safe and idempotent.
            WorkManager.getInstance(context).apply {
                // Re-enqueue backup
                val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
                enqueueUniquePeriodicWork(BackupWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, backupRequest)

                // Re-enqueue streak check
                val streakRequest = PeriodicWorkRequestBuilder<StreakCheckWorker>(24, TimeUnit.HOURS).build()
                enqueueUniquePeriodicWork(StreakCheckWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, streakRequest)
            }
        }
    }
}
