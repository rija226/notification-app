package hr.podsjetnik.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** PRD §8.4: editing a task with pending reminders. */
class ApplyEditTest {
    private val now = at("2026-11-05T14:30")
    private val today = date("2026-11-05")

    @Test
    fun `open occurrences restart from now and lose their snooze`() {
        val open = occurrence("2026-11-30", id = 1).copy(lastNotifiedAt = at("2026-11-01T09:00"), snoozedUntil = at("2026-11-06T09:00"))
        val result = applyEdit(task(), RecurrenceRule.LastDayOfMonth, listOf(open), null, today, now)
        val updated = result.update.single()
        assertEquals(now, updated.lastNotifiedAt)
        assertNull(updated.snoozedUntil)
        assertTrue(result.deleteIds.isEmpty() && result.insert.isEmpty())
    }

    @Test
    fun `rule change replaces the future occurrence, keeps the overdue one`() {
        val overdue = occurrence("2026-10-31", id = 1)
        val future = occurrence("2026-11-30", id = 2)
        val edited = task(recurrence = RecurrenceRule.DayOfMonth(10))
        val result = applyEdit(edited, RecurrenceRule.LastDayOfMonth, listOf(overdue, future), null, today, now)
        assertEquals(listOf(2L), result.deleteIds)
        assertEquals(listOf(1L), result.update.map { it.id })
        assertEquals(date("2026-11-10"), result.insert.single().dueDate)
        assertEquals(now, result.insert.single().lastNotifiedAt)
    }

    @Test
    fun `rule change never duplicates a closed date`() {
        val doneEarly = occurrence("2026-11-10", id = 1, status = OccurrenceStatus.DONE, completedAt = at("2026-11-04T10:00"))
        val edited = task(recurrence = RecurrenceRule.DayOfMonth(10))
        val result = applyEdit(edited, RecurrenceRule.LastDayOfMonth, listOf(doneEarly, occurrence("2026-11-30", id = 2)), null, today, now)
        assertEquals(date("2026-12-10"), result.insert.single().dueDate)
    }

    @Test
    fun `one-off moves to the new date`() {
        val oneOff = task(type = TaskType.ONE_OFF, dueTime = time("10:00"))
        val result = applyEdit(oneOff, null, listOf(occurrence("2026-11-03", id = 1)), date("2026-11-12"), today, now)
        assertEquals(date("2026-11-12"), result.update.single().dueDate)
    }
}
