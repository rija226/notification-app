package hr.podsjetnik.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Reminder stage, which also picks the notification channel (PRD §5.5). */
enum class Stage { EARLY, DUE, OVERDUE }

/** Post (or update) the notification of one occurrence. Persist [notifiedAt] as its lastNotifiedAt. */
data class ReminderPost(
    val taskId: Long,
    val occurrenceId: Long,
    val stage: Stage,
    val alert: Boolean,
    val notifiedAt: Instant,
)

/** Morning summary content (PRD FR-6). [titles] lists at most 5 tasks, overdue first. */
data class SummaryPost(val dueToday: Int, val overdue: Int, val titles: List<String>, val alert: Boolean)

/**
 * Result of one planner run (PRD §8.1).
 * [summaryDate] non-null → the summary for that date is handled (posted, or nothing to say);
 * persist it as lastSummaryDate. [activeOccurrenceIds] = occurrences that may keep a notification.
 */
data class ReminderPlan(
    val posts: List<ReminderPost>,
    val summary: SummaryPost?,
    val summaryDate: LocalDate?,
    val activeOccurrenceIds: Set<Long>,
    val nextWakeAt: Instant?,
)

/** An alarm firing slightly early still counts (PRD §8.2 rule 6). */
private val FIRE_TOLERANCE: Duration = Duration.ofMinutes(1)

private const val SUMMARY_TITLES = 5

/**
 * The reminder planner: a pure function of tasks, settings and time (PRD §8.2, §8.3).
 * The engine (Phase 4) persists the result, posts notifications and sets the single alarm to [ReminderPlan.nextWakeAt].
 */
fun planReminders(items: List<TaskWithOccurrences>, settings: Settings, now: Instant, zone: ZoneId): ReminderPlan {
    val fireUntil = now + FIRE_TOLERANCE
    val active = items
        .filter { it.task.pausedAt == null }
        .flatMap { item -> item.openOccurrences.map { item.task to it } }

    val due = mutableListOf<Triple<Task, Occurrence, Pair<Stage, Instant>>>()
    var nextWake: Instant? = null
    for ((task, occurrence) in active) {
        val slot = nextSlot(task, occurrence, settings, occurrence.lastNotifiedAt ?: occurrence.createdAt, zone)
        val upcoming = if (slot != null && slot <= fireUntil) {
            // A slot up to 1 min ahead counts as now; consuming up to the slot avoids firing it twice.
            val notifiedAt = maxOf(now, slot)
            due += Triple(task, occurrence, stageAt(task, occurrence, notifiedAt, zone) to notifiedAt)
            nextSlot(task, occurrence, settings, notifiedAt, zone)
        } else {
            slot
        }
        nextWake = earliest(nextWake, upcoming)
    }

    val today = now.atZone(zone).toLocalDate()
    val todaySummaryAt = ZonedDateTime.of(today, settings.quietTo, zone).toInstant()
    val summaryDue = settings.lastSummaryDate != today && todaySummaryAt <= fireUntil
    val summaryCounts = if (summaryDue) summaryCounts(active, now, zone) else null
    val nextSummary = if (settings.lastSummaryDate == today || todaySummaryAt <= fireUntil) {
        ZonedDateTime.of(today.plusDays(1), settings.quietTo, zone).toInstant()
    } else {
        todaySummaryAt
    }
    nextWake = earliest(nextWake, nextSummary)

    // Sound policy (PRD §8.3): one alerting post per run, the most urgent. Early reminders are always silent.
    val loudest = due.maxOfOrNull { urgency(it.third.first) }
    val summaryUrgency = if (summaryCounts != null) SUMMARY_URGENCY else 0
    val alertWinner = maxOf(loudest ?: 0, summaryUrgency)
    var alertGiven = false
    val posts = due
        .sortedByDescending { urgency(it.third.first) }
        .map { (task, occurrence, stageAndTime) ->
            val (stage, notifiedAt) = stageAndTime
            val alert = !alertGiven && stage != Stage.EARLY && urgency(stage) == alertWinner
            if (alert) alertGiven = true
            ReminderPost(task.id, occurrence.id, stage, alert, notifiedAt)
        }
    val summary = summaryCounts?.copy(alert = !alertGiven && summaryUrgency == alertWinner)

    return ReminderPlan(
        posts = posts,
        summary = summary,
        summaryDate = if (summaryDue) today else null,
        activeOccurrenceIds = active.map { it.second.id }.toSet(),
        nextWakeAt = nextWake,
    )
}

/** Next reminder after now for Detalji ("sljedeći podsjetnik u …"); null when paused or closed. */
fun nextReminderAt(task: Task, occurrence: Occurrence, settings: Settings, now: Instant, zone: ZoneId): Instant? {
    if (task.pausedAt != null || occurrence.status != OccurrenceStatus.OPEN) return null
    val after = maxOf(occurrence.lastNotifiedAt ?: occurrence.createdAt, now)
    return nextSlot(task, occurrence, settings, after, zone)
}

