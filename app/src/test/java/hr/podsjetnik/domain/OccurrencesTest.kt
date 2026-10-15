package hr.podsjetnik.domain

import hr.podsjetnik.domain.OccurrenceStatus.DONE
import hr.podsjetnik.domain.OccurrenceStatus.OPEN
import hr.podsjetnik.domain.OccurrenceStatus.SKIPPED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OccurrencesTest {
    private val now = at("2026-10-14T10:00")

    private fun create(existing: List<Occurrence>, today: String, task: Task = task()) =
        occurrencesToCreate(task, existing, date(today), now).map { it.dueDate.toString() }

    @Test
    fun `new recurring task gets the first rule date on or after today`() {
        assertEquals(listOf("2026-10-31"), create(emptyList(), "2026-10-14"))
        val day5 = task(recurrence = RecurrenceRule.DayOfMonth(5))
        assertEquals(listOf("2026-11-05"), create(emptyList(), "2026-11-05", day5))
        assertEquals(listOf("2026-12-05"), create(emptyList(), "2026-11-06", day5))
    }

    @Test
    fun `open future occurrence needs nothing`() {
        assertEquals(emptyList<String>(), create(listOf(occurrence("2026-10-31")), "2026-10-14"))
    }

    @Test
    fun `open occurrence due today needs nothing`() {
        assertEquals(emptyList<String>(), create(listOf(occurrence("2026-10-31")), "2026-10-31"))
    }

    @Test
    fun `done early creates the next occurrence immediately`() {
        val doneEarly = occurrence("2026-10-31", status = DONE)
        assertEquals(listOf("2026-11-30"), create(listOf(doneEarly), "2026-10-28"))
    }

    @Test
    fun `still open the day after due creates the next one and keeps both open`() {
        assertEquals(listOf("2026-11-30"), create(listOf(occurrence("2026-10-31")), "2026-11-01"))
    }

    @Test
    fun `done on the old month while next already exists creates nothing`() {
        val existing = listOf(occurrence("2026-10-31", status = DONE), occurrence("2026-11-30"))
        assertEquals(emptyList<String>(), create(existing, "2026-11-05"))
    }

    @Test
    fun `skipped latest creates the next one`() {
        assertEquals(listOf("2026-12-31"), create(listOf(occurrence("2026-11-30", status = SKIPPED)), "2026-11-20"))
    }

    @Test
    fun `phone off for months creates every missed month up to today`() {
        assertEquals(
            listOf("2026-11-30", "2026-12-31", "2027-01-31", "2027-02-28"),
            create(listOf(occurrence("2026-10-31")), "2027-02-02"),
        )
    }

    @Test
    fun `paused and one-off tasks get nothing`() {
        assertEquals(emptyList<String>(), create(emptyList(), "2026-10-14", task(pausedAt = now)))
        assertEquals(emptyList<String>(), create(emptyList(), "2026-10-14", task(type = TaskType.ONE_OFF)))
    }

    @Test
    fun `resume creates first date on or after today, skipping paused months`() {
        val oldOpen = occurrence("2026-10-31")
        assertEquals(date("2026-12-31"), occurrenceOnResume(task(), listOf(oldOpen), date("2026-12-10"), now)?.dueDate)
    }

    @Test
    fun `resume creates nothing when an open occurrence is still ahead`() {
        assertNull(occurrenceOnResume(task(), listOf(occurrence("2026-12-31")), date("2026-12-10"), now))
    }

    @Test
    fun `resume never duplicates a date that already exists`() {
        val doneEarly = occurrence("2026-12-31", status = DONE)
        assertEquals(date("2027-01-31"), occurrenceOnResume(task(), listOf(doneEarly), date("2026-12-10"), now)?.dueDate)
    }

    @Test
    fun `overdue moment is the due time, or midnight after the due day`() {
        assertEquals(at("2026-11-01T00:00"), overdueMoment(date("2026-10-31"), null, ZAGREB))
        assertEquals(at("2026-11-03T10:00"), overdueMoment(date("2026-11-03"), time("10:00"), ZAGREB))
    }

    @Test
    fun `done on time or early has zero lateness`() {
        val done = occurrence("2026-10-31").markDone(task(), at("2026-10-28T18:00"), ZAGREB)
        assertEquals(DONE, done.status)
        assertEquals(0, done.lateMinutes)
        assertEquals(0, occurrence("2026-10-31").markDone(task(), at("2026-10-31T23:59"), ZAGREB).lateMinutes)
    }

    @Test
    fun `done two days late stores minutes after the overdue moment (PRD timeline C)`() {
        val done = occurrence("2026-10-31").markDone(task(), at("2026-11-02T13:20"), ZAGREB)
        assertEquals(2240, done.lateMinutes)
    }

    @Test
    fun `one-off with a time is late from that time`() {
        val oneOff = task(type = TaskType.ONE_OFF, dueTime = time("10:00"))
        assertEquals(30, occurrence("2026-11-03").markDone(oneOff, at("2026-11-03T10:30"), ZAGREB).lateMinutes)
    }

    @Test
    fun `skip closes without lateness and clears snooze`() {
        val skipped = occurrence("2026-11-30", snoozedUntil = now).skip(now)
        assertEquals(SKIPPED, skipped.status)
        assertNull(skipped.lateMinutes)
        assertNull(skipped.snoozedUntil)
    }

    @Test
    fun `undo deletes only the untouched occurrence created by the close`() {
        val closedAt = at("2026-10-31T13:40")
        val undone = occurrence("2026-10-31", id = 1, status = DONE, completedAt = closedAt)
        val createdByDone = occurrence("2026-11-30", id = 2, createdAt = closedAt)
        val createdEarlier = occurrence("2026-12-31", id = 3, createdAt = at("2026-10-01T00:00"))
        val snoozed = occurrence("2027-01-31", id = 4, createdAt = closedAt, snoozedUntil = closedAt)
        val toDelete = occurrencesToDeleteOnUndo(undone, listOf(undone, createdByDone, createdEarlier, snoozed))
        assertEquals(listOf(2L), toDelete.map { it.id })
        val reopened = undone.reopen()
        assertEquals(OPEN, reopened.status)
        assertNull(reopened.completedAt)
    }

    @Test
    fun `undo of an occurrence that was never closed deletes nothing`() {
        assertTrue(occurrencesToDeleteOnUndo(occurrence("2026-10-31"), emptyList()).isEmpty())
    }
}
