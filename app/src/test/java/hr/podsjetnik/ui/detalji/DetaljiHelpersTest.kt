package hr.podsjetnik.ui.detalji

import hr.podsjetnik.domain.OccurrenceState
import hr.podsjetnik.domain.OccurrenceStatus
import hr.podsjetnik.domain.Settings
import hr.podsjetnik.domain.StatusKind
import hr.podsjetnik.domain.TaskWithOccurrences
import hr.podsjetnik.domain.ZAGREB
import hr.podsjetnik.domain.at
import hr.podsjetnik.domain.date
import hr.podsjetnik.domain.occurrence
import hr.podsjetnik.domain.task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/** Screen 02 at 2026-11-05 10:24: Očitati struju, 5 days late. */
class DetaljiHelpersTest {
    private val now = at("2026-11-05T10:24")
    private val lastDone = occurrence("2026-09-30", id = 9, status = OccurrenceStatus.DONE, completedAt = at("2026-10-01T08:15"))
        .copy(lateMinutes = 495)

    private fun struja(vararg open: hr.podsjetnik.domain.Occurrence) =
        buildDetalji(TaskWithOccurrences(task(), open.toList() + lastDone), Settings(), now, ZAGREB)

    @Test
    fun `current due is the missed month, next termin the following one`() {
        val state = struja(occurrence("2026-11-30", id = 2), occurrence("2026-10-31", id = 1))
        assertEquals(date("2026-10-31"), state.current?.dueDate)
        assertEquals(OccurrenceState(StatusKind.OVERDUE, 5), state.current?.state)
        assertEquals(date("2026-11-30"), state.nextTermin)
        assertEquals(LocalDateTime.parse("2026-11-05T11:00"), state.nextReminder)
    }

    @Test
    fun `schedule rows - early sent, all day times sent, escalated interval`() {
        val state = struja(occurrence("2026-10-31", id = 1))
        assertTrue(state.early!!.sent)
        assertEquals(LocalDateTime.parse("2026-10-29T09:00"), state.early!!.at)
        assertTrue(state.allDayTimesSent)
        assertEquals(2, state.baseIntervalHours)
        assertEquals(1, state.currentIntervalHours) // 5 days late → every hour
    }

    @Test
    fun `next termin falls back to the rule when only one month is open`() {
        assertEquals(date("2026-11-30"), struja(occurrence("2026-10-31", id = 1)).nextTermin)
    }

    @Test
    fun `last done shows calendar days late`() {
        val done = struja(occurrence("2026-10-31", id = 1)).lastDone!!
        assertEquals(LocalDateTime.parse("2026-10-01T08:15"), done.completedAt)
        assertEquals(1, done.daysLate)
    }

    @Test
    fun `due day chips mark past, next and future`() {
        val state = buildDetalji(
            TaskWithOccurrences(task(), listOf(occurrence("2026-11-05", id = 1))),
            Settings(),
            now,
            ZAGREB,
        )
        assertEquals(listOf(ChipState.PAST, ChipState.NEXT, ChipState.FUTURE, ChipState.FUTURE), state.dayTimes.map { it.state })
        assertFalse(state.allDayTimesSent)
        assertNull(state.currentIntervalHours)
    }
}
