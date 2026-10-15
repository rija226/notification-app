package hr.podsjetnik.data.backup

import hr.podsjetnik.domain.Occurrence
import hr.podsjetnik.domain.OccurrenceStatus
import hr.podsjetnik.domain.RecurrenceRule
import hr.podsjetnik.domain.Settings
import hr.podsjetnik.domain.Task
import hr.podsjetnik.domain.TaskType
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Backup file (PRD FR-10): { "format": "podsjetnik-backup", "version": 1, "exportedAt", "settings", "tasks", "occurrences" }.
 * ISO-8601 text for every date/time so the file stays readable. The alarm log is not exported.
 */
const val BACKUP_FORMAT = "podsjetnik-backup"
const val BACKUP_VERSION = 1

data class BackupContent(val tasks: List<Task>, val occurrences: List<Occurrence>, val settings: Settings)

/** The file is not a valid backup, or is from a newer app version. Existing data must stay untouched. */
class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun encodeBackup(content: BackupContent, exportedAt: Instant): String = json.encodeToString(
    BackupFile(
        format = BACKUP_FORMAT,
        version = BACKUP_VERSION,
        exportedAt = exportedAt.toString(),
        settings = content.settings.toDto(),
        tasks = content.tasks.map { it.toDto() },
        occurrences = content.occurrences.map { it.toDto() },
    ),
)

/** Parses and validates a backup; throws [InvalidBackupException] for anything that isn't a v1 backup. */
fun decodeBackup(text: String): BackupContent {
    val file = try {
        json.decodeFromString<BackupFile>(text)
    } catch (e: SerializationException) {
        throw InvalidBackupException("not a backup file", e)
    } catch (e: IllegalArgumentException) {
        throw InvalidBackupException("not a backup file", e)
    }
    if (file.format != BACKUP_FORMAT) throw InvalidBackupException("unknown format ${file.format}")
    if (file.version > BACKUP_VERSION) throw InvalidBackupException("backup version ${file.version} is newer than this app")
    val content = try {
        BackupContent(file.tasks.map { it.toDomain() }, file.occurrences.map { it.toDomain() }, file.settings.toDomain())
    } catch (e: RuntimeException) { // bad dates, enum names, rule days, …
        throw InvalidBackupException("invalid value in backup", e)
    }
    validate(content)
    return content
}

private fun validate(content: BackupContent) {
    val taskIds = content.tasks.map { it.id }
    if (taskIds.size != taskIds.toSet().size) throw InvalidBackupException("duplicate task ids")
    if (content.tasks.any { it.title.isBlank() }) throw InvalidBackupException("task without a title")
    if (content.tasks.any { it.type == TaskType.RECURRING && it.recurrence == null }) {
        throw InvalidBackupException("recurring task without a rule")
    }
    if (content.occurrences.any { it.taskId !in taskIds.toSet() }) throw InvalidBackupException("occurrence of a missing task")
    val keys = content.occurrences.map { it.taskId to it.dueDate }
    if (keys.size != keys.toSet().size) throw InvalidBackupException("duplicate occurrence dates")
}

@Serializable
private data class BackupFile(
    val format: String,
    val version: Int,
    val exportedAt: String,
    val settings: SettingsDto,
    val tasks: List<TaskDto>,
    val occurrences: List<OccurrenceDto>,
)

@Serializable
private data class SettingsDto(
    val quietEnabled: Boolean,
    val quietFrom: String,
    val quietTo: String,
    val defaultDayTimes: List<String>,
    val defaultEarlyDays: Int,
    val defaultEarlyTime: String,
    val defaultOverdueIntervalHours: Int,
    val onboardingCompleted: Boolean,
    val samsungSetupConfirmed: Boolean,
    val lastSummaryDate: String? = null,
)

@Serializable
private data class TaskDto(
    val id: Long,
    val title: String,
    val type: String,
    val recurrence: RecurrenceRule? = null,
    val dueTime: String? = null,
    val earlyDays: Int,
    val earlyTime: String,
    val dayTimes: List<String>,
    val overdueIntervalHours: Int,
    val note: String? = null,
    val pausedAt: String? = null,
    val archivedAt: String? = null,
    val categoryId: Long? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
private data class OccurrenceDto(
    val id: Long,
    val taskId: Long,
    val dueDate: String,
    val status: String,
    val completedAt: String? = null,
    val lateMinutes: Int? = null,
    val value: Double? = null,
    val photoUri: String? = null,
    val snoozedUntil: String? = null,
    val lastNotifiedAt: String? = null,
    val createdAt: String,
)

private fun Settings.toDto() = SettingsDto(
    quietEnabled, quietFrom.toString(), quietTo.toString(), defaultDayTimes.map(LocalTime::toString), defaultEarlyDays,
    defaultEarlyTime.toString(), defaultOverdueIntervalHours, onboardingCompleted, samsungSetupConfirmed,
    lastSummaryDate?.toString(),
)

private fun SettingsDto.toDomain() = Settings(
    quietEnabled = quietEnabled,
    quietFrom = LocalTime.parse(quietFrom),
    quietTo = LocalTime.parse(quietTo),
    defaultDayTimes = defaultDayTimes.map(LocalTime::parse),
    defaultEarlyDays = defaultEarlyDays,
    defaultEarlyTime = LocalTime.parse(defaultEarlyTime),
    defaultOverdueIntervalHours = defaultOverdueIntervalHours,
    onboardingCompleted = onboardingCompleted,
    samsungSetupConfirmed = samsungSetupConfirmed,
    lastSummaryDate = lastSummaryDate?.let(LocalDate::parse),
)

private fun Task.toDto() = TaskDto(
    id, title, type.name, recurrence, dueTime?.toString(), earlyDays, earlyTime.toString(), dayTimes.map(LocalTime::toString),
    overdueIntervalHours, note, pausedAt?.toString(), archivedAt?.toString(), categoryId, createdAt.toString(), updatedAt.toString(),
)

private fun TaskDto.toDomain() = Task(
    id = id,
    title = title,
    type = TaskType.valueOf(type),
    recurrence = recurrence,
    dueTime = dueTime?.let(LocalTime::parse),
    earlyDays = earlyDays,
    earlyTime = LocalTime.parse(earlyTime),
    dayTimes = dayTimes.map(LocalTime::parse),
    overdueIntervalHours = overdueIntervalHours,
    note = note,
    pausedAt = pausedAt?.let(Instant::parse),
    archivedAt = archivedAt?.let(Instant::parse),
    categoryId = categoryId,
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
)

private fun Occurrence.toDto() = OccurrenceDto(
    id, taskId, dueDate.toString(), status.name, completedAt?.toString(), lateMinutes, value, photoUri,
    snoozedUntil?.toString(), lastNotifiedAt?.toString(), createdAt.toString(),
)

private fun OccurrenceDto.toDomain() = Occurrence(
    id = id,
    taskId = taskId,
    dueDate = LocalDate.parse(dueDate),
    status = OccurrenceStatus.valueOf(status),
    completedAt = completedAt?.let(Instant::parse),
    lateMinutes = lateMinutes,
    value = value,
    photoUri = photoUri,
    snoozedUntil = snoozedUntil?.let(Instant::parse),
    lastNotifiedAt = lastNotifiedAt?.let(Instant::parse),
    createdAt = Instant.parse(createdAt),
)
