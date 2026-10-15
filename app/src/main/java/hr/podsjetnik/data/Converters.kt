package hr.podsjetnik.data

import androidx.room3.ColumnTypeConverter
import hr.podsjetnik.domain.RecurrenceRule
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Room column converters (CODING_STANDARDS: Data): Instant → epoch millis,
 * LocalDate / LocalTime → ISO text, lists and rules → JSON.
 */
class Converters {
    @ColumnTypeConverter
    fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()

    @ColumnTypeConverter
    fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @ColumnTypeConverter
    fun dateToText(value: LocalDate?): String? = value?.toString()

    @ColumnTypeConverter
    fun textToDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @ColumnTypeConverter
    fun timeToText(value: LocalTime?): String? = value?.toString()

    @ColumnTypeConverter
    fun textToTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    @ColumnTypeConverter
    fun timesToJson(value: List<LocalTime>): String = Json.encodeToString(value.map(LocalTime::toString))

    @ColumnTypeConverter
    fun jsonToTimes(value: String): List<LocalTime> = Json.decodeFromString<List<String>>(value).map(LocalTime::parse)

    @ColumnTypeConverter
    fun ruleToJson(value: RecurrenceRule?): String? = value?.let { Json.encodeToString(it) }

    @ColumnTypeConverter
    fun jsonToRule(value: String?): RecurrenceRule? = value?.let { Json.decodeFromString<RecurrenceRule>(it) }
}
