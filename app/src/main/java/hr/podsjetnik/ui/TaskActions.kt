package hr.podsjetnik.ui

import hr.podsjetnik.data.TaskRepository
import hr.podsjetnik.domain.RecurrenceRule
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.reminders.ReminderEngine
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/** Content of the Odrađeno dialog for a recurring task (HANDOFF §6.4). */
data class DoneDialogState(
    val occurrenceId: Long,
    val title: String,
    val nextDue: LocalDate,
    val rule: RecurrenceRule?,
    val earlyAt: LocalDateTime?,
    val today: LocalDate,
)

/** What the UI shows after Odrađeno: the dialog (Stalni) or the archive snackbar (Povremeni, O8). */
sealed interface DoneOutcome {
    val occurrenceId: Long

    data class Recurring(val dialog: DoneDialogState) : DoneOutcome {
        override val occurrenceId get() = dialog.occurrenceId
    }

    data class Archived(override val occurrenceId: Long) : DoneOutcome
}

/** Odrađeno / Poništi / Odgodi from screens, shared by Danas and Detalji. All go through the engine. */
class TaskActions(
    private val engine: ReminderEngine,
    private val repository: TaskRepository,
    private val clock: Clock,
) {
    suspend fun done(occurrenceId: Long): DoneOutcome? {
        val (task, _) = repository.find(occurrenceId) ?: return null
        engine.onDone(occurrenceId, showUndo = false) // the screen has its own Poništi
        if (task.type == TaskType.ONE_OFF) return DoneOutcome.Archived(occurrenceId)
        val next = repository.openOccurrences(task.id).maxByOrNull { it.dueDate } ?: return null
        val earlyAt = if (task.earlyDays > 0) next.dueDate.minusDays(task.earlyDays.toLong()).atTime(task.earlyTime) else null
        return DoneOutcome.Recurring(
            DoneDialogState(occurrenceId, task.title, next.dueDate, task.recurrence, earlyAt, LocalDate.now(clock)),
        )
    }

    suspend fun undo(occurrenceId: Long) = engine.onUndo(occurrenceId)

    suspend fun snooze(occurrenceId: Long, until: LocalDateTime) =
        engine.snoozeUntil(occurrenceId, until.atZone(clock.zone).toInstant())
}
