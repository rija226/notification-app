package hr.podsjetnik.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * When an occurrence becomes overdue (PRD §5.3): at the due time if there is one,
 * otherwise at 00:00 the day after the due date.
 */
fun overdueMoment(dueDate: LocalDate, dueTime: LocalTime?, zone: ZoneId): Instant {
    val local = if (dueTime != null) dueDate.atTime(dueTime) else dueDate.plusDays(1).atStartOfDay()
    return local.atZone(zone).toInstant()
}

/** Minutes after the overdue moment, 0 when on time. Frozen at completion (PRD §11). */
fun lateMinutes(dueDate: LocalDate, dueTime: LocalTime?, completedAt: Instant, zone: ZoneId): Int =
    Duration.between(overdueMoment(dueDate, dueTime, zone), completedAt).toMinutes().coerceAtLeast(0).toInt()

/**
 * Occurrences a recurring task needs today (PRD FR-3):
 * a new task gets its first occurrence on/after today; when the latest occurrence is closed,
 * or still open after its due date, the next one is created (repeating after the phone was off).
 */
fun occurrencesToCreate(task: Task, existing: List<Occurrence>, today: LocalDate, now: Instant): List<Occurrence> {
    val rule = task.recurrence ?: return emptyList()
    if (task.pausedAt != null) return emptyList()
    var latest = existing.maxByOrNull { it.dueDate }
        ?: return listOf(Occurrence(taskId = task.id, dueDate = rule.firstOnOrAfter(today), createdAt = now))
    val created = mutableListOf<Occurrence>()
    while (latest.status != OccurrenceStatus.OPEN || latest.dueDate < today) {
        latest = Occurrence(taskId = task.id, dueDate = rule.nextAfter(latest.dueDate), createdAt = now)
        created += latest
    }
    return created
}

/**
 * On resume: if nothing is open on/after today, one occurrence from firstOnOrAfter(today).
 * Months during the pause are not recorded (PRD FR-3).
 */
fun occurrenceOnResume(task: Task, existing: List<Occurrence>, today: LocalDate, now: Instant): Occurrence? {
    val rule = task.recurrence ?: return null
    if (existing.any { it.status == OccurrenceStatus.OPEN && it.dueDate >= today }) return null
    var date = rule.firstOnOrAfter(today)
    while (existing.any { it.dueDate == date }) date = rule.nextAfter(date)
    return Occurrence(taskId = task.id, dueDate = date, createdAt = now)
}

/** What an edit does to a task's occurrences (PRD §8.4). */
data class EditResult(val update: List<Occurrence>, val deleteIds: List<Long>, val insert: List<Occurrence>)

/**
 * Edit with pending reminders (PRD §8.4): open occurrences restart from now (no reminder fires for past
 * slots of the new schedule, snooze cleared). A one-off moves to [oneOffDate]. A changed recurrence rule
 * replaces the open, not-yet-past occurrences with the new rule's first date on/after today; overdue ones stay.
 */
fun applyEdit(
    edited: Task,
    previousRule: RecurrenceRule?,
    existing: List<Occurrence>,
    oneOffDate: LocalDate?,
    today: LocalDate,
    now: Instant,
): EditResult {
    val open = existing.filter { it.status == OccurrenceStatus.OPEN }
    val ruleChanged = edited.recurrence != null && edited.recurrence != previousRule
    val replaced = if (ruleChanged) open.filter { it.dueDate >= today } else emptyList()
    val update = (open - replaced.toSet()).map { occurrence ->
        val moved = if (edited.type == TaskType.ONE_OFF && oneOffDate != null) occurrence.copy(dueDate = oneOffDate) else occurrence
        moved.copy(lastNotifiedAt = now, snoozedUntil = null)
    }
    val insert = if (ruleChanged) {
        val rule = checkNotNull(edited.recurrence)
        val kept = existing - replaced.toSet()
        var date = rule.firstOnOrAfter(today)
        while (kept.any { it.dueDate == date }) date = rule.nextAfter(date)
        listOf(Occurrence(taskId = edited.id, dueDate = date, lastNotifiedAt = now, createdAt = now))
    } else {
        emptyList()
    }
    return EditResult(update, replaced.map { it.id }, insert)
}

fun Occurrence.markDone(task: Task, now: Instant, zone: ZoneId): Occurrence = copy(
    status = OccurrenceStatus.DONE,
    completedAt = now,
    lateMinutes = lateMinutes(dueDate, task.dueTime, now, zone),
    snoozedUntil = null,
)

fun Occurrence.skip(now: Instant): Occurrence = copy(
    status = OccurrenceStatus.SKIPPED,
    completedAt = now,
    lateMinutes = null,
    snoozedUntil = null,
)

fun Occurrence.reopen(): Occurrence = copy(status = OccurrenceStatus.OPEN, completedAt = null, lateMinutes = null)

/**
 * Occurrences that closing [undone] auto-created and Poništi should delete:
 * created at/after the close and still untouched (open, never snoozed or notified).
 */
fun occurrencesToDeleteOnUndo(undone: Occurrence, all: List<Occurrence>): List<Occurrence> {
    val closedAt = undone.completedAt ?: return emptyList()
    return all.filter {
        it.taskId == undone.taskId &&
            it.id != undone.id &&
            it.status == OccurrenceStatus.OPEN &&
            it.createdAt >= closedAt &&
            it.snoozedUntil == null &&
            it.lastNotifiedAt == null
    }
}
