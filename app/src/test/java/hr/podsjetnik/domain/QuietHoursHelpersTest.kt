package hr.podsjetnik.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuietHoursHelpersTest {
    private val defaults = Settings() // 22:00–07:00, on

    @Test
    fun `window crossing midnight includes start and excludes end`() {
        assertTrue(isInQuietHours(time("22:00"), time("22:00"), time("07:00")))
        assertTrue(isInQuietHours(time("03:00"), time("22:00"), time("07:00")))
        assertFalse(isInQuietHours(time("07:00"), time("22:00"), time("07:00")))
        assertFalse(isInQuietHours(time("21:59"), time("22:00"), time("07:00")))
    }

    @Test
    fun `window inside one day`() {
        assertTrue(isInQuietHours(time("13:30"), time("13:00"), time("14:00")))
        assertFalse(isInQuietHours(time("14:00"), time("13:00"), time("14:00")))
    }

    @Test
    fun `evening slot moves to next morning, night slot to the same morning`() {
        assertEquals(at("2026-11-02T07:00"), deferPastQuietHours(at("2026-11-01T23:00"), defaults, ZAGREB))
        assertEquals(at("2026-11-02T07:00"), deferPastQuietHours(at("2026-11-02T05:00"), defaults, ZAGREB))
        assertEquals(at("2026-11-02T07:00"), deferPastQuietHours(at("2026-11-01T22:00"), defaults, ZAGREB))
    }

    @Test
    fun `slots outside quiet hours are unchanged`() {
        assertEquals(at("2026-11-01T21:00"), deferPastQuietHours(at("2026-11-01T21:00"), defaults, ZAGREB))
        assertEquals(at("2026-11-01T07:00"), deferPastQuietHours(at("2026-11-01T07:00"), defaults, ZAGREB))
    }

    @Test
    fun `custom window 23 to 06`() {
        val custom = defaults.copy(quietFrom = time("23:00"), quietTo = time("06:00"))
        assertEquals(at("2026-11-01T22:30"), deferPastQuietHours(at("2026-11-01T22:30"), custom, ZAGREB))
        assertEquals(at("2026-11-02T06:00"), deferPastQuietHours(at("2026-11-01T23:30"), custom, ZAGREB))
    }

    @Test
    fun `quiet hours switched off change nothing`() {
        val off = defaults.copy(quietEnabled = false)
        assertEquals(at("2026-11-01T23:00"), deferPastQuietHours(at("2026-11-01T23:00"), off, ZAGREB))
    }
}
