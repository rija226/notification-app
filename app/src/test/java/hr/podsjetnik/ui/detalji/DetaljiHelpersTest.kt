package hr.podsjetnik.ui.detalji

import hr.podsjetnik.domain.OccurrenceState
import hr.podsjetnik.domain.Settings
import hr.podsjetnik.domain.StatusKind
import hr.podsjetnik.domain.TaskWithOccurrences
import hr.podsjetnik.domain.ZAGREB
import hr.podsjetnik.domain.at
import hr.podsjetnik.domain.date
import hr.podsjetnik.domain.occurrence
import hr.podsjetnik.domain.task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class DetaljiHelpersTest {
    private val now = at("2026-11-05T10:24")

    @Test
    fun `missed October is the current due date and the next reminder is the earliest of all open`() {
        val item = TaskWithOccurrences(
            task(),
            listOf(occurrence("2026-11-30", id = 2), occurrence("2026-10-31", id = 1)),
        )
        val state = buildDetalji(item, Settings(), now, ZAGREB)
        assertEquals(listOf(date("2026-10-31"), date("2026-11-30")), state.openOccurrences.map { it.dueDate })
        assertEquals(OccurrenceState(StatusKind.OVERDUE, 5), state.openOccurrences.first().state)
        assertEquals(LocalDateTime.parse("2026-11-05T11:00"), state.nextReminder)
    }

    @Test
    fun `paused task has no next reminder`() {
        val item = TaskWithOccurrences(task(pausedAt = now), listOf(occurrence("2026-11-30", id = 1)))
        val state = buildDetalji(item, Settings(), now, ZAGREB)
        assertTrue(state.paused)
        assertNull(state.nextReminder)
    }
}
