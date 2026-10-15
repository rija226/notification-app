package hr.podsjetnik.domain

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class StatusKind { OVERDUE, TODAY, UPCOMING, LATER, DONE }

/**
 * Status of an occurrence (PRD §5.4). [days] = days late for OVERDUE (0 = "Rok istekao"),
 * days until the due date for UPCOMING / LATER, 0 otherwise.
 */
data class OccurrenceState(val kind: StatusKind, val days: Int = 0)

fun statusOf(occurrence: Occurrence, dueTime: LocalTime?, now: Instant, zone: ZoneId): OccurrenceState {
    if (occurrence.status != OccurrenceStatus.OPEN) return OccurrenceState(StatusKind.DONE)
    val today = now.atZone(zone).toLocalDate()
    if (now >= overdueMoment(occurrence.dueDate, dueTime, zone)) {
        return OccurrenceState(StatusKind.OVERDUE, ChronoUnit.DAYS.between(occurrence.dueDate, today).toInt())
    }
    val daysUntil = ChronoUnit.DAYS.between(today, occurrence.dueDate).toInt()
    return when (daysUntil) {
        0 -> OccurrenceState(StatusKind.TODAY)
        in 1..7 -> OccurrenceState(StatusKind.UPCOMING, daysUntil)
        else -> OccurrenceState(StatusKind.LATER, daysUntil)
    }
}
