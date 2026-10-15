package hr.podsjetnik.ui.uredi

import hr.podsjetnik.domain.RecurrenceRule
import hr.podsjetnik.domain.Settings
import hr.podsjetnik.domain.Task
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.domain.TaskWithOccurrences
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Form state for Novi / Uredi zadatak (HANDOFF §6.6). [taskId] null = new task. */
data class UrediForm(
    val taskId: Long? = null,
    val title: String = "",
    val type: TaskType = TaskType.RECURRING,
    val lastDay: Boolean = false,
    val day: Int,
    val date: LocalDate,
    val time: LocalTime? = null,
    val earlyDays: Int,
    val earlyTime: LocalTime,
    val dayTimes: List<LocalTime>,
    val overdueIntervalHours: Int,
    val note: String = "",
    val showErrors: Boolean = false,
)

/** New task, prefilled from settings defaults. */
fun initialForm(today: LocalDate, settings: Settings) = UrediForm(
    day = today.dayOfMonth.coerceAtMost(28),
    date = today,
    earlyDays = settings.defaultEarlyDays,
    earlyTime = settings.defaultEarlyTime,
    dayTimes = settings.defaultDayTimes,
    overdueIntervalHours = settings.defaultOverdueIntervalHours,
)

/** Existing task → form. A one-off's date is its open occurrence's due date. */
fun formFromTask(item: TaskWithOccurrences, today: LocalDate): UrediForm {
    val task = item.task
    val rule = task.recurrence
    return UrediForm(
        taskId = task.id,
        title = task.title,
        type = task.type,
        lastDay = rule == RecurrenceRule.LastDayOfMonth,
        day = (rule as? RecurrenceRule.DayOfMonth)?.day ?: today.dayOfMonth.coerceAtMost(28),
        date = item.openOccurrences.firstOrNull()?.dueDate ?: item.occurrences.maxOfOrNull { it.dueDate } ?: today,
        time = task.dueTime,
        earlyDays = task.earlyDays,
        earlyTime = task.earlyTime,
        dayTimes = task.dayTimes,
        overdueIntervalHours = task.overdueIntervalHours,
        note = task.note.orEmpty(),
    )
}

fun isTitleValid(title: String): Boolean = title.isNotBlank()

fun ruleOf(form: UrediForm): RecurrenceRule =
    if (form.lastDay) RecurrenceRule.LastDayOfMonth else RecurrenceRule.DayOfMonth(form.day)

/** "Sljedeći termin" shown live under the rule. */
fun nextDueDate(form: UrediForm, today: LocalDate): LocalDate =
    if (form.type == TaskType.RECURRING) ruleOf(form).firstOnOrAfter(today) else form.date

/** Form → task. Editing keeps the original's other fields (pause, archive, category, createdAt). */
fun buildTask(form: UrediForm, now: Instant, original: Task? = null): Task {
    val base = original ?: Task(
        title = "",
        type = form.type,
        recurrence = null,
        dueTime = null,
        earlyDays = 0,
        earlyTime = form.earlyTime,
        dayTimes = form.dayTimes,
        overdueIntervalHours = form.overdueIntervalHours,
        note = null,
        createdAt = now,
        updatedAt = now,
    )
    return base.copy(
        title = form.title.trim(),
        recurrence = if (base.type == TaskType.RECURRING) ruleOf(form) else null,
        dueTime = if (base.type == TaskType.ONE_OFF) form.time else null,
        earlyDays = form.earlyDays,
        earlyTime = form.earlyTime,
        dayTimes = form.dayTimes,
        overdueIntervalHours = form.overdueIntervalHours,
        note = form.note.trim().ifBlank { null },
        updatedAt = now,
    )
}
