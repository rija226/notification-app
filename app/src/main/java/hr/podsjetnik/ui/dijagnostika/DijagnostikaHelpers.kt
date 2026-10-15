package hr.podsjetnik.ui.dijagnostika

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val LOG_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("d. M. HH:mm:ss")

fun formatLogTime(instant: Instant, zone: ZoneId): String = LOG_TIME.format(instant.atZone(zone))

/** Fire delay as "+12 s" / "-3 s" / "+2 min 5 s"; null when not fired yet. */
fun formatDelay(scheduledFor: Instant, firedAt: Instant?): String? {
    if (firedAt == null) return null
    val seconds = Duration.between(scheduledFor, firedAt).seconds
    val sign = if (seconds < 0) "-" else "+"
    val absolute = abs(seconds)
    return if (absolute < 60) "$sign$absolute s" else "$sign${absolute / 60} min ${absolute % 60} s"
}
