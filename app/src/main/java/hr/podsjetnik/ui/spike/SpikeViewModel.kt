package hr.podsjetnik.ui.spike

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import hr.podsjetnik.container
import hr.podsjetnik.data.AlarmLogDao
import hr.podsjetnik.data.AlarmLogEntity
import hr.podsjetnik.data.AlarmMode
import hr.podsjetnik.data.DebugSeed
import hr.podsjetnik.reminders.SpikeReminders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration

data class SpikeUiState(
    val log: List<AlarmLogEntity> = emptyList(),
    val mode: AlarmMode = AlarmMode.EXACT_WHILE_IDLE,
)

class SpikeViewModel(
    private val reminders: SpikeReminders,
    dao: AlarmLogDao,
    /** Loads handoff sample data; null in release builds (no asset). */
    private val seed: (suspend () -> Unit)?,
) : ViewModel() {
    val seedAvailable = seed != null

    fun loadSeed() {
        seed?.let { viewModelScope.launch { it() } }
    }

    private val mode = MutableStateFlow(AlarmMode.EXACT_WHILE_IDLE)

    val state: StateFlow<SpikeUiState> = combine(dao.observeRecent(), mode, ::SpikeUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpikeUiState())

    fun setMode(value: AlarmMode) {
        mode.value = value
    }

    fun addTest(minutes: Long) = add(listOf(Duration.ofMinutes(minutes)))

    fun addNightSeries() = add(nightSeriesDelays())

    fun clearPending() {
        viewModelScope.launch { reminders.clearPending() }
    }

    private fun add(delays: List<Duration>) {
        viewModelScope.launch { reminders.addTests(delays, mode.value) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val container = app.container
                val seed: (suspend () -> Unit)? = if (DebugSeed.isAvailable(app)) {
                    { DebugSeed.load(app, container.taskRepository) }
                } else {
                    null
                }
                SpikeViewModel(container.spikeReminders, container.database.alarmLogDao(), seed)
            }
        }
    }
}
