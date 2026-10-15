package hr.podsjetnik.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import hr.podsjetnik.domain.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Settings in DataStore Preferences (PRD FR-8). Missing keys fall back to the defaults in [Settings]. */
class SettingsStore(context: Context) {
    private val store = context.applicationContext.settingsDataStore

    val settings: Flow<Settings> = store.data.map { it.toSettings() }

    suspend fun current(): Settings = settings.first()

    suspend fun update(transform: (Settings) -> Settings) {
        store.edit { prefs -> prefs.write(transform(prefs.toSettings())) }
    }
}

private object Keys {
    val quietEnabled = booleanPreferencesKey("quietEnabled")
    val quietFrom = stringPreferencesKey("quietFrom")
    val quietTo = stringPreferencesKey("quietTo")
    val defaultDayTimes = stringPreferencesKey("defaultDayTimes")
    val defaultEarlyDays = intPreferencesKey("defaultEarlyDays")
    val defaultEarlyTime = stringPreferencesKey("defaultEarlyTime")
    val defaultOverdueIntervalHours = intPreferencesKey("defaultOverdueIntervalHours")
    val onboardingCompleted = booleanPreferencesKey("onboardingCompleted")
    val samsungSetupConfirmed = booleanPreferencesKey("samsungSetupConfirmed")
    val lastSummaryDate = stringPreferencesKey("lastSummaryDate")
}

private fun Preferences.toSettings(): Settings {
    val defaults = Settings()
    return Settings(
        quietEnabled = this[Keys.quietEnabled] ?: defaults.quietEnabled,
        quietFrom = this[Keys.quietFrom]?.let(LocalTime::parse) ?: defaults.quietFrom,
        quietTo = this[Keys.quietTo]?.let(LocalTime::parse) ?: defaults.quietTo,
        defaultDayTimes = this[Keys.defaultDayTimes]?.split(",")?.map(LocalTime::parse) ?: defaults.defaultDayTimes,
        defaultEarlyDays = this[Keys.defaultEarlyDays] ?: defaults.defaultEarlyDays,
        defaultEarlyTime = this[Keys.defaultEarlyTime]?.let(LocalTime::parse) ?: defaults.defaultEarlyTime,
        defaultOverdueIntervalHours = this[Keys.defaultOverdueIntervalHours] ?: defaults.defaultOverdueIntervalHours,
        onboardingCompleted = this[Keys.onboardingCompleted] ?: defaults.onboardingCompleted,
        samsungSetupConfirmed = this[Keys.samsungSetupConfirmed] ?: defaults.samsungSetupConfirmed,
        lastSummaryDate = this[Keys.lastSummaryDate]?.let(LocalDate::parse),
    )
}

private fun MutablePreferences.write(settings: Settings) {
    this[Keys.quietEnabled] = settings.quietEnabled
    this[Keys.quietFrom] = settings.quietFrom.toString()
    this[Keys.quietTo] = settings.quietTo.toString()
    this[Keys.defaultDayTimes] = settings.defaultDayTimes.joinToString(",")
    this[Keys.defaultEarlyDays] = settings.defaultEarlyDays
    this[Keys.defaultEarlyTime] = settings.defaultEarlyTime.toString()
    this[Keys.defaultOverdueIntervalHours] = settings.defaultOverdueIntervalHours
    this[Keys.onboardingCompleted] = settings.onboardingCompleted
    this[Keys.samsungSetupConfirmed] = settings.samsungSetupConfirmed
    val summary = settings.lastSummaryDate
    if (summary == null) remove(Keys.lastSummaryDate) else this[Keys.lastSummaryDate] = summary.toString()
}