/** Overdue interval for a day (PRD §8.2 rule 3): base until 2 days late, then halved, at least 1 h. */
fun overdueIntervalHours(baseHours: Int, daysLate: Long): Int =
    if (daysLate <= 2) baseHours else maxOf(1, baseHours / 2)

/** Times of the fixed daily grid `anchor + k × hours` within one day. */
fun gridTimes(anchor: LocalTime, hours: Int): List<LocalTime> {
    val step = hours * 60
    val first = (anchor.hour * 60 + anchor.minute) % step
    return (first until 24 * 60 step step).map { LocalTime.of(it / 60, it % 60) }
}

/**
 * Earliest reminder slot strictly after [after], with snooze and quiet hours applied (PRD §8.2 rules 1–5).
 * Slots before the occurrence existed are never "missed": callers pass createdAt when never notified.
 */
internal fun nextSlot(task: Task, occurrence: Occurrence, settings: Settings, after: Instant, zone: ZoneId): Instant? {
    val overdue = overdueMoment(occurrence.dueDate, task.dueTime, zone)
    val snooze = occurrence.snoozedUntil
    val raw = buildList {
        if (task.earlyDays > 0) add(at(occurrence.dueDate.minusDays(task.earlyDays.toLong()), task.earlyTime, zone))
        task.dayTimes.map { at(occurrence.dueDate, it, zone) }.filterTo(this) { it < overdue }
        if (task.dueTime != null) add(overdue) // "Rok je istekao"
        if (snooze != null) add(snooze)
        addAll(overdueGrid(task, occurrence, overdue, snooze, after, zone))
    }
    return raw.asSequence()
        .filter { snooze == null || it >= snooze }
        .map { deferPastQuietHours(it, settings, zone) }
        .filter { it > after }
        .minOrNull()
}

/**
 * Overdue grid slots for a few days around [after]. Anchor = due time, else the first day time;
 * after a snooze that ends in the overdue period, the grid restarts from the snooze time (rule 4).
 */
private fun overdueGrid(
    task: Task,
    occurrence: Occurrence,
    overdue: Instant,
    snooze: Instant?,
    after: Instant,
    zone: ZoneId,
): List<Instant> {
    val anchor = when {
        snooze != null && snooze >= overdue -> snooze.atZone(zone).toLocalTime()
        else -> task.dueTime ?: task.dayTimes.minOrNull() ?: LocalTime.of(9, 0)
    }
    // ponytail: 4 days from max(overdue date, day before `after`) always contains the next slot,
    // because every day has grid slots and quiet hours can't cover a whole day (start ≠ end).
    val start = maxOf(overdue.atZone(zone).toLocalDate(), after.atZone(zone).toLocalDate().minusDays(1))
    return (0L..3L).flatMap { offset ->
        val date = start.plusDays(offset)
        val hours = overdueIntervalHours(task.overdueIntervalHours, ChronoUnit.DAYS.between(occurrence.dueDate, date))
        gridTimes(anchor, hours).map { at(date, it, zone) }
    }.filter { it >= overdue }
}

private fun stageAt(task: Task, occurrence: Occurrence, time: Instant, zone: ZoneId): Stage = when {
    time >= overdueMoment(occurrence.dueDate, task.dueTime, zone) -> Stage.OVERDUE
    time.atZone(zone).toLocalDate() >= occurrence.dueDate -> Stage.DUE
    else -> Stage.EARLY
}

private fun summaryCounts(active: List<Pair<Task, Occurrence>>, now: Instant, zone: ZoneId): SummaryPost? {
    val states = active.map { (task, occurrence) -> task to statusOf(occurrence, task.dueTime, now, zone).kind }
    val overdue = states.filter { it.second == StatusKind.OVERDUE }
    val dueToday = states.filter { it.second == StatusKind.TODAY }
    if (overdue.isEmpty() && dueToday.isEmpty()) return null
    val titles = (overdue + dueToday).map { it.first.title }.distinct().take(SUMMARY_TITLES)
    return SummaryPost(dueToday.size, overdue.size, titles, alert = false)
}

// Higher = more urgent (PRD §8.3): overdue > due > summary > early.
private const val SUMMARY_URGENCY = 2

private fun urgency(stage: Stage) = when (stage) {
    Stage.OVERDUE -> 4
    Stage.DUE -> 3
    Stage.EARLY -> 1
}

/** Wall-clock date + time in [zone]; java.time moves times in a DST gap forward, overlaps use the earlier offset. */
private fun at(date: LocalDate, time: LocalTime, zone: ZoneId): Instant = ZonedDateTime.of(date, time, zone).toInstant()

private fun earliest(a: Instant?, b: Instant?): Instant? = if (a == null) b else if (b == null) a else minOf(a, b)
