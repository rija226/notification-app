package hr.podsjetnik.ui.detalji

import hr.podsjetnik.domain.OccurrenceState
import hr.podsjetnik.domain.OccurrenceStatus
import hr.podsjetnik.domain.RecurrenceRule
import hr.podsjetnik.domain.Settings
import hr.podsjetnik.domain.StatusKind
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.domain.TaskWithOccurrences
import hr.podsjetnik.domain.nextReminderAt
import hr.podsjetnik.domain.overdueIntervalHours
import hr.podsjetnik.domain.statusOf
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class OccurrenceRow(val id: Long, val dueDate: LocalDate, val state: OccurrenceState)

enum class ChipState { PAST, NEXT, FUTURE }

data class DayTimeChip(val time: LocalTime, val state: ChipState)

/** Rani podsjetnik row: null in [DetaljiUiState.early] means "Isključen". */
data class EarlyRow(val days: Int, val time: LocalTime, val at: LocalDateTime, val sent: Boolean)

data class LastDone(val completedAt: LocalDateTime, val dueDate: LocalDate, val daysLate: Int)

/** Detalji (HANDOFF §6.2 + PRD FR-7). "Trenutni rok" is the oldest open occurrence. */
data class DetaljiUiState(
    val taskId: Long,
    val title: String,
    val type: TaskType,
    val rule: RecurrenceRule?,
    val dueTime: LocalTime?,
    val note: String?,
    val paused: Boolean,
    val archived: Boolean,
    val current: OccurrenceRow?,
    val nextTermin: LocalDate?,
    val nextReminder: LocalDateTime?,
    val early: EarlyRow?,
    val dayTimes: List<DayTimeChip>,
    val baseIntervalHours: Int,
    /** Interval in force now when overdue, else null. */
    val currentIntervalHours: Int?,
    val quietEnabled: Boolean,
    val quietFrom: LocalTime,
    val quietTo: LocalTime,
    val lastDone: LastDone?,
    val now: LocalDateTime,
) {
    val today: LocalDate get() = now.toLocalDate()
    val allDayTimesSent: Boolean get() = dayTimes.isNotEmpty() && dayTimes.all { it.state == ChipState.PAST }
}

fun buildDetalji(item: TaskWithOccurrences, settings: Settings, now: Instant, zone: ZoneId): DetaljiUiState {
    val task = item.task
    val open = item.openOccurrences
    val current = open.firstOrNull()
    val localNow = now.atZone(zone).toLocalDateTime()
    val state = current?.let { statusOf(it, task.dueTime, now, zone) }

    val dayTimes = if (current == null) {
        task.dayTimes.map { DayTimeChip(it, ChipState.FUTURE) }
    } else {
        val past = task.dayTimes.map { current.dueDate.atTime(it) < localNow }
        val nextIndex = past.indexOfFirst { !it }
        task.dayTimes.mapIndexed { i, time ->
            DayTimeChip(time, if (past[i]) ChipState.PAST else if (i == nextIndex) ChipState.NEXT else ChipState.FUTURE)
        }
    }
    val early = if (task.earlyDays > 0 && current != null) {
        val at = current.dueDate.minusDays(task.earlyDays.toLong()).atTime(task.earlyTime)
        EarlyRow(task.earlyDays, task.earlyTime, at, sent = at < localNow)
    } else {
        null
    }
    val lastDone = item.occurrences
        .filter { it.status == OccurrenceStatus.DONE && it.completedAt != null }
        .maxByOrNull { checkNotNull(it.completedAt) }
        ?.let { done ->
            val completed = checkNotNull(done.completedAt).atZone(zone).toLocalDateTime()
            // ponytail: late by hours on the due day itself counts as on time here (calendar days only)
            val daysLate = if ((done.lateMinutes ?: 0) > 0) ChronoUnit.DAYS.between(done.dueDate, completed.toLocalDate()).toInt() else 0
            LastDone(completed, done.dueDate, daysLate)
        }

    return DetaljiUiState(
        taskId = task.id,
        title = task.title,
        type = task.type,
        rule = task.recurrence,
        dueTime = task.dueTime,
        note = task.note,
        paused = task.pausedAt != null,
        archived = task.archivedAt != null,
        current = current?.let { OccurrenceRow(it.id, it.dueDate, checkNotNull(state)) },
        nextTermin = if (task.type == TaskType.RECURRING && current != null) {
            open.getOrNull(1)?.dueDate ?: task.recurrence?.nextAfter(current.dueDate)
        } else {
            null
        },
        nextReminder = open.mapNotNull { nextReminderAt(task, it, settings, now, zone) }.minOrNull()?.atZone(zone)?.toLocalDateTime(),
        early = early,
        dayTimes = dayTimes,
        baseIntervalHours = task.overdueIntervalHours,
        currentIntervalHours = if (state?.kind == StatusKind.OVERDUE && current != null) {
            overdueIntervalHours(task.overdueIntervalHours, ChronoUnit.DAYS.between(current.dueDate, localNow.toLocalDate()))
        } else {
            null
        },
        quietEnabled = settings.quietEnabled,
        quietFrom = settings.quietFrom,
        quietTo = settings.quietTo,
        lastDone = lastDone,
        now = localNow,
    )
}
