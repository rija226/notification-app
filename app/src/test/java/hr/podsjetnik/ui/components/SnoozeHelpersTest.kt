package hr.podsjetnik.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class SnoozeHelpersTest {
    private fun options(now: String) = snoozeOptions(LocalDateTime.parse(now)).associate { it.kind to it.at.toString() }

    @Test
    fun `morning offers 1 h, tonight and tomorrow morning`() {
        assertEquals(
            mapOf(
                SnoozeKind.ONE_HOUR to "2026-11-05T11:24",
                SnoozeKind.TONIGHT to "2026-11-05T20:00",
                SnoozeKind.TOMORROW_MORNING to "2026-11-06T09:00",
            ),
            options("2026-11-05T10:24:37"),
        )
    }

    @Test
    fun `tonight is hidden from 19 30`() {
        assertEquals(setOf(SnoozeKind.ONE_HOUR, SnoozeKind.TOMORROW_MORNING), options("2026-11-05T19:30").keys)
        assertEquals(3, options("2026-11-05T19:29").size)
    }

    @Test
    fun `1 h late in the evening crosses midnight`() {
        assertEquals("2026-11-06T00:15", options("2026-11-05T23:15")[SnoozeKind.ONE_HOUR])
    }
}
