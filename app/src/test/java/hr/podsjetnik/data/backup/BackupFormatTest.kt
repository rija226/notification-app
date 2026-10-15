package hr.podsjetnik.data.backup

import hr.podsjetnik.domain.OccurrenceStatus
import hr.podsjetnik.domain.RecurrenceRule
import hr.podsjetnik.domain.Settings
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.domain.at
import hr.podsjetnik.domain.date
import hr.podsjetnik.domain.occurrence
import hr.podsjetnik.domain.task
import hr.podsjetnik.domain.time
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFormatTest {
    private val exportedAt = at("2026-10-15T12:00")

    private val content = BackupContent(
        tasks = listOf(
            task(id = 1).copy(note = "brojilo u podrumu"),
            task(id = 2, type = TaskType.ONE_OFF, dueTime = time("10:00")).copy(archivedAt = at("2026-10-10T08:00")),
            task(id = 3, recurrence = RecurrenceRule.DayOfMonth(15), pausedAt = at("2026-10-01T10:00")),
        ),
        occurrences = listOf(
            occurrence("2026-09-30", id = 1, taskId = 1, status = OccurrenceStatus.DONE, completedAt = at("2026-10-01T08:15"))
                .copy(lateMinutes = 495, value = 1234.5),
            occurrence("2026-10-31", id = 2, taskId = 1).copy(lastNotifiedAt = at("2026-10-29T09:00"), snoozedUntil = at("2026-10-29T10:00")),
            occurrence("2026-10-10", id = 3, taskId = 2, status = OccurrenceStatus.DONE, completedAt = at("2026-10-10T08:00")),
        ),
        settings = Settings(quietFrom = time("23:00"), defaultEarlyDays = 3, onboardingCompleted = true, lastSummaryDate = date("2026-10-15")),
    )

    @Test
    fun `export then import gives back exactly the same data`() {
        assertEquals(content, decodeBackup(encodeBackup(content, exportedAt)))
    }

    @Test
    fun `file is readable JSON with format, version and ISO dates`() {
        val text = encodeBackup(content, exportedAt)
        assertTrue(text.contains("\"format\": \"podsjetnik-backup\""))
        assertTrue(text.contains("\"version\": 1"))
        assertTrue(text.contains("\"dueDate\": \"2026-10-31\""))
        assertTrue(text.contains("\"lastDayOfMonth\""))
    }

    @Test(expected = InvalidBackupException::class)
    fun `garbage is rejected`() {
        decodeBackup("this is not json")
    }

    @Test(expected = InvalidBackupException::class)
    fun `a newer version is rejected`() {
        decodeBackup(encodeBackup(content, exportedAt).replace("\"version\": 1", "\"version\": 2"))
    }

    @Test(expected = InvalidBackupException::class)
    fun `another app's JSON is rejected`() {
        decodeBackup(encodeBackup(content, exportedAt).replace("podsjetnik-backup", "something-else"))
    }

    @Test(expected = InvalidBackupException::class)
    fun `an occurrence pointing to a missing task is rejected`() {
        decodeBackup(encodeBackup(content.copy(occurrences = content.occurrences + occurrence("2026-11-30", id = 9, taskId = 99)), exportedAt))
    }

    @Test(expected = InvalidBackupException::class)
    fun `an invalid value is rejected`() {
        decodeBackup(encodeBackup(content, exportedAt).replace("\"2026-10-31\"", "\"31.10.2026\""))
    }
}
