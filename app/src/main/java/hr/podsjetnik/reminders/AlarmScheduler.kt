package hr.podsjetnik.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.Instant

/**
 * Holds THE single pending alarm of the app (PRD §8.1). Scheduling replaces the previous one,
 * because the PendingIntent is always the same (same receiver, same request code).
 * Mode A, setExactAndAllowWhileIdle, chosen in the Phase 1 spike (docs/spike-results.md).
 */
class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(at: Instant) {
        val millis = at.toEpochMilli()
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, alarmIntent())
        } else {
            // Should never happen with USE_EXACT_ALARM; degrade instead of crashing (PRD FR-9).
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, alarmIntent())
        }
    }

    fun cancel() {
        alarmManager.cancel(alarmIntent())
    }

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
