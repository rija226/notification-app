package hr.podsjetnik.ui.components

import java.time.LocalDate
import java.time.LocalTime

// Croatian date formats from HANDOFF §8. Weekday/month names come from strings.xml arrays.

/** "30. 11." in the current year, "3. 11. 2027." otherwise. */
fun formatDate(date: LocalDate, today: LocalDate): String =
    if (date.year == today.year) "${date.dayOfMonth}. ${date.monthValue}." else "${date.dayOfMonth}. ${date.monthValue}. ${date.year}."

/** "čet, 5. 11." — [weekdaysShort] is Monday-first (R.array.dani_kratko). */
fun formatShortDate(date: LocalDate, today: LocalDate, weekdaysShort: List<String>): String =
    "${weekdaysShort[date.dayOfWeek.value - 1]}, ${formatDate(date, today)}"

/** "uto, 3. 11. 2026." — date field in the form always shows the year (screen 06b). */
fun formatShortDateWithYear(date: LocalDate, weekdaysShort: List<String>): String =
    "${weekdaysShort[date.dayOfWeek.value - 1]}, ${date.dayOfMonth}. ${date.monthValue}. ${date.year}."

/** "Četvrtak, 5. studenoga" — Danas header (R.array.dani_dugo, R.array.mjeseci_genitiv). */
fun formatLongDate(date: LocalDate, weekdaysLong: List<String>, monthsGenitive: List<String>): String =
    "${weekdaysLong[date.dayOfWeek.value - 1].replaceFirstChar { it.uppercase() }}, " +
        "${date.dayOfMonth}. ${monthsGenitive[date.monthValue - 1]}"

/** 24 h "09:00". */
fun formatTime(time: LocalTime): String = "%02d:%02d".format(time.hour, time.minute)
