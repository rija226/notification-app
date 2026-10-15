package hr.podsjetnik.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import hr.podsjetnik.R
import hr.podsjetnik.domain.OccurrenceState
import hr.podsjetnik.domain.RecurrenceRule
import hr.podsjetnik.domain.StatusKind
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Chip text for a status (HANDOFF §4.2, PRD §5.4). */
@Composable
fun statusText(state: OccurrenceState): String = when (state.kind) {
    StatusKind.OVERDUE ->
        if (state.days == 0) {
            stringResource(R.string.chip_rok_istekao)
        } else {
            pluralStringResource(R.plurals.chip_kasni, state.days, state.days)
        }
    StatusKind.TODAY -> stringResource(R.string.chip_danas)
    StatusKind.UPCOMING, StatusKind.LATER -> pluralStringResource(R.plurals.chip_za, state.days, state.days)
    StatusKind.DONE -> stringResource(R.string.chip_odradjeno)
}

/** "Zadnji dan u mjesecu" / "Svakog 5. u mjesecu". */
@Composable
fun ruleText(rule: RecurrenceRule): String = when (rule) {
    is RecurrenceRule.DayOfMonth -> stringResource(R.string.pravilo_svakog_dana, rule.day)
    RecurrenceRule.LastDayOfMonth -> stringResource(R.string.pravilo_zadnji_dan)
}

/** "čet, 5. 11." */
@Composable
fun shortDateText(date: LocalDate, today: LocalDate): String =
    formatShortDate(date, today, stringArrayResource(R.array.dani_kratko).toList())

/** "čet, 5. 11." or "uto, 3. 11. u 10:00". */
@Composable
fun dateTimeText(date: LocalDate, time: LocalTime?, today: LocalDate): String {
    val dateText = shortDateText(date, today)
    return time?.let { stringResource(R.string.meta_datum_vrijeme, dateText, formatTime(it)) } ?: dateText
}

/** Next reminder label: "11:00" today, otherwise "8. 11." (HANDOFF §6.1). */
@Composable
fun reminderLabel(at: LocalDateTime, today: LocalDate): String =
    if (at.toLocalDate() == today) formatTime(at.toLocalTime()) else formatDate(at.toLocalDate(), today)

/** "svaki sat" / "svaka 2 sata" / "svaka 4 sata". */
@Composable
fun intervalText(hours: Int): String = stringResource(
    when (hours) {
        1 -> R.string.interval_1h
        2 -> R.string.interval_2h
        else -> R.string.interval_4h
    },
)
