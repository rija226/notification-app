package hr.podsjetnik.domain

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** True when [time] is inside [from, to). The window may cross midnight (22:00–07:00). */
fun isInQuietHours(time: LocalTime, from: LocalTime, to: LocalTime): Boolean =
    if (from < to) time >= from && time < to else time >= from || time < to

/**
 * Moves an instant inside quiet hours to the end of the window ("Do"), PRD §5.6.
 * Instants outside the window, or with quiet hours off, are returned unchanged.
 */
fun deferPastQuietHours(instant: Instant, settings: Settings, zone: ZoneId): Instant {
    if (!settings.quietEnabled) return instant
    val local = instant.atZone(zone).toLocalDateTime()
    val time = local.toLocalTime()
    if (!isInQuietHours(time, settings.quietFrom, settings.quietTo)) return instant
    val crossesMidnight = settings.quietFrom > settings.quietTo
    val endDate = if (crossesMidnight && time >= settings.quietFrom) local.toLocalDate().plusDays(1) else local.toLocalDate()
    return ZonedDateTime.of(endDate, settings.quietTo, zone).toInstant()
}
