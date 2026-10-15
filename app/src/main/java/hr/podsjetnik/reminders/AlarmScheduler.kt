package hr.podsjetnik.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import hr.podsjetnik.MainActivity
import hr.podsjetnik.data.AlarmMode
import hr.podsjetnik.log
import java.time.Instant

/**
 * Holds THE single pending alarm of the app (PRD §8.1). Scheduling replaces the previous one,
 * because the PendingIntent is always the same (same receiver, same request code).
 */
class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(at: Instant, mode: AlarmMode = AlarmMode.EXACT_WHILE_IDLE) {
        val operation = alarmIntent()
        val millis = at.toEpochMilli()
        when {
            // Should never happen with USE_EXACT_ALARM; degrade instead of crashing (PRD FR-9).
            !alarmManager.canScheduleExactAlarms() ->
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, operation)
            // ponytail: ALARM_CLOCK exists only for the Phase 1 spike comparison (PRD §10.2)
            mode == AlarmMode.ALARM_CLOCK ->
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(millis, openAppIntent()), operation)
            else ->
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, operation)
        }
        log("alarm scheduled for $at ($mode)")
    }

    fun cancel() {
        alarmManager.cancel(alarmIntent())
        log("alarm cancelled")
    }

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE,
    )
}
