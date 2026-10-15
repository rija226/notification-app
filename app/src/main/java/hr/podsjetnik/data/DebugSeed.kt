package hr.podsjetnik.data

import android.content.Context
import hr.podsjetnik.domain.Occurrence
import hr.podsjetnik.domain.OccurrenceStatus
import hr.podsjetnik.domain.RecurrenceRule
import hr.podsjetnik.domain.Settings
import hr.podsjetnik.domain.Task
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.domain.lateMinutes
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * Loads the handoff's sample-tasks.json (debug builds only: the asset lives in src/debug/assets).
 * The file uses the handoff's older format (due date on the task + completions), converted here
 * to the occurrence model (PRD H1).
 */
object DebugSeed {
    private const val ASSET = "sample-tasks.json"

    fun isAvailable(context: Context): Boolean = context.assets.list("")?.contains(ASSET) == true

    suspend fun load(context: Context, repository: TaskRepository) {
        val text = context.assets.open(ASSET).bufferedReader().use { it.readText() }
        val seed = Json { ignoreUnknownKeys = true }.decodeFromString<SeedFile>(text)
        val zone = ZoneId.systemDefault()
        val tasks = seed.tasks.map { it.toTask() }
        val open = seed.tasks.map {
            Occurrence(taskId = it.id, dueDate = LocalDate.parse(it.dueDate), createdAt = instant(it.createdAt))
        }
        val done = seed.completions.map { c ->
            val task = tasks.first { it.id == c.taskId }
            val dueDate = LocalDate.parse(c.occurrenceDate)
            val completedAt = instant(c.completedAt)
            Occurrence(
                taskId = c.taskId,
                dueDate = dueDate,
                status = OccurrenceStatus.DONE,
                completedAt = completedAt,
                lateMinutes = lateMinutes(dueDate, task.dueTime, completedAt, zone),
                createdAt = completedAt,
            )
        }
        repository.replaceAll(tasks, open + done, seed.settings.toSettings())
    }
}

private fun instant(text: String) = OffsetDateTime.parse(text).toInstant()

@Serializable
private data class SeedFile(
    val settings: SeedSettings,
    val tasks: List<SeedTask>,
    val completions: List<SeedCompletion>,
)

@Serializable
private data class SeedSettings(
    val quietEnabled: Boolean,
    val quietFrom: String,
    val quietTo: String,
    val defaultDayTimes: List<String>,
    val defaultEarlyDays: Int,
    val defaultEarlyTime: String,
    val defaultOverdueIntervalHours: Int,
    val onboardingCompleted: Boolean,
) {
    fun toSettings() = Settings(
        quietEnabled = quietEnabled,
        quietFrom = LocalTime.parse(quietFrom),
        quietTo = LocalTime.parse(quietTo),
        defaultDayTimes = defaultDayTimes.map(LocalTime::parse),
        defaultEarlyDays = defaultEarlyDays,
        defaultEarlyTime = LocalTime.parse(defaultEarlyTime),
        defaultOverdueIntervalHours = defaultOverdueIntervalHours,
        onboardingCompleted = onboardingCompleted,
    )
}

@Serializable
private data class SeedTask(
    val id: Long,
    val title: String,
    val type: String,
    val ruleKind: String?,
    val ruleDay: Int?,
    val dueDate: String,
    val dueTime: String?,
    val earlyDays: Int,
    val earlyTime: String,
    val dayTimes: List<String>,
    val overdueIntervalHours: Int,
    val note: String?,
    val createdAt: String,
) {
    fun toTask(): Task {
        val created = instant(createdAt)
        return Task(
            id = id,
            title = title,
            type = TaskType.valueOf(type),
            recurrence = when (ruleKind) {
                "DAY_OF_MONTH" -> RecurrenceRule.DayOfMonth(checkNotNull(ruleDay))
                "LAST_DAY_OF_MONTH" -> RecurrenceRule.LastDayOfMonth
                else -> null
            },
            dueTime = dueTime?.let(LocalTime::parse),
            earlyDays = earlyDays,
            earlyTime = LocalTime.parse(earlyTime),
            dayTimes = dayTimes.map(LocalTime::parse),
            overdueIntervalHours = overdueIntervalHours,
            note = note,
            createdAt = created,
            updatedAt = created,
        )
    }
}

@Serializable
private data class SeedCompletion(val taskId: Long, val occurrenceDate: String, val completedAt: String)
