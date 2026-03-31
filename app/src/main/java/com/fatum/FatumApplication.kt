package com.fatum

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.fatum.workers.BackupWorker
import com.fatum.workers.CalendarSyncWorker
import com.fatum.workers.StreakCheckWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class FatumApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        scheduleRecurringWork()
    }

    private fun scheduleRecurringWork() {
        val wm = WorkManager.getInstance(this)

        // ── Google Calendar sync at 02:30 ─────────────────────────────────────
        val calSync = PeriodicWorkRequestBuilder<CalendarSyncWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayUntil(2, 30), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        wm.enqueueUniquePeriodicWork(CalendarSyncWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, calSync)

        // ── Drive backup at 03:00 ────────────────────────────────────────────
        val backup = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayUntil(3, 0), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        wm.enqueueUniquePeriodicWork(BackupWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, backup)

        // ── Streak check at 22:00 ─────────────────────────────────────────────
        val streak = PeriodicWorkRequestBuilder<StreakCheckWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayUntil(22, 0), TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniquePeriodicWork(StreakCheckWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, streak)
    }

    private fun delayUntil(hour: Int, minute: Int): Long {
        val now    = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis - now.timeInMillis
    }
}
