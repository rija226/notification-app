package hr.podsjetnik.ui.danas

import hr.podsjetnik.domain.OccurrenceState
import hr.podsjetnik.domain.RecurrenceRule
import hr.podsjetnik.domain.Settings
import hr.podsjetnik.domain.StatusKind
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.domain.TaskWithOccurrences
import hr.podsjetnik.domain.nextReminderAt
import hr.podsjetnik.domain.statusOf
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** One card on Danas = one open occurrence (two open months of a task → two cards). */
data class DanasCard(
    val occurrenceId: Long,
    val taskId: Long,
    val title: String,
    val type: TaskType,
    val rule: RecurrenceRule?,
    val dueDate: LocalDate,
    val dueTime: LocalTime?,
    val state: OccurrenceState,
    val nextReminder: LocalDateTime?,
)

data class DanasSections(
    val overdue: List<DanasCard>,
    val today: List<DanasCard>,
    val upcoming: List<DanasCard>,
    /** Nearest future task for the empty state ("Sljedeće"), only when all sections are empty. */
    val next: DanasCard?,
) {
    val isEmpty get() = overdue.isEmpty() && today.isEmpty() && upcoming.isEmpty()
}

/**
 * Danas sections (HANDOFF §6.1, §4.2): Zakašnjelo most days late first, then title;
 * Danas by next reminder; Uskoro (1–7 days) by date then time. Paused tasks are hidden (PRD FR-7).
 */
fun buildDanas(items: List<TaskWithOccurrences>, settings: Settings, now: Instant, zone: ZoneId): DanasSections {
    val cards = items
        .filter { it.task.pausedAt == null && it.task.archivedAt == null }
        .flatMap { item ->
            item.openOccurrences.map { occurrence ->
                DanasCard(
                    occurrenceId = occurrence.id,
                    taskId = item.task.id,
                    title = item.task.title,
                    type = item.task.type,
                    rule = item.task.recurrence,
                    dueDate = occurrence.dueDate,
                    dueTime = item.task.dueTime,
                    state = statusOf(occurrence, item.task.dueTime, now, zone),
                    nextReminder = nextReminderAt(item.task, occurrence, settings, now, zone)?.atZone(zone)?.toLocalDateTime(),
                )
            }
        }
    val overdue = cards.filter { it.state.kind == StatusKind.OVERDUE }
        .sortedWith(compareByDescending<DanasCard> { it.state.days }.thenBy { it.title })
    val today = cards.filter { it.state.kind == StatusKind.TODAY }
        .sortedWith(compareBy<DanasCard, LocalDateTime?>(nullsLast()) { it.nextReminder }.thenBy { it.title })
    val upcoming = cards.filter { it.state.kind == StatusKind.UPCOMING }
        .sortedWith(compareBy<DanasCard> { it.dueDate }.thenBy(nullsFirst()) { it.dueTime })
    val next = if (overdue.isEmpty() && today.isEmpty() && upcoming.isEmpty()) {
        cards.filter { it.state.kind == StatusKind.LATER }.minByOrNull { it.dueDate }
    } else {
        null
    }
    return DanasSections(overdue, today, upcoming, next)
}
