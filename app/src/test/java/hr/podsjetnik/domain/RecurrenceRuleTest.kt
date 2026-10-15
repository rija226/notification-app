package hr.podsjetnik.domain

import hr.podsjetnik.domain.RecurrenceRule.DayOfMonth
import hr.podsjetnik.domain.RecurrenceRule.LastDayOfMonth
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class RecurrenceRuleTest {

    @Test
    fun `day of month is inclusive on the same day (handoff test 4)`() {
        assertEquals(date("2026-11-05"), DayOfMonth(5).firstOnOrAfter(date("2026-11-05")))
        assertEquals(date("2026-12-05"), DayOfMonth(5).firstOnOrAfter(date("2026-11-06")))
    }

    @Test
    fun `day of month next is strictly after`() {
        assertEquals(date("2026-12-15"), DayOfMonth(15).nextAfter(date("2026-11-15")))
        assertEquals(date("2026-11-15"), DayOfMonth(15).nextAfter(date("2026-11-14")))
    }

    @Test
    fun `day of month crosses the year end`() {
        assertEquals(date("2027-01-01"), DayOfMonth(1).nextAfter(date("2026-12-01")))
        assertEquals(date("2027-01-10"), DayOfMonth(10).firstOnOrAfter(date("2026-12-11")))
    }

    @Test
    fun `day 28 exists in every month including February`() {
        assertEquals(date("2027-02-28"), DayOfMonth(28).firstOnOrAfter(date("2027-02-01")))
        assertEquals(date("2027-03-28"), DayOfMonth(28).nextAfter(date("2027-02-28")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `day 29 is rejected, use last day of month`() {
        DayOfMonth(29)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `day 0 is rejected`() {
        DayOfMonth(0)
    }

    @Test
    fun `last day of month for every month length (handoff test 3)`() {
        assertEquals(date("2027-01-31"), LastDayOfMonth.firstOnOrAfter(date("2027-01-01")))
        assertEquals(date("2027-02-28"), LastDayOfMonth.firstOnOrAfter(date("2027-02-10")))
        assertEquals(date("2028-02-29"), LastDayOfMonth.firstOnOrAfter(date("2028-02-10")))
        assertEquals(date("2100-02-28"), LastDayOfMonth.firstOnOrAfter(date("2100-02-01")))
        assertEquals(date("2027-04-30"), LastDayOfMonth.firstOnOrAfter(date("2027-04-30")))
        assertEquals(date("2026-12-31"), LastDayOfMonth.firstOnOrAfter(date("2026-12-01")))
    }

    @Test
    fun `last day of month next steps month by month`() {
        assertEquals(date("2026-11-30"), LastDayOfMonth.nextAfter(date("2026-10-31")))
        assertEquals(date("2026-12-31"), LastDayOfMonth.nextAfter(date("2026-11-30")))
        assertEquals(date("2027-01-31"), LastDayOfMonth.nextAfter(date("2026-12-31")))
        assertEquals(date("2028-02-29"), LastDayOfMonth.nextAfter(date("2028-01-31")))
        // Done early on the 28th still means this month's last day is next only if not yet passed.
        assertEquals(date("2026-10-31"), LastDayOfMonth.nextAfter(date("2026-10-28")))
    }

    @Test
    fun `rules round-trip through JSON with a type discriminator`() {
        val dayJson = Json.encodeToString<RecurrenceRule>(DayOfMonth(5))
        val lastJson = Json.encodeToString<RecurrenceRule>(LastDayOfMonth)
        assertEquals("""{"type":"dayOfMonth","day":5}""", dayJson)
        assertEquals("""{"type":"lastDayOfMonth"}""", lastJson)
        assertEquals(DayOfMonth(5), Json.decodeFromString<RecurrenceRule>(dayJson))
        assertEquals(LastDayOfMonth, Json.decodeFromString<RecurrenceRule>(lastJson))
    }
}
