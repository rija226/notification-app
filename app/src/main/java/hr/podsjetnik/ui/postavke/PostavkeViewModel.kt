package hr.podsjetnik.ui.postavke

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import hr.podsjetnik.container
import hr.podsjetnik.data.AlarmLogDao
import hr.podsjetnik.data.TaskRepository
import hr.podsjetnik.domain.Settings
import hr.podsjetnik.ui.components.addDayTime
import hr.podsjetnik.ui.components.removeDayTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalTime

data class PostavkeUiState(val settings: Settings, val lateAlarms: Boolean)

class PostavkeViewModel(
    private val repository: TaskRepository,
    alarmLog: AlarmLogDao,
    clock: Clock,
) : ViewModel() {
    val state: StateFlow<PostavkeUiState?> = combine(repository.settings, alarmLog.observeRecent()) { settings, log ->
        PostavkeUiState(settings, hasLateAlarm(log, clock.instant()))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun update(change: (Settings) -> Settings) {
        viewModelScope.launch { repository.updateSettings(change) }
    }

    fun setQuietEnabled(on: Boolean) = update { it.copy(quietEnabled = on) }

    fun setQuietFrom(time: LocalTime) = update { if (isValidQuietRange(time, it.quietTo)) it.copy(quietFrom = time) else it }

    fun setQuietTo(time: LocalTime) = update { if (isValidQuietRange(it.quietFrom, time)) it.copy(quietTo = time) else it }

    fun addDefaultDayTime(time: LocalTime) = update { it.copy(defaultDayTimes = addDayTime(it.defaultDayTimes, time)) }

    fun removeDefaultDayTime(time: LocalTime) = update { it.copy(defaultDayTimes = removeDayTime(it.defaultDayTimes, time)) }

    fun setDefaultEarly(days: Int, time: LocalTime) = update { it.copy(defaultEarlyDays = days, defaultEarlyTime = time) }

    fun setDefaultInterval(hours: Int) = update { it.copy(defaultOverdueIntervalHours = hours) }

    fun setSamsungConfirmed(confirmed: Boolean) = update { it.copy(samsungSetupConfirmed = confirmed) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = checkNotNull(this[APPLICATION_KEY]).container
                PostavkeViewModel(container.taskRepository, container.database.alarmLogDao(), container.clock)
            }
        }
    }
}
