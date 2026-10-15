package hr.podsjetnik.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

enum class TaskType { RECURRING, ONE_OFF }

enum class OccurrenceStatus { OPEN, DONE, SKIPPED }

/** A task (PRD §5.1, §11). Wall-clock fields stay wall-clock; real moments are Instants. */
data class Task(
    val id: Long = 0,
    val title: String,
    val type: TaskType,
    val recurrence: RecurrenceRule?, // RECURRING only
    val dueTime: LocalTime?, // ONE_OFF only
    val earlyDays: Int, // 0..7, 0 = off
    val earlyTime: LocalTime,
    val dayTimes: List<LocalTime>, // sorted, unique, 1..6
    val overdueIntervalHours: Int, // 1, 2 or 4
    val note: String?,
    val pausedAt: Instant? = null,
    val archivedAt: Instant? = null,
    val categoryId: Long? = null, // v2, unused in v1 UI
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** One due instance of a task; when closed it is also the completion/skip record (PRD H1). */
data class Occurrence(
    val id: Long = 0,
    val taskId: Long,
    val dueDate: LocalDate,
    val status: OccurrenceStatus = OccurrenceStatus.OPEN,
    val completedAt: Instant? = null,
    val lateMinutes: Int? = null,
    val value: Double? = null, // v2
    val photoUri: String? = null, // v2
    val snoozedUntil: Instant? = null,
    val lastNotifiedAt: Instant? = null,
    val createdAt: Instant, // lets Poništi find the occurrence a Done auto-created
)

data class TaskWithOccurrences(val task: Task, val occurrences: List<Occurrence>) {
    val openOccurrences: List<Occurrence>
        get() = occurrences.filter { it.status == OccurrenceStatus.OPEN }.sortedBy { it.dueDate }
}

/** User settings with PRD FR-8 defaults. */
data class Settings(
    val quietEnabled: Boolean = true,
    val quietFrom: LocalTime = LocalTime.of(22, 0),
    val quietTo: LocalTime = LocalTime.of(7, 0),
    val defaultDayTimes: List<LocalTime> = listOf(9, 13, 17, 20).map { LocalTime.of(it, 0) },
    val defaultEarlyDays: Int = 2,
    val defaultEarlyTime: LocalTime = LocalTime.of(9, 0),
    val defaultOverdueIntervalHours: Int = 2,
    val onboardingCompleted: Boolean = false,
    val samsungSetupConfirmed: Boolean = false,
    val lastSummaryDate: LocalDate? = null,
)
