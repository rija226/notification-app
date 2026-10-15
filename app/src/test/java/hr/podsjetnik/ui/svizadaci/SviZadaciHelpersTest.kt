package hr.podsjetnik.ui.svizadaci

import hr.podsjetnik.domain.OccurrenceState
import hr.podsjetnik.domain.OccurrenceStatus
import hr.podsjetnik.domain.RecurrenceRule
import hr.podsjetnik.domain.StatusKind
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.domain.TaskWithOccurrences
import hr.podsjetnik.domain.ZAGREB
import hr.podsjetnik.domain.at
import hr.podsjetnik.domain.date
import hr.podsjetnik.domain.occurrence
import hr.podsjetnik.domain.task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SviZadaciHelpersTest {
    private val now = at("2026-11-05T10:24")

    @Test
    fun `row shows the oldest open occurrence, so a missed month stays visible`() {
        val item = TaskWithOccurrences(
            task(id = 1),
            listOf(
                occurrence("2026-09-30", taskId = 1, status = OccurrenceStatus.DONE),
                occurrence("2026-11-30", taskId = 1),
                occurrence("2026-10-31", taskId = 1),
            ),
        )
        val row = buildRows(listOf(item), TaskType.RECURRING, now, ZAGREB).single()
        assertEquals(date("2026-10-31"), row.dueDate)
        assertEquals(OccurrenceState(StatusKind.OVERDUE, 5), row.state)
    }

    @Test
    fun `rows are filtered by type, skip archived, and sort by due date`() {
        val later = TaskWithOccurrences(
            task(id = 1, recurrence = RecurrenceRule.DayOfMonth(15)),
            listOf(occurrence("2026-11-15", taskId = 1)),
        )
        val sooner = TaskWithOccurrences(
            task(id = 2, recurrence = RecurrenceRule.DayOfMonth(5)),
            listOf(occurrence("2026-11-05", taskId = 2)),
        )
        val oneOff = TaskWithOccurrences(task(id = 3, type = TaskType.ONE_OFF), listOf(occurrence("2026-11-10", taskId = 3)))
        val archived = TaskWithOccurrences(
            task(id = 4, type = TaskType.ONE_OFF).copy(archivedAt = now),
            listOf(occurrence("2026-11-01", taskId = 4, status = OccurrenceStatus.DONE)),
        )
        val all = listOf(later, sooner, oneOff, archived)
        assertEquals(listOf(2L, 1L), buildRows(all, TaskType.RECURRING, now, ZAGREB).map { it.taskId })
        assertEquals(listOf(3L), buildRows(all, TaskType.ONE_OFF, now, ZAGREB).map { it.taskId })
    }

    @Test
    fun `paused task is flagged`() {
        val paused = TaskWithOccurrences(task(id = 1, pausedAt = now), listOf(occurrence("2026-11-30", taskId = 1)))
        assertTrue(buildRows(listOf(paused), TaskType.RECURRING, now, ZAGREB).single().paused)
    }
}
