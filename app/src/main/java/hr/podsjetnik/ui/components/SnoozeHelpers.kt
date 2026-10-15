package hr.podsjetnik.ui.components

import java.time.LocalDateTime
import java.time.LocalTime

enum class SnoozeKind { ONE_HOUR, TONIGHT, TOMORROW_MORNING }

data class SnoozeOption(val kind: SnoozeKind, val at: LocalDateTime)

private val TONIGHT = LocalTime.of(20, 0)
private val TONIGHT_CUTOFF = LocalTime.of(19, 30)
private val TOMORROW_MORNING = LocalTime.of(9, 0)

/**
 * Odgodi options (HANDOFF §4.3 point 5, §6.3): 1 sat, Večeras (20:00, hidden from 19:30, O7),
 * Sutra ujutro (09:00).
 */
fun snoozeOptions(now: LocalDateTime): List<SnoozeOption> = buildList {
    add(SnoozeOption(SnoozeKind.ONE_HOUR, now.plusHours(1).withSecond(0).withNano(0)))
    if (now.toLocalTime() < TONIGHT_CUTOFF) add(SnoozeOption(SnoozeKind.TONIGHT, now.toLocalDate().atTime(TONIGHT)))
    add(SnoozeOption(SnoozeKind.TOMORROW_MORNING, now.toLocalDate().plusDays(1).atTime(TOMORROW_MORNING)))
}
