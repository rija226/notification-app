package hr.podsjetnik.ui.postavke

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import hr.podsjetnik.R
import hr.podsjetnik.container
import hr.podsjetnik.data.AlarmLogDao
import hr.podsjetnik.data.TaskRepository
import hr.podsjetnik.data.backup.BackupContent
import hr.podsjetnik.data.backup.BackupRepository
import hr.podsjetnik.data.backup.InvalidBackupException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val backup: BackupRepository,
    alarmLog: AlarmLogDao,
    clock: Clock,
) : ViewModel() {
    val state: StateFlow<PostavkeUiState?> = combine(repository.settings, alarmLog.observeRecent()) { settings, log ->
        PostavkeUiState(settings, hasLateAlarm(log, clock.instant()))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _pendingImport = MutableStateFlow<BackupContent?>(null)

    /** A validated backup waiting for the user's "Zamijeniti sve podatke?" confirmation. */
    val pendingImport: StateFlow<BackupContent?> = _pendingImport.asStateFlow()

    private val _message = MutableStateFlow<Int?>(null)

    /** Result to show once as a snackbar (string resource). */
    val message: StateFlow<Int?> = _message.asStateFlow()

    fun backupFileName(): String = backup.suggestedFileName()

    fun export(uri: Uri) {
        viewModelScope.launch {
            _message.value = runCatching { backup.export(uri) }
                .fold(onSuccess = { R.string.izvoz_uspjeh }, onFailure = { R.string.izvoz_greska })
        }
    }

    fun readImport(uri: Uri) {
        viewModelScope.launch {
            try {
                _pendingImport.value = backup.read(uri)
            } catch (e: InvalidBackupException) {
                _message.value = R.string.uvoz_greska
            }
        }
    }

    fun confirmImport() {
        val content = _pendingImport.value ?: return
        _pendingImport.value = null
        viewModelScope.launch {
            // ponytail: any failure here (e.g. a constraint) rolls the transaction back; old data stays.
            _message.value = runCatching { backup.apply(content) }
                .fold(onSuccess = { R.string.uvoz_uspjeh }, onFailure = { R.string.uvoz_greska })
        }
    }

    fun cancelImport() {
        _pendingImport.value = null
    }

    fun messageShown() {
        _message.value = null
    }

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
                PostavkeViewModel(container.taskRepository, container.backupRepository, container.database.alarmLogDao(), container.clock)
            }
        }
    }
}
