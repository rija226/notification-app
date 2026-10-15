package hr.podsjetnik.ui.uredi

import hr.podsjetnik.domain.RecurrenceRule
import hr.podsjetnik.domain.Settings
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.domain.at
import hr.podsjetnik.domain.date
import hr.podsjetnik.domain.time
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrediHelpersTest {
    private val today = date("2026-10-14")

    @Test
    fun `blank title is invalid`() {
        assertFalse(isTitleValid("   "))
        assertTrue(isTitleValid("Očitati struju"))
    }

    @Test
    fun `initial day is today, capped at 28`() {
        assertEquals(14, initialForm(today).day)
        assertEquals(28, initialForm(date("2026-10-31")).day)
    }

    @Test
    fun `next due follows the chosen rule`() {
        val form = initialForm(today)
        assertEquals(date("2026-10-31"), nextDueDate(form.copy(lastDay = true), today))
        assertEquals(date("2026-11-10"), nextDueDate(form.copy(day = 10), today))
        assertEquals(date("2026-10-14"), nextDueDate(form.copy(day = 14), today))
    }

    @Test
    fun `next due of a one-off is its date`() {
        val form = initialForm(today).copy(type = TaskType.ONE_OFF, date = date("2026-11-03"))
        assertEquals(date("2026-11-03"), nextDueDate(form, today))
    }

    @Test
    fun `built recurring task uses the rule, settings defaults and a trimmed title`() {
        val task = buildTask(initialForm(today).copy(title = "  Platiti režije ", day = 15), Settings(), at("2026-10-14T10:00"))
        assertEquals("Platiti režije", task.title)
        assertEquals(RecurrenceRule.DayOfMonth(15), task.recurrence)
        assertNull(task.dueTime)
        assertEquals(2, task.earlyDays)
        assertEquals(2, task.overdueIntervalHours)
        assertEquals(listOf("09:00", "13:00", "17:00", "20:00").map(::time), task.dayTimes)
    }

    @Test
    fun `built one-off keeps its time and has no rule`() {
        val form = initialForm(today).copy(title = "Servis", type = TaskType.ONE_OFF, time = time("10:00"))
        val task = buildTask(form, Settings(), at("2026-10-14T10:00"))
        assertNull(task.recurrence)
        assertEquals(time("10:00"), task.dueTime)
    }
}
