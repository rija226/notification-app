package hr.podsjetnik.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.YearMonth

/**
 * Recurrence rules (PRD FR-2). Stored as JSON, e.g. {"type":"dayOfMonth","day":5}.
 * A new rule type (v2: weekly, yearly, ...) = new subtype + tests, no DB migration.
 */
@Serializable
sealed interface RecurrenceRule {
    /** First date matching the rule on or after [date]. */
    fun firstOnOrAfter(date: LocalDate): LocalDate

    /** First date matching the rule strictly after [date]. */
    fun nextAfter(date: LocalDate): LocalDate = firstOnOrAfter(date.plusDays(1))

    /** "Svakog N." — N is 1..28 so every month has it; 29–31 use [LastDayOfMonth]. */
    @Serializable
    @SerialName("dayOfMonth")
    data class DayOfMonth(val day: Int) : RecurrenceRule {
        init {
            require(day in 1..28) { "day must be 1..28, was $day" }
        }

        override fun firstOnOrAfter(date: LocalDate): LocalDate {
            val inMonth = date.withDayOfMonth(day)
            return if (inMonth < date) inMonth.plusMonths(1) else inMonth
        }
    }

    @Serializable
    @SerialName("lastDayOfMonth")
    data object LastDayOfMonth : RecurrenceRule {
        override fun firstOnOrAfter(date: LocalDate): LocalDate = YearMonth.from(date).atEndOfMonth()
    }
}
