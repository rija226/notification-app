package hr.podsjetnik.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DateFormatHelpersTest {
    private val weekdays = listOf("pon", "uto", "sri", "čet", "pet", "sub", "ned")
    private val today = LocalDate.parse("2026-11-05")

    @Test
    fun `date in the current year has no year`() {
        assertEquals("30. 11.", formatDate(LocalDate.parse("2026-11-30"), today))
    }

    @Test
    fun `date in another year shows the year`() {
        assertEquals("3. 11. 2027.", formatDate(LocalDate.parse("2027-11-03"), today))
    }

    @Test
    fun `short date has the weekday abbreviation (HANDOFF 8)`() {
        assertEquals("čet, 5. 11.", formatShortDate(today, today, weekdays))
        assertEquals("sub, 31. 10.", formatShortDate(LocalDate.parse("2026-10-31"), today, weekdays))
        assertEquals("pon, 30. 11.", formatShortDate(LocalDate.parse("2026-11-30"), today, weekdays))
    }

    @Test
    fun `time is 24h with leading zero`() {
        assertEquals("09:00", formatTime(LocalTime.of(9, 0)))
        assertEquals("20:05", formatTime(LocalTime.of(20, 5)))
    }
}
