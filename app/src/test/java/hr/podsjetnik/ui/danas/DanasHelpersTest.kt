package hr.podsjetnik.ui.danas

import hr.podsjetnik.domain.OccurrenceStatus
import hr.podsjetnik.domain.RecurrenceRule
import hr.podsjetnik.domain.Settings
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.domain.TaskWithOccurrences
import hr.podsjetnik.domain.ZAGREB
import hr.podsjetnik.domain.at
import hr.podsjetnik.domain.date
import hr.podsjetnik.domain.occurrence
import hr.podsjetnik.domain.task
import hr.podsjetnik.domain.time
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/** HANDOFF §11 expected Danas at Thursday 2026-11-05 10:24. */
class DanasHelpersTest {
    private val now = at("2026-11-05T10:24")

    private val sample = listOf(
        TaskWithOccurrences(task(id = 1).copy(title = "Očitati struju"), listOf(occurrence("2026-10-31", id = 1, taskId = 1))),
        TaskWithOccurrences(
            task(id = 2, type = TaskType.ONE_OFF, dueTime = time("10:00")).copy(title = "Nazvati servis za klimu"),
            listOf(occurrence("2026-11-03", id = 2, taskId = 2)),
        ),
        TaskWithOccurrences(
            task(id = 3, recurrence = RecurrenceRule.DayOfMonth(5)).copy(title = "Poslati račun knjigovođi"),
            listOf(occurrence("2026-11-05", id = 3, taskId = 3)),
        ),
        TaskWithOccurrences(
            task(id = 4, type = TaskType.ONE_OFF, dueTime = time("09:00")).copy(title = "Produžiti registraciju auta"),
            listOf(occurrence("2026-11-10", id = 4, taskId = 4)),
        ),
        TaskWithOccurrences(
            task(id = 5, recurrence = RecurrenceRule.DayOfMonth(15)).copy(title = "Platiti režije"),
            listOf(occurrence("2026-11-15", id = 5, taskId = 5)),
        ),
    )

    @Test
    fun `sections and next reminders match the handoff`() {
        val sections = buildDanas(sample, Settings(), now, ZAGREB)
        assertEquals(listOf(1L, 2L), sections.overdue.map { it.taskId }) // 5 days late first, then 2
        assertEquals(listOf(LocalDateTime.parse("2026-11-05T11:00"), LocalDateTime.parse("2026-11-05T12:00")), sections.overdue.map { it.nextReminder })
        assertEquals(listOf(3L), sections.today.map { it.taskId })
        assertEquals(LocalDateTime.parse("2026-11-05T13:00"), sections.today.single().nextReminder)
        assertEquals(listOf(4L), sections.upcoming.map { it.taskId })
        assertEquals(LocalDateTime.parse("2026-11-08T09:00"), sections.upcoming.single().nextReminder)
        assertNull(sections.next) // Režije (10 days) is only in Svi zadaci
    }

    @Test
    fun `paused tasks are hidden`() {
        val paused = listOf(TaskWithOccurrences(task(pausedAt = now), listOf(occurrence("2026-10-31", id = 1))))
        assertTrue(buildDanas(paused, Settings(), now, ZAGREB).isEmpty)
    }

    @Test
    fun `empty Danas shows the nearest later task`() {
        val later = sample.drop(4)
        val sections = buildDanas(later, Settings(), now, ZAGREB)
        assertTrue(sections.isEmpty)
        assertEquals(5L, sections.next?.taskId)
    }

    @Test
    fun `two open months of one task are two cards`() {
        val twoMonths = listOf(
            TaskWithOccurrences(
                task(id = 1),
                listOf(
                    occurrence("2026-10-31", id = 1, taskId = 1),
                    occurrence("2026-11-30", id = 2, taskId = 1),
                    occurrence("2026-09-30", id = 3, taskId = 1, status = OccurrenceStatus.DONE),
                ),
            ),
        )
        val sections = buildDanas(twoMonths, Settings(), at("2026-11-25T10:00"), ZAGREB)
        assertEquals(listOf(date("2026-10-31")), sections.overdue.map { it.dueDate })
        assertEquals(listOf(date("2026-11-30")), sections.upcoming.map { it.dueDate })
    }
}
