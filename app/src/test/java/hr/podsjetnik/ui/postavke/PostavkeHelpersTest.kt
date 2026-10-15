package hr.podsjetnik.ui.postavke

import hr.podsjetnik.data.AlarmLogEntity
import hr.podsjetnik.domain.at
import hr.podsjetnik.domain.time
import hr.podsjetnik.reminders.DeviceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostavkeHelpersTest {
    private val allGood = DeviceStatus(notificationPermission = true, notificationsOk = true, exactAlarms = true, batteryExempt = true)

    @Test
    fun `everything set up means no problems`() {
        assertTrue(problemsOf(allGood, samsungConfirmed = true, lateAlarms = false).isEmpty())
    }

    @Test
    fun `each missing item is reported, only some block reminders`() {
        val bad = DeviceStatus(notificationPermission = false, notificationsOk = false, exactAlarms = false, batteryExempt = false)
        val problems = problemsOf(bad, samsungConfirmed = false, lateAlarms = true)
        assertEquals(Problem.entries.toSet(), problems)
        assertEquals(setOf(Problem.NOTIFICATIONS, Problem.EXACT_ALARMS, Problem.BATTERY), problems intersect BLOCKING_PROBLEMS)
    }

    @Test
    fun `Samsung and late alarms are warnings, not blocking`() {
        val problems = problemsOf(allGood, samsungConfirmed = false, lateAlarms = true)
        assertTrue(problems.none { it in BLOCKING_PROBLEMS })
    }

    @Test
    fun `late alarm means more than 5 minutes late within the last 7 days`() {
        val now = at("2026-11-05T10:00")
        fun entry(scheduled: String, fired: String?) =
            AlarmLogEntity(scheduledFor = at(scheduled), firedAt = fired?.let(::at))
        assertFalse(hasLateAlarm(listOf(entry("2026-11-05T09:00", "2026-11-05T09:04")), now))
        assertTrue(hasLateAlarm(listOf(entry("2026-11-05T09:00", "2026-11-05T09:06")), now))
        assertFalse(hasLateAlarm(listOf(entry("2026-10-20T09:00", "2026-10-20T11:00")), now)) // older than 7 days
        assertFalse(hasLateAlarm(listOf(entry("2026-11-05T11:00", null)), now)) // still pending
    }

    @Test
    fun `quiet hours need different start and end`() {
        assertFalse(isValidQuietRange(time("22:00"), time("22:00")))
        assertTrue(isValidQuietRange(time("22:00"), time("07:00")))
    }
}
