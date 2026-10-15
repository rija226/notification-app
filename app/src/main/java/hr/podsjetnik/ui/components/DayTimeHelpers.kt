package hr.podsjetnik.ui.components

import java.time.LocalTime

// Shared by the task form and Postavke › Zadano za nove zadatke (HANDOFF §6.6, §6.7).

private const val MAX_DAY_TIMES = 6

/** Day times stay sorted, unique, at most 6. */
fun addDayTime(times: List<LocalTime>, time: LocalTime): List<LocalTime> =
    (times + time).distinct().sorted().take(MAX_DAY_TIMES)

/** At least one day time always remains. */
fun removeDayTime(times: List<LocalTime>, time: LocalTime): List<LocalTime> = if (times.size <= 1) times else times - time

fun canAddDayTime(times: List<LocalTime>): Boolean = times.size < MAX_DAY_TIMES

/** Early reminder days are 0 (off) to 7. */
fun stepEarlyDays(days: Int, delta: Int): Int = (days + delta).coerceIn(0, 7)
