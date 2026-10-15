package hr.podsjetnik.ui.svizadaci

import hr.podsjetnik.domain.OccurrenceState
import hr.podsjetnik.domain.RecurrenceRule
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.domain.TaskWithOccurrences
import hr.podsjetnik.domain.statusOf
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** One row in Svi zadaci. [dueDate]/[state] belong to the oldest open occurrence. */
data class TaskRow(
    val taskId: Long,
    val title: String,
    val type: TaskType,
    val rule: RecurrenceRule?,
    val dueDate: LocalDate?,
    val dueTime: LocalTime?,
    val state: OccurrenceState?,
    val paused: Boolean,
)

/** Arhiva row: a done one-off (HANDOFF §6.5). */
data class ArchiveRow(val taskId: Long, val title: String, val archivedOn: LocalDate)

/** Active (not archived) tasks of [type], sorted by due date ascending (HANDOFF §4.2). */
fun buildRows(items: List<TaskWithOccurrences>, type: TaskType, now: Instant, zone: ZoneId): List<TaskRow> =
    items
        .filter { it.task.type == type && it.task.archivedAt == null }
        .map { item ->
            val current = item.openOccurrences.firstOrNull()
            TaskRow(
                taskId = item.task.id,
                title = item.task.title,
                type = item.task.type,
                rule = item.task.recurrence,
                dueDate = current?.dueDate,
                dueTime = item.task.dueTime,
                state = current?.let { statusOf(it, item.task.dueTime, now, zone) },
                paused = item.task.pausedAt != null,
            )
        }
        .sortedWith(compareBy<TaskRow, LocalDate?>(nullsLast()) { it.dueDate }.thenBy { it.title })

/** Archived one-offs, newest first (HANDOFF §4.2). */
fun buildArchive(items: List<TaskWithOccurrences>, zone: ZoneId): List<ArchiveRow> =
    items
        .mapNotNull { item -> item.task.archivedAt?.let { item to it } }
        .sortedByDescending { it.second }
        .map { (item, archivedAt) -> ArchiveRow(item.task.id, item.task.title, archivedAt.atZone(zone).toLocalDate()) }
