package hr.podsjetnik.ui.uredi

import hr.podsjetnik.domain.RecurrenceRule
import hr.podsjetnik.domain.Settings
import hr.podsjetnik.domain.Task
import hr.podsjetnik.domain.TaskType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Form state for Novi zadatak (Phase 2 subset of HANDOFF §6.6). */
data class UrediForm(
    val title: String = "",
    val type: TaskType = TaskType.RECURRING,
    val lastDay: Boolean = false,
    val day: Int,
    val date: LocalDate,
    val time: LocalTime? = null,
    val showErrors: Boolean = false,
)

fun initialForm(today: LocalDate) = UrediForm(day = today.dayOfMonth.coerceAtMost(28), date = today)

fun isTitleValid(title: String): Boolean = title.isNotBlank()

fun ruleOf(form: UrediForm): RecurrenceRule =
    if (form.lastDay) RecurrenceRule.LastDayOfMonth else RecurrenceRule.DayOfMonth(form.day)

/** "Sljedeći termin" shown live under the rule / date. */
fun nextDueDate(form: UrediForm, today: LocalDate): LocalDate =
    if (form.type == TaskType.RECURRING) ruleOf(form).firstOnOrAfter(today) else form.date

/** New task; reminder fields come from settings defaults until the full form (Phase 5). */
fun buildTask(form: UrediForm, settings: Settings, now: Instant): Task = Task(
    title = form.title.trim(),
    type = form.type,
    recurrence = if (form.type == TaskType.RECURRING) ruleOf(form) else null,
    dueTime = if (form.type == TaskType.ONE_OFF) form.time else null,
    earlyDays = settings.defaultEarlyDays,
    earlyTime = settings.defaultEarlyTime,
    dayTimes = settings.defaultDayTimes,
    overdueIntervalHours = settings.defaultOverdueIntervalHours,
    note = null,
    createdAt = now,
    updatedAt = now,
)
