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
import com.fatum.data.db.entities.CalendarEventEntity
import com.fatum.data.repository.CalendarRepository
import com.fatum.data.repository.HabitRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// ─────────────────────────────────────────────────────────────────────────────
// CalendarSyncWorker  –  runs nightly at 02:30
// Pulls events from Google Calendar into Room.
// ─────────────────────────────────────────────────────────────────────────────
@HiltWorker
class CalendarSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val calendarRepo: CalendarRepository
) : CoroutineWorker(context, params) {

    companion object { const val WORK_NAME = "fatum_calendar_sync" }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
                ?: return@withContext Result.success() // not signed in — skip silently

            val cred = GoogleAccountCredential.usingOAuth2(
                applicationContext,
                listOf("https://www.googleapis.com/auth/calendar.readonly")
            ).apply { selectedAccount = account.account }

            val svc = Calendar.Builder(
                NetHttpTransport(), GsonFactory.getDefaultInstance(), cred
            ).setApplicationName("FATUM").build()

            val now   = DateTime(System.currentTimeMillis())
            val limit = DateTime(System.currentTimeMillis() + 90L * 86_400_000) // 90 days

            val items = svc.events().list("primary")
                .setMaxResults(500).setTimeMin(now).setTimeMax(limit)
                .setSingleEvents(true).setOrderBy("startTime")
                .execute().items ?: emptyList()

            val entities = items.mapNotNull { ev ->
                val s = ev.start?.dateTime?.value ?: ev.start?.date?.value ?: return@mapNotNull null
                val e = ev.end?.dateTime?.value   ?: ev.end?.date?.value   ?: return@mapNotNull null
                CalendarEventEntity(
                    gcalEventId    = ev.id,
                    title          = ev.summary ?: "(Sin título)",
                    description    = ev.description,
                    importance     = "MEDIUM",
                    startTimestamp = s, endTimestamp = e,
                    recurrenceRule = ev.recurrence?.firstOrNull(),
                    isFromGcal     = true
                )
            }
            calendarRepo.syncFromGcal(entities)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BackupWorker  –  runs nightly at 03:00
// Zips Room DB and uploads to Google Drive appDataFolder.
// ─────────────────────────────────────────────────────────────────────────────
@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object { const val WORK_NAME = "fatum_backup" }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val dbFile = applicationContext.getDatabasePath(FatumDatabase.DATABASE_NAME)
            if (!dbFile.exists()) return@withContext Result.failure()

            val dir = File(applicationContext.cacheDir, "backup").also { it.mkdirs() }
            val zip = File(dir, "fatum_backup.zip")

            ZipOutputStream(zip.outputStream().buffered()).use { z ->
                z.putNextEntry(ZipEntry(FatumDatabase.DATABASE_NAME))
                FileInputStream(dbFile).copyTo(z); z.closeEntry()
                listOf("-wal", "-shm").forEach { suffix ->
                    val f = File(dbFile.path + suffix)
                    if (f.exists()) { z.putNextEntry(ZipEntry(f.name)); FileInputStream(f).copyTo(z); z.closeEntry() }
                }
            }

            DriveUploader.uploadBackup(applicationContext, zip)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DriveUploader — shared by BackupWorker and ProfileViewModel
// ─────────────────────────────────────────────────────────────────────────────
object DriveUploader {

    suspend fun uploadBackup(context: Context, zipFile: File) = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext

        val cred = GoogleAccountCredential.usingOAuth2(
            context, listOf("https://www.googleapis.com/auth/drive.appdata")
        ).apply { selectedAccount = account.account }

        val transport   = NetHttpTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()
        val drive = com.google.api.services.drive.Drive.Builder(transport, jsonFactory, cred)
            .setApplicationName("FATUM").build()

        val meta = com.google.api.services.drive.model.File().apply {
            name    = "fatum_backup.zip"
            parents = listOf("appDataFolder")
        }
        val media = com.google.api.client.http.FileContent("application/zip", zipFile)

        // Delete previous backup to avoid quota accumulation
        drive.files().list().setSpaces("appDataFolder")
            .setQ("name='fatum_backup.zip'").setFields("files(id)")
            .execute().files?.forEach { drive.files().delete(it.id).execute() }

        drive.files().create(meta, media).setFields("id").execute()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// StreakCheckWorker  –  runs at 22:00
// Sends a push notification for any DAILY habit not yet completed today.
// ─────────────────────────────────────────────────────────────────────────────
@HiltWorker
class StreakCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val habitRepo: HabitRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME  = "fatum_streak_check"
        const val CHANNEL_ID = "fatum_streak_channel"
    }

    override suspend fun doWork(): Result {
        val today  = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val habits = habitRepo.observeActive().first()

        createChannel()

        habits.filter { it.frequencyType == "DAILY" }.forEach { habit ->
            val executions = habitRepo.observeExecutions(habit.id).first()
            val doneToday  = executions.any { it.dateCompleted == today &&
                it.valueLogged >= (if (habit.habitType == "VALUE") habit.valueTarget else 1) }

            if (!doneToday && habit.currentStreak > 0) {
                notify(habit.id, habit.name, habit.currentStreak)
            }
        }
        return Result.success()
    }

    private fun notify(id: Int, name: String, streak: Int) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id + 3000, NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("⚠️ Racha en peligro: $name")
            .setContentText("Llevas $streak días seguidos. ¡No pierdas la racha hoy!")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build())
    }

    private fun createChannel() {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Alertas de racha", NotificationManager.IMPORTANCE_HIGH))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BootReceiver  –  reschedules WorkManager after device reboot
// ─────────────────────────────────────────────────────────────────────────────
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val wm = WorkManager.getInstance(context)

        // Calendar sync 02:30
        wm.enqueueUniquePeriodicWork(
            CalendarSyncWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<CalendarSyncWorker>(24, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
        )
        // Drive backup 03:00
        wm.enqueueUniquePeriodicWork(
            BackupWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
        )
        // Streak check 22:00
        wm.enqueueUniquePeriodicWork(
            StreakCheckWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<StreakCheckWorker>(24, TimeUnit.HOURS).build()
        )
    }
}
