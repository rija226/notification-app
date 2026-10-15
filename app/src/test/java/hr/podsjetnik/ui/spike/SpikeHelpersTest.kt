package hr.podsjetnik.ui.spike

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class SpikeHelpersTest {
    private val scheduled = Instant.parse("2026-10-14T10:00:00Z")

    @Test
    fun `delay is null until fired`() {
        assertNull(formatDelay(scheduled, null))
    }

    @Test
    fun `delay under a minute shows seconds with sign`() {
        assertEquals("+12 s", formatDelay(scheduled, scheduled.plusSeconds(12)))
        assertEquals("-3 s", formatDelay(scheduled, scheduled.minusSeconds(3)))
        assertEquals("+0 s", formatDelay(scheduled, scheduled))
    }

    @Test
    fun `delay over a minute shows minutes and seconds`() {
        assertEquals("+2 min 5 s", formatDelay(scheduled, scheduled.plusSeconds(125)))
    }

    @Test
    fun `log time uses local zone`() {
        assertEquals("14. 10. 12:00:00", formatLogTime(scheduled, ZoneId.of("Europe/Zagreb")))
    }
}
