package hr.podsjetnik.domain

import hr.podsjetnik.domain.StatusKind.DONE
import hr.podsjetnik.domain.StatusKind.LATER
import hr.podsjetnik.domain.StatusKind.OVERDUE
import hr.podsjetnik.domain.StatusKind.TODAY
import hr.podsjetnik.domain.StatusKind.UPCOMING
import org.junit.Assert.assertEquals
import org.junit.Test

/** HANDOFF §11 table at the fixed clock Thursday 2026-11-05 10:24, plus PRD H3 "Rok istekao". */
class TaskStatusTest {
    private val now = at("2026-11-05T10:24")

    private fun status(dueDate: String, dueTime: String? = null) =
        statusOf(occurrence(dueDate), dueTime?.let(::time), now, ZAGREB)

    @Test
    fun `Ocitati struju, last day of October, is 5 days late`() {
        assertEquals(OccurrenceState(OVERDUE, 5), status("2026-10-31"))
    }

    @Test
    fun `Nazvati servis, 3 11 at 10 00, is 2 days late`() {
        assertEquals(OccurrenceState(OVERDUE, 2), status("2026-11-03", "10:00"))
    }

    @Test
    fun `Poslati racun, due today without time, is today`() {
        assertEquals(OccurrenceState(TODAY), status("2026-11-05"))
    }

    @Test
    fun `Produziti registraciju, 10 11, is upcoming in 5 days`() {
        assertEquals(OccurrenceState(UPCOMING, 5), status("2026-11-10", "09:00"))
    }

    @Test
    fun `Platiti rezije, 15 11, is later in 10 days`() {
        assertEquals(OccurrenceState(LATER, 10), status("2026-11-15"))
    }

    @Test
    fun `due today at a time already passed is overdue with 0 days (Rok istekao)`() {
        assertEquals(OccurrenceState(OVERDUE, 0), status("2026-11-05", "10:00"))
        assertEquals(OccurrenceState(TODAY), status("2026-11-05", "11:00"))
    }

    @Test
    fun `7 days ahead is upcoming, 8 is later`() {
        assertEquals(OccurrenceState(UPCOMING, 7), status("2026-11-12"))
        assertEquals(OccurrenceState(LATER, 8), status("2026-11-13"))
    }

    @Test
    fun `closed occurrence is done`() {
        val done = occurrence("2026-10-31", status = OccurrenceStatus.DONE)
        assertEquals(OccurrenceState(DONE), statusOf(done, null, now, ZAGREB))
    }
}
