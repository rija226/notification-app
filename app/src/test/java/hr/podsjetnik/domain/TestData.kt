package hr.podsjetnik.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

val ZAGREB: ZoneId = ZoneId.of("Europe/Zagreb")

fun date(value: String): LocalDate = LocalDate.parse(value)

fun time(value: String): LocalTime = LocalTime.parse(value)

/** Local Zagreb date-time "2026-11-05T10:24" as an Instant. */
fun at(value: String): Instant = LocalDateTime.parse(value).atZone(ZAGREB).toInstant()

fun task(
    id: Long = 1,
    type: TaskType = TaskType.RECURRING,
    recurrence: RecurrenceRule? = RecurrenceRule.LastDayOfMonth,
    dueTime: LocalTime? = null,
    pausedAt: Instant? = null,
    earlyDays: Int = 2,
) = Task(
    id = id,
    title = "Očitati struju",
    type = type,
    recurrence = if (type == TaskType.RECURRING) recurrence else null,
    dueTime = dueTime,
    earlyDays = earlyDays,
    earlyTime = time("09:00"),
    dayTimes = listOf("09:00", "13:00", "17:00", "20:00").map(::time),
    overdueIntervalHours = 2,
    note = null,
    pausedAt = pausedAt,
    createdAt = at("2026-09-01T08:00"),
    updatedAt = at("2026-09-01T08:00"),
)

fun occurrence(
    dueDate: String,
    id: Long = 0,
    status: OccurrenceStatus = OccurrenceStatus.OPEN,
    createdAt: Instant = at("2026-09-01T08:00"),
    completedAt: Instant? = null,
    snoozedUntil: Instant? = null,
    taskId: Long = 1,
) = Occurrence(
    id = id,
    taskId = taskId,
    dueDate = date(dueDate),
    status = status,
    completedAt = completedAt,
    snoozedUntil = snoozedUntil,
    createdAt = createdAt,
)
