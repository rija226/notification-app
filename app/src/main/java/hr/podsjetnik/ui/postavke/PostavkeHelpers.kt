package hr.podsjetnik.ui.postavke

import hr.podsjetnik.data.AlarmLogEntity
import hr.podsjetnik.reminders.DeviceStatus
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

/** Device/setup problems shown in Postavke › Dozvole (PRD FR-9). */
enum class Problem { NOTIFICATIONS, EXACT_ALARMS, BATTERY, SAMSUNG, LATE_ALARMS }

/** These stop reminders from arriving → red banner on Danas. The rest are warnings. */
val BLOCKING_PROBLEMS = setOf(Problem.NOTIFICATIONS, Problem.EXACT_ALARMS, Problem.BATTERY)

fun problemsOf(status: DeviceStatus, samsungConfirmed: Boolean, lateAlarms: Boolean): Set<Problem> = buildSet {
    if (!status.notificationsOk) add(Problem.NOTIFICATIONS)
    if (!status.exactAlarms) add(Problem.EXACT_ALARMS)
    if (!status.batteryExempt) add(Problem.BATTERY)
    if (!samsungConfirmed) add(Problem.SAMSUNG)
    if (lateAlarms) add(Problem.LATE_ALARMS)
}

private val LATE_THRESHOLD: Duration = Duration.ofMinutes(5)
private val LATE_WINDOW: Duration = Duration.ofDays(7)

/** Any alarm in the last 7 days that fired more than 5 min late (PRD FR-9 "Točnost alarma"). */
fun hasLateAlarm(log: List<AlarmLogEntity>, now: Instant): Boolean = log.any { entry ->
    val fired = entry.firedAt ?: return@any false
    entry.scheduledFor >= now - LATE_WINDOW && Duration.between(entry.scheduledFor, fired) > LATE_THRESHOLD
}

/** Quiet hours need start ≠ end (PRD §5.6). */
fun isValidQuietRange(from: LocalTime, to: LocalTime): Boolean = from != to
