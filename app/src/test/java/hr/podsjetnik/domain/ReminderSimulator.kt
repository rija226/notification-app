package hr.podsjetnik.domain

import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Test-only stand-in for the Phase 4 engine: plan → "post" → persist → sleep until nextWakeAt.
 * Records each post as "31.10 09:00 DUE!" ("!" = makes a sound).
 */
class ReminderSimulator(var items: List<TaskWithOccurrences>, var settings: Settings = Settings()) {
    val events = mutableListOf<String>()
    private val format = DateTimeFormatter.ofPattern("dd.MM HH:mm")

    /** Runs every wake-up from [from] up to and including [until]. */
    fun run(from: String, until: String) {
        var now = at(from)
        val end = at(until)
        repeat(10_000) {
            step(now)
            val next = planReminders(items, settings, now, ZAGREB).nextWakeAt ?: return
            if (next > end) return
            now = maxOf(next, now.plusSeconds(1))
        }
        error("simulation did not settle")
    }

    /** One engine refresh at [now] (also what a user action triggers). */
    fun step(now: Instant) {
        ensureOccurrences(now)
        val plan = planReminders(items, settings, now, ZAGREB)
        val stamp = format.format(now.atZone(ZAGREB))
        plan.posts.forEach { post ->
            events += "$stamp ${post.stage}${if (post.alert) "!" else ""}"
            updateOccurrence(post.occurrenceId) { it.copy(lastNotifiedAt = post.notifiedAt) }
        }
        plan.summary?.let { events += "$stamp SUMMARY${if (it.alert) "!" else ""}" }
        plan.summaryDate?.let { settings = settings.copy(lastSummaryDate = it) }
    }

    fun updateOccurrence(id: Long, change: (Occurrence) -> Occurrence) {
        items = items.map { item -> item.copy(occurrences = item.occurrences.map { if (it.id == id) change(it) else it }) }
    }

    fun occurrence(dueDate: String): Occurrence =
        items.flatMap { it.occurrences }.first { it.dueDate == LocalDate.parse(dueDate) }

    private fun ensureOccurrences(now: Instant) {
        val today = now.atZone(ZAGREB).toLocalDate()
        var nextId = items.flatMap { it.occurrences }.maxOfOrNull { it.id } ?: 0
        items = items.map { item ->
            val created = occurrencesToCreate(item.task, item.occurrences, today, now).map { it.copy(id = ++nextId) }
            item.copy(occurrences = item.occurrences + created)
        }
    }
}
