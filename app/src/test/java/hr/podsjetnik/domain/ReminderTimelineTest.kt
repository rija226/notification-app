package hr.podsjetnik.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PRD §8.5 timelines A–G, run through the planner like the engine will (one refresh per wake-up).
 * "Očitati struju": last day of month, early 2 days at 09:00, day times 09/13/17/20, overdue every 2 h,
 * quiet hours 22–07. D (swipe re-post) is engine behavior, tested in Phase 4.
 */
class ReminderTimelineTest {
    private val struja = task()
    private fun october() = TaskWithOccurrences(struja, listOf(occurrence("2026-10-31", id = 1)))

    @Test
    fun `A - done on time`() {
        val sim = ReminderSimulator(listOf(october()))
        sim.run("2026-10-28T12:00", "2026-10-31T13:39")
        assertEquals(
            listOf("29.10 09:00 EARLY", "31.10 07:00 SUMMARY!", "31.10 09:00 DUE!", "31.10 13:00 DUE!"),
            sim.events,
        )

        sim.updateOccurrence(1) { it.markDone(struja, at("2026-10-31T13:40"), ZAGREB) }
        sim.events.clear()
        sim.run("2026-10-31T13:40", "2026-11-28T09:00")
        assertEquals(listOf("28.11 09:00 EARLY"), sim.events)
    }

    @Test
    fun `B - done early, no October reminders, next early reminder 28 11`() {
        val doneEarly = occurrence("2026-10-31", id = 1, status = OccurrenceStatus.DONE, completedAt = at("2026-10-28T18:00"))
        val sim = ReminderSimulator(listOf(TaskWithOccurrences(struja, listOf(doneEarly))))
        sim.run("2026-10-28T18:00", "2026-11-28T09:00")
        assertEquals(listOf("28.11 09:00 EARLY"), sim.events)
    }

    @Test
    fun `C - two days late, quiet hours collapse to 07 00, hourly from day 3`() {
        val sim = ReminderSimulator(listOf(october()))
        sim.run("2026-10-28T12:00", "2026-11-02T13:19")
        val overdueDay1 = listOf("09:00", "11:00", "13:00", "15:00", "17:00", "19:00", "21:00").map { "01.11 $it OVERDUE!" }
        assertEquals(
            listOf("29.10 09:00 EARLY") +
                listOf("31.10 07:00 SUMMARY!") +
                listOf("09:00", "13:00", "17:00", "20:00").map { "31.10 $it DUE!" } +
                listOf("01.11 07:00 OVERDUE!", "01.11 07:00 SUMMARY") + overdueDay1 +
                listOf("02.11 07:00 OVERDUE!", "02.11 07:00 SUMMARY") +
                listOf("09:00", "11:00", "13:00").map { "02.11 $it OVERDUE!" },
            sim.events,
        )
        // The November occurrence was created on 1. 11.; it stays quiet until its own early reminder.
        assertEquals(OccurrenceStatus.OPEN, sim.occurrence("2026-11-30").status)

        // Not done: from day 3 late the interval halves to every hour.
        sim.events.clear()
        sim.run("2026-11-02T13:19", "2026-11-03T10:00")
        assertEquals(
            listOf("15:00", "17:00", "19:00", "21:00").map { "02.11 $it OVERDUE!" } +
                listOf("03.11 07:00 OVERDUE!", "03.11 07:00 SUMMARY") +
                listOf("08:00", "09:00", "10:00").map { "03.11 $it OVERDUE!" },
            sim.events,
        )
    }

    @Test
    fun `E - skipped month, next reminder is December's early one`() {
        val items = TaskWithOccurrences(
            struja,
            listOf(
                occurrence("2026-10-31", id = 1, status = OccurrenceStatus.DONE, completedAt = at("2026-10-31T13:40")),
                occurrence("2026-11-30", id = 2).skip(at("2026-11-20T10:00")),
            ),
        )
        val sim = ReminderSimulator(listOf(items))
        sim.run("2026-11-20T10:00", "2026-12-29T09:00")
        assertEquals(listOf("29.12 09:00 EARLY"), sim.events)
    }

    @Test
    fun `F - snooze 1 h, then tomorrow morning`() {
        val notified = occurrence("2026-10-31", id = 1).copy(lastNotifiedAt = at("2026-10-31T09:00"))
        val sim = ReminderSimulator(
            listOf(TaskWithOccurrences(struja, listOf(notified))),
            Settings(lastSummaryDate = date("2026-10-31")),
        )
        sim.run("2026-10-31T12:00", "2026-10-31T13:00")
        assertEquals(listOf("31.10 13:00 DUE!"), sim.events)

        sim.updateOccurrence(1) { it.copy(snoozedUntil = at("2026-10-31T14:00")) } // Odgodi 1 sat at 13:00
        sim.events.clear()
        sim.run("2026-10-31T13:00", "2026-10-31T14:59")
        assertEquals(listOf("31.10 14:00 DUE!"), sim.events)

        sim.updateOccurrence(1) { it.copy(snoozedUntil = at("2026-11-01T09:00")) } // Sutra ujutro at 15:00
        sim.events.clear()
        sim.run("2026-10-31T15:00", "2026-11-01T11:00")
        assertEquals(listOf("01.11 07:00 SUMMARY!", "01.11 09:00 OVERDUE!", "01.11 11:00 OVERDUE!"), sim.events)
    }

    @Test
    fun `G - one-off with a time is overdue at that time, on a grid anchored at it`() {
        val servis = task(id = 2, type = TaskType.ONE_OFF, dueTime = time("10:00"))
        val notifiedEarly = occurrence("2026-11-03", id = 1, taskId = 2).copy(lastNotifiedAt = at("2026-11-01T09:00"))
        val sim = ReminderSimulator(
            listOf(TaskWithOccurrences(servis, listOf(notifiedEarly))),
            Settings(lastSummaryDate = date("2026-11-03")),
        )
        sim.run("2026-11-03T08:00", "2026-11-04T10:00")
        assertEquals(
            listOf("03.11 09:00 DUE!") +
                listOf("10:00", "12:00", "14:00", "16:00", "18:00", "20:00").map { "03.11 $it OVERDUE!" } +
                listOf("04.11 07:00 OVERDUE!", "04.11 07:00 SUMMARY", "04.11 08:00 OVERDUE!", "04.11 10:00 OVERDUE!"),
            sim.events,
        )
    }
}
