package hr.podsjetnik.data

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import hr.podsjetnik.domain.Occurrence
import hr.podsjetnik.domain.OccurrenceStatus
import hr.podsjetnik.domain.RecurrenceRule
import hr.podsjetnik.domain.Task
import hr.podsjetnik.domain.TaskType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "task")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: TaskType,
    val recurrence: RecurrenceRule?,
    val dueTime: LocalTime?,
    val earlyDays: Int,
    val earlyTime: LocalTime,
    val dayTimes: List<LocalTime>,
    val overdueIntervalHours: Int,
    val note: String?,
    val pausedAt: Instant?,
    val archivedAt: Instant?,
    val categoryId: Long?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "occurrence",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["taskId", "dueDate"], unique = true), Index(value = ["status"])],
)
data class OccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val dueDate: LocalDate,
    val status: OccurrenceStatus,
    val completedAt: Instant?,
    val lateMinutes: Int?,
    val value: Double?,
    val photoUri: String?,
    val snoozedUntil: Instant?,
    val lastNotifiedAt: Instant?,
    val createdAt: Instant,
)

/**
 * One planned alarm fire and when it actually fired (PRD §8.6). Diagnostics only, never exported.
 * `mode` is spike-only (Phase 1): which AlarmManager API scheduled it.
 */
@Entity(tableName = "alarm_log")
data class AlarmLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scheduledFor: Instant,
    val firedAt: Instant? = null,
    val postedCount: Int = 0,
    val mode: AlarmMode = AlarmMode.EXACT_WHILE_IDLE,
)

enum class AlarmMode { EXACT_WHILE_IDLE, ALARM_CLOCK }

fun TaskEntity.toDomain() = Task(
    id, title, type, recurrence, dueTime, earlyDays, earlyTime, dayTimes, overdueIntervalHours, note,
    pausedAt, archivedAt, categoryId, createdAt, updatedAt,
)

fun Task.toEntity() = TaskEntity(
    id, title, type, recurrence, dueTime, earlyDays, earlyTime, dayTimes, overdueIntervalHours, note,
    pausedAt, archivedAt, categoryId, createdAt, updatedAt,
)

fun OccurrenceEntity.toDomain() = Occurrence(
    id, taskId, dueDate, status, completedAt, lateMinutes, value, photoUri, snoozedUntil, lastNotifiedAt, createdAt,
)

fun Occurrence.toEntity() = OccurrenceEntity(
    id, taskId, dueDate, status, completedAt, lateMinutes, value, photoUri, snoozedUntil, lastNotifiedAt, createdAt,
)
