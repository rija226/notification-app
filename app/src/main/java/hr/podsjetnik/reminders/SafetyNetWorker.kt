package hr.podsjetnik.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import hr.podsjetnik.container
import java.util.concurrent.TimeUnit

/**
 * Safety net only (CLAUDE.md): every ~6 h re-run refresh in case the single alarm was lost.
 * WorkManager may run late; it never drives reminder timing.
 */
class SafetyNetWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        applicationContext.container.reminderEngine.refresh()
        return Result.success()
    }

    companion object {
        private const val NAME = "safety-net-refresh"

        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<SafetyNetWorker>(6, TimeUnit.HOURS).build(),
            )
        }
    }
}
