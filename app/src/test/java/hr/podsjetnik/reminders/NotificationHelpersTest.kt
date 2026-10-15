package hr.podsjetnik.reminders

import hr.podsjetnik.domain.Settings
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.domain.ZAGREB
import hr.podsjetnik.domain.at
import hr.podsjetnik.domain.date
import hr.podsjetnik.domain.occurrence
import hr.podsjetnik.domain.task
import hr.podsjetnik.domain.time
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationHelpersTest {

    @Test
    fun `summary variant picks the title shape`() {
        assertEquals(SummaryVariant.TODAY_AND_OVERDUE, summaryVariant(dueToday = 3, overdue = 1))
        assertEquals(SummaryVariant.TODAY_ONLY, summaryVariant(dueToday = 2, overdue = 0))
        assertEquals(SummaryVariant.OVERDUE_ONLY, summaryVariant(dueToday = 0, overdue = 1))
    }

    @Test
    fun `overdue text interval halves from day 3`() {
        val struja = task() // every 2 h
        val october = occurrence("2026-10-31")
        assertEquals(2, currentOverdueInterval(struja, october, date("2026-11-02")))
        assertEquals(1, currentOverdueInterval(struja, october, date("2026-11-03")))
    }

    @Test
    fun `due-day text names the next reminder later today`() {
        val next = nextReminderToday(task(), occurrence("2026-10-31"), Settings(), at("2026-10-31T09:00"), ZAGREB)
        assertEquals(time("13:00"), next)
    }

    @Test
    fun `no next reminder today after the last day time`() {
        // After 20:00 the next slot is the overdue one the next morning.
        assertNull(nextReminderToday(task(), occurrence("2026-10-31"), Settings(), at("2026-10-31T20:00"), ZAGREB))
    }

    @Test
    fun `one-off due-day text points at the due time`() {
        val servis = task(type = TaskType.ONE_OFF, dueTime = time("10:00"))
        assertEquals(time("10:00"), nextReminderToday(servis, occurrence("2026-11-03"), Settings(), at("2026-11-03T09:00"), ZAGREB))
    }

    @Test
    fun `rule text is lower-cased inside a sentence`() {
        assertEquals("zadnji dan u mjesecu", lowerFirst("Zadnji dan u mjesecu"))
    }
}
