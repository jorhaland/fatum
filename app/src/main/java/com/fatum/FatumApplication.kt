package com.fatum

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.fatum.workers.BackupWorker
import com.fatum.workers.StreakCheckWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Entry point for the Hilt dependency injection graph.
 * Also schedules recurring WorkManager jobs on first launch.
 */
@HiltAndroidApp
class FatumApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleRecurringWork()
    }

    /**
     * Schedules:
     *  1. Nightly backup at 03:00 (RNF-3)
     *  2. Daily streak check + notification at 22:00 (RF-2.4)
     */
    private fun scheduleRecurringWork() {
        val workManager = WorkManager.getInstance(this)

        // ── Nightly backup ──────────────────────────────────────────────────
        val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateDelayUntil(3, 0), TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            BackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            backupRequest
        )

        // ── Daily streak check at 22:00 ─────────────────────────────────────
        val streakRequest = PeriodicWorkRequestBuilder<StreakCheckWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateDelayUntil(22, 0), TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            StreakCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            streakRequest
        )
    }

    /** Calculates milliseconds until the next occurrence of [hour]:[minute]. */
    private fun calculateDelayUntil(hour: Int, minute: Int): Long {
        val now = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) target.add(java.util.Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis - now.timeInMillis
    }
}
