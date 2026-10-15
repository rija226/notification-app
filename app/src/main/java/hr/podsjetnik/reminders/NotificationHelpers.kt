package hr.podsjetnik.reminders

import hr.podsjetnik.domain.Occurrence
import hr.podsjetnik.domain.Settings
import hr.podsjetnik.domain.Task
import hr.podsjetnik.domain.nextReminderAt
import hr.podsjetnik.domain.overdueIntervalHours
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Which morning summary title to use (PRD FR-6). */
enum class SummaryVariant { TODAY_AND_OVERDUE, TODAY_ONLY, OVERDUE_ONLY }

fun summaryVariant(dueToday: Int, overdue: Int): SummaryVariant = when {
    dueToday > 0 && overdue > 0 -> SummaryVariant.TODAY_AND_OVERDUE
    dueToday > 0 -> SummaryVariant.TODAY_ONLY
    else -> SummaryVariant.OVERDUE_ONLY
}

/** Overdue interval in hours valid on [today], for "Podsjećam svaka 2 sata …" (halves from day 3). */
fun currentOverdueInterval(task: Task, occurrence: Occurrence, today: LocalDate): Int =
    overdueIntervalHours(task.overdueIntervalHours, ChronoUnit.DAYS.between(occurrence.dueDate, today))

/**
 * Next reminder later today after a post at [notifiedAt], for "Sljedeći podsjetnik u 13:00 …";
 * null after the last one of the day (HANDOFF §5.3).
 */
fun nextReminderToday(task: Task, occurrence: Occurrence, settings: Settings, notifiedAt: Instant, zone: ZoneId): LocalTime? {
    val next = nextReminderAt(task, occurrence.copy(lastNotifiedAt = notifiedAt), settings, notifiedAt, zone) ?: return null
    val local = next.atZone(zone)
    return local.toLocalTime().takeIf { local.toLocalDate() == notifiedAt.atZone(zone).toLocalDate() }
}

/** First letter lower-cased, for rule text inside a sentence ("… (zadnji dan u mjesecu)."). */
fun lowerFirst(text: String): String = text.replaceFirstChar { it.lowercase() }
