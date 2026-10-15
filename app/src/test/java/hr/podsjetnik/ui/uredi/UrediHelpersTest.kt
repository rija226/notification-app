package hr.podsjetnik.ui.uredi

import hr.podsjetnik.domain.RecurrenceRule
import hr.podsjetnik.domain.Settings
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.domain.TaskWithOccurrences
import hr.podsjetnik.domain.at
import hr.podsjetnik.domain.date
import hr.podsjetnik.domain.occurrence
import hr.podsjetnik.domain.task
import hr.podsjetnik.domain.time
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import hr.podsjetnik.ui.components.addDayTime
import hr.podsjetnik.ui.components.canAddDayTime
import hr.podsjetnik.ui.components.removeDayTime
import hr.podsjetnik.ui.components.stepEarlyDays
import org.junit.Test

class UrediHelpersTest {
    private val today = date("2026-10-14")
    private val now = at("2026-10-14T10:00")
    private fun newForm() = initialForm(today, Settings())

    @Test
    fun `blank title is invalid`() {
        assertFalse(isTitleValid("   "))
        assertTrue(isTitleValid("Očitati struju"))
    }

    @Test
    fun `new form starts from today and settings defaults`() {
        val form = newForm()
        assertEquals(14, form.day)
        assertEquals(2, form.earlyDays)
        assertEquals(listOf("09:00", "13:00", "17:00", "20:00").map(::time), form.dayTimes)
        assertEquals(28, initialForm(date("2026-10-31"), Settings()).day)
    }

    @Test
    fun `next due follows the chosen rule`() {
        assertEquals(date("2026-10-31"), nextDueDate(newForm().copy(lastDay = true), today))
        assertEquals(date("2026-11-10"), nextDueDate(newForm().copy(day = 10), today))
        assertEquals(date("2026-10-14"), nextDueDate(newForm().copy(day = 14), today))
        val oneOff = newForm().copy(type = TaskType.ONE_OFF, date = date("2026-11-03"))
        assertEquals(date("2026-11-03"), nextDueDate(oneOff, today))
    }

    @Test
    fun `day times stay sorted, unique, between 1 and 6`() {
        val times = listOf("13:00", "09:00").map(::time)
        assertEquals(listOf("09:00", "12:00", "13:00").map(::time), addDayTime(times, time("12:00")))
        assertEquals(2, addDayTime(times, time("09:00")).size)
        val six = (8..13).map { time("%02d:00".format(it)) }
        assertFalse(canAddDayTime(six))
        assertEquals(6, addDayTime(six, time("20:00")).size)
        assertEquals(listOf(time("09:00")), removeDayTime(listOf(time("09:00")), time("09:00")))
    }

    @Test
    fun `early days are clamped to 0 to 7`() {
        assertEquals(0, stepEarlyDays(0, -1))
        assertEquals(7, stepEarlyDays(7, 1))
        assertEquals(3, stepEarlyDays(2, 1))
    }

    @Test
    fun `new recurring task uses the form values and a trimmed title`() {
        val form = newForm().copy(title = "  Platiti režije ", day = 15, overdueIntervalHours = 4, note = "  ")
        val built = buildTask(form, now)
        assertEquals("Platiti režije", built.title)
        assertEquals(RecurrenceRule.DayOfMonth(15), built.recurrence)
        assertNull(built.dueTime)
        assertEquals(4, built.overdueIntervalHours)
        assertNull(built.note)
    }

    @Test
    fun `new one-off keeps its time and has no rule`() {
        val built = buildTask(newForm().copy(title = "Servis", type = TaskType.ONE_OFF, time = time("10:00")), now)
        assertNull(built.recurrence)
        assertEquals(time("10:00"), built.dueTime)
    }

    @Test
    fun `editing keeps the original type, pause and creation time`() {
        val original = task(id = 7, pausedAt = at("2026-10-01T10:00"))
        val form = formFromTask(TaskWithOccurrences(original, listOf(occurrence("2026-10-31", taskId = 7))), today)
        assertTrue(form.lastDay)
        val edited = buildTask(form.copy(title = "Struja", type = TaskType.ONE_OFF), now, original)
        assertEquals(7L, edited.id)
        assertEquals(TaskType.RECURRING, edited.type)
        assertEquals(original.pausedAt, edited.pausedAt)
        assertEquals(original.createdAt, edited.createdAt)
        assertEquals("Struja", edited.title)
    }
}
