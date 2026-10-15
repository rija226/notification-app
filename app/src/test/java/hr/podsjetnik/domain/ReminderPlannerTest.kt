package hr.podsjetnik.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ReminderPlannerTest {

    // --- HANDOFF §11, fixed clock Thursday 2026-11-05 10:24 -------------------------------------

    private val now = at("2026-11-05T10:24")

    private fun next(task: Task, occurrence: Occurrence, settings: Settings = Settings(), at: Instant = now) =
        nextReminderAt(task, occurrence, settings, at, ZAGREB)

    @Test
    fun `handoff table - next reminder of each sample task`() {
        val struja = task(id = 1)
        val servis = task(id = 2, type = TaskType.ONE_OFF, dueTime = time("10:00"))
        val racun = task(id = 3, recurrence = RecurrenceRule.DayOfMonth(5))
        val registracija = task(id = 4, type = TaskType.ONE_OFF, dueTime = time("09:00"))
        val rezije = task(id = 5, recurrence = RecurrenceRule.DayOfMonth(15))
        assertEquals(at("2026-11-05T11:00"), next(struja, occurrence("2026-10-31"))) // 5 days late → hourly
        assertEquals(at("2026-11-05T12:00"), next(servis, occurrence("2026-11-03"))) // 2 days late → every 2 h
        assertEquals(at("2026-11-05T13:00"), next(racun, occurrence("2026-11-05"))) // due today
        assertEquals(at("2026-11-08T09:00"), next(registracija, occurrence("2026-11-10"))) // early
        assertEquals(at("2026-11-13T09:00"), next(rezije, occurrence("2026-11-15"))) // early
    }

    @Test
    fun `handoff 2 - after done the next occurrence's early reminder is 28 11 at 09 00`() {
        val struja = task()
        val done = occurrence("2026-10-31").markDone(struja, now, ZAGREB)
        val created = occurrencesToCreate(struja, listOf(done), now.atZone(ZAGREB).toLocalDate(), now).single()
        assertEquals(date("2026-11-30"), created.dueDate)
        assertEquals(at("2026-11-28T09:00"), next(struja, created))
    }

    @Test
    fun `handoff 6 - snooze to tomorrow morning at 21 00 on the due day`() {
        val snoozed = occurrence("2026-10-31").copy(
            lastNotifiedAt = at("2026-10-31T20:00"),
            snoozedUntil = at("2026-11-01T09:00"),
        )
        assertEquals(at("2026-11-01T09:00"), next(task(), snoozed, at = at("2026-10-31T21:00")))
        val afterSnooze = snoozed.copy(lastNotifiedAt = at("2026-11-01T09:00"))
        assertEquals(at("2026-11-01T11:00"), next(task(), afterSnooze, at = at("2026-11-01T09:00")))
    }

    @Test
    fun `handoff 7 - escalation halves the interval from 3 days late, never below 1 h`() {
        assertEquals(2, overdueIntervalHours(2, 2))
        assertEquals(1, overdueIntervalHours(2, 3))
        assertEquals(1, overdueIntervalHours(1, 5))
        assertEquals(2, overdueIntervalHours(4, 3))
        assertEquals(4, overdueIntervalHours(4, 0))
    }

    @Test
    fun `grid keeps the anchor's minutes`() {
        assertEquals(listOf("01:00", "03:00", "05:00"), gridTimes(time("09:00"), 2).take(3).map { it.toString() })
        assertEquals(listOf("02:00", "06:00", "10:00"), gridTimes(time("10:00"), 4).take(3).map { it.toString() })
        assertEquals("00:30", gridTimes(time("09:30"), 1).first().toString())
        assertEquals(24, gridTimes(time("09:00"), 1).size)
    }

    @Test
    fun `handoff 8 - DST end keeps wall-clock day times`() {
        // 25. 10. 2026: clocks go back 03:00 → 02:00.
        assertEquals(at("2026-10-25T09:00"), next(task(earlyDays = 0), occurrence("2026-10-25"), at = at("2026-10-25T08:00")))
    }

    @Test
    fun `DST spring gap moves a grid slot forward by an hour`() {
        // 28. 3. 2027: 02:00 → 03:00. Grid 00, 02, 04 … for a one-off due 27. 3. at 10:00.
        val oneOff = task(type = TaskType.ONE_OFF, dueTime = time("10:00"))
        val occ = occurrence("2027-03-27").copy(lastNotifiedAt = Instant.parse("2027-03-27T23:00:00Z")) // 00:00 CET
        val settings = Settings(quietFrom = time("05:00"), quietTo = time("06:00"))
        val gapSlot = nextSlot(oneOff, occ, settings, occ.lastNotifiedAt!!, ZAGREB)
        assertEquals(Instant.parse("2027-03-28T01:00:00Z"), gapSlot) // 03:00 CEST
        assertEquals(Instant.parse("2027-03-28T02:00:00Z"), nextSlot(oneOff, occ, settings, gapSlot!!, ZAGREB)) // 04:00
    }

    @Test
    fun `DST autumn overlap fires the repeated hour once`() {
        // 25. 10. 2026: 02:00–03:00 happens twice. The 02:00 slot uses the earlier (summer) offset.
        val oneOff = task(type = TaskType.ONE_OFF, dueTime = time("10:00"))
        val occ = occurrence("2026-10-24")
        val settings = Settings(quietFrom = time("05:00"), quietTo = time("06:00"))
        val first = nextSlot(oneOff, occ, settings, Instant.parse("2026-10-24T23:30:00Z"), ZAGREB) // after 01:30 CEST
        assertEquals(Instant.parse("2026-10-25T00:00:00Z"), first) // 02:00 CEST
        assertEquals(Instant.parse("2026-10-25T03:00:00Z"), nextSlot(oneOff, occ, settings, first!!, ZAGREB)) // 04:00 CET
    }

    @Test
    fun `handoff 9 - no early reminder when earlyDays is 0`() {
        assertEquals(at("2026-10-31T09:00"), next(task(earlyDays = 0), occurrence("2026-10-31"), at = at("2026-10-28T10:00")))
    }

    @Test
    fun `handoff 10 - one-off due time not in day times still gets a reminder at it (overdue under H3)`() {
        val servis = task(id = 2, type = TaskType.ONE_OFF, dueTime = time("10:30"))
        val occ = occurrence("2026-11-03", taskId = 2).copy(lastNotifiedAt = at("2026-11-03T09:00"))
        val plan = planReminders(
            listOf(TaskWithOccurrences(servis, listOf(occ))),
            Settings(lastSummaryDate = date("2026-11-03")),
            at("2026-11-03T10:30"),
            ZAGREB,
        )
        assertEquals(Stage.OVERDUE, plan.posts.single().stage)
    }

    // --- Engine-facing behavior ------------------------------------------------------------------

    private val day = Settings(lastSummaryDate = date("2026-10-31"))

    private fun plan(occurrence: Occurrence, at: String, task: Task = task(), settings: Settings = day) =
        planReminders(listOf(TaskWithOccurrences(task, listOf(occurrence))), settings, at(at), ZAGREB)

    @Test
    fun `missed slots while the phone was off produce one post`() {
        val occ = occurrence("2026-10-31", id = 7).copy(lastNotifiedAt = at("2026-10-31T09:00"))
        val result = plan(occ, "2026-10-31T19:30") // missed 13:00 and 17:00
        assertEquals(1, result.posts.size)
        assertEquals(at("2026-10-31T19:30"), result.posts.single().notifiedAt)
        assertEquals(at("2026-10-31T20:00"), result.nextWakeAt)
    }

    @Test
    fun `planning twice at the same moment posts once`() {
        val occ = occurrence("2026-10-31", id = 7).copy(lastNotifiedAt = at("2026-10-31T09:00"))
        val first = plan(occ, "2026-10-31T13:00")
        val afterPost = occ.copy(lastNotifiedAt = first.posts.single().notifiedAt)
        assertTrue(plan(afterPost, "2026-10-31T13:00").posts.isEmpty())
    }

    @Test
    fun `alarm a few seconds early still fires the slot, and only once`() {
        val occ = occurrence("2026-10-31", id = 7).copy(lastNotifiedAt = at("2026-10-31T09:00"))
        val early = plan(occ, "2026-10-31T12:59:30")
        assertEquals(at("2026-10-31T13:00"), early.posts.single().notifiedAt)
        val afterPost = occ.copy(lastNotifiedAt = early.posts.single().notifiedAt)
        assertTrue(plan(afterPost, "2026-10-31T13:00").posts.isEmpty())
    }

    @Test
    fun `new task created after today's day times does not fire immediately`() {
        val created = occurrence("2026-10-31", id = 7, createdAt = at("2026-10-31T14:30"))
        val result = plan(created, "2026-10-31T14:30")
        assertTrue(result.posts.isEmpty())
        assertEquals(at("2026-10-31T17:00"), result.nextWakeAt)
    }

    @Test
    fun `edit sets lastNotifiedAt to now, so past slots of the new schedule stay silent`() {
        val edited = occurrence("2026-10-31", id = 7).copy(lastNotifiedAt = at("2026-10-31T14:30"))
        assertTrue(plan(edited, "2026-10-31T14:30").posts.isEmpty())
    }

    @Test
    fun `paused task gets nothing and loses its notification`() {
        val result = plan(occurrence("2026-10-31", id = 7), "2026-10-31T13:00", task = task(pausedAt = at("2026-10-20T10:00")))
        assertTrue(result.posts.isEmpty())
        assertFalse(7L in result.activeOccurrenceIds)
    }

    @Test
    fun `closed occurrence is not active`() {
        val done = occurrence("2026-10-31", id = 7, status = OccurrenceStatus.DONE, completedAt = at("2026-10-31T10:00"))
        assertTrue(plan(done, "2026-10-31T13:00").activeOccurrenceIds.isEmpty())
    }

    @Test
    fun `two tasks at the same minute make one sound`() {
        val a = TaskWithOccurrences(task(id = 1), listOf(occurrence("2026-10-31", id = 1, taskId = 1)))
        val b = TaskWithOccurrences(task(id = 2), listOf(occurrence("2026-10-31", id = 2, taskId = 2)))
        val result = planReminders(listOf(a, b), day, at("2026-10-31T09:00"), ZAGREB)
        assertEquals(2, result.posts.size)
        assertEquals(1, result.posts.count { it.alert })
    }

    @Test
    fun `quiet hours off - night slots fire at night`() {
        val occ = occurrence("2026-10-31", id = 7).copy(lastNotifiedAt = at("2026-11-01T21:00"))
        val off = day.copy(quietEnabled = false, lastSummaryDate = date("2026-11-01"))
        assertEquals(at("2026-11-01T23:00"), plan(occ, "2026-11-01T21:00", settings = off).nextWakeAt)
    }

    @Test
    fun `quiet hours on - nothing is posted inside them`() {
        val occ = occurrence("2026-10-31", id = 7).copy(lastNotifiedAt = at("2026-11-01T21:00"))
        val result = plan(occ, "2026-11-01T23:00", settings = day.copy(lastSummaryDate = date("2026-11-01")))
        assertTrue(result.posts.isEmpty())
        assertEquals(at("2026-11-02T07:00"), result.nextWakeAt)
    }

    @Test
    fun `summary counts today and overdue, lists overdue first, once per day`() {
        val items = listOf(
            TaskWithOccurrences(task(id = 1).copy(title = "Racun"), listOf(occurrence("2026-11-05", id = 1, taskId = 1))),
            TaskWithOccurrences(task(id = 2).copy(title = "Struja"), listOf(occurrence("2026-10-31", id = 2, taskId = 2))),
        )
        val morning = planReminders(items, Settings(lastSummaryDate = date("2026-11-04")), at("2026-11-05T07:00"), ZAGREB)
        val summary = morning.summary!!
        assertEquals(1, summary.dueToday)
        assertEquals(1, summary.overdue)
        assertEquals(listOf("Struja", "Racun"), summary.titles)
        assertEquals(date("2026-11-05"), morning.summaryDate)

        val later = planReminders(items, Settings(lastSummaryDate = date("2026-11-05")), at("2026-11-05T07:00"), ZAGREB)
        assertNull(later.summary)
    }

    @Test
    fun `summary is skipped but marked handled when nothing is due`() {
        val items = listOf(TaskWithOccurrences(task(), listOf(occurrence("2026-11-30", id = 1))))
        val morning = planReminders(items, Settings(lastSummaryDate = date("2026-11-04")), at("2026-11-05T07:00"), ZAGREB)
        assertNull(morning.summary)
        assertEquals(date("2026-11-05"), morning.summaryDate)
    }

    @Test
    fun `next wake includes tomorrow's summary when nothing else is planned`() {
        val result = planReminders(emptyList(), Settings(lastSummaryDate = date("2026-11-05")), at("2026-11-05T10:00"), ZAGREB)
        assertEquals(at("2026-11-06T07:00"), result.nextWakeAt)
    }

    @Test
    fun `early reminder never makes a sound`() {
        val result = plan(occurrence("2026-10-31", id = 7), "2026-10-29T09:00", settings = Settings(lastSummaryDate = date("2026-10-29")))
        assertEquals(Stage.EARLY, result.posts.single().stage)
        assertFalse(result.posts.single().alert)
    }
}
