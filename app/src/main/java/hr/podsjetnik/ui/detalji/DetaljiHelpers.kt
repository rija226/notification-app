package hr.podsjetnik.ui.detalji

import hr.podsjetnik.domain.OccurrenceState
import hr.podsjetnik.domain.RecurrenceRule
import hr.podsjetnik.domain.Settings
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.domain.TaskWithOccurrences
import hr.podsjetnik.domain.nextReminderAt
import hr.podsjetnik.domain.statusOf
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class OccurrenceRow(val id: Long, val dueDate: LocalDate, val state: OccurrenceState)

/** Detalji content (Phase 3 subset of HANDOFF §6.2). Open occurrences are oldest first; the first is "Trenutni rok". */
data class DetaljiUiState(
    val title: String,
    val type: TaskType,
    val rule: RecurrenceRule?,
    val dueTime: LocalTime?,
    val paused: Boolean,
    val openOccurrences: List<OccurrenceRow>,
    val nextReminder: LocalDateTime?,
    val today: LocalDate,
)

fun buildDetalji(item: TaskWithOccurrences, settings: Settings, now: Instant, zone: ZoneId): DetaljiUiState {
    val task = item.task
    val open = item.openOccurrences
    val next = open.mapNotNull { nextReminderAt(task, it, settings, now, zone) }.minOrNull()
    return DetaljiUiState(
        title = task.title,
        type = task.type,
        rule = task.recurrence,
        dueTime = task.dueTime,
        paused = task.pausedAt != null,
        openOccurrences = open.map { OccurrenceRow(it.id, it.dueDate, statusOf(it, task.dueTime, now, zone)) },
        nextReminder = next?.atZone(zone)?.toLocalDateTime(),
        today = now.atZone(zone).toLocalDate(),
    )
}
