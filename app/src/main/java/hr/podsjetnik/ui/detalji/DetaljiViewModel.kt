package hr.podsjetnik.ui.detalji

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import hr.podsjetnik.container
import hr.podsjetnik.data.TaskRepository
import hr.podsjetnik.ui.DetaljiRoute
import hr.podsjetnik.ui.DoneOutcome
import hr.podsjetnik.ui.TaskActions
import hr.podsjetnik.ui.components.minuteTicks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDateTime

class DetaljiViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: TaskRepository,
    private val actions: TaskActions,
    clock: Clock,
) : ViewModel() {
    private val taskId = savedStateHandle.toRoute<DetaljiRoute>().taskId

    /** null while loading or when the task no longer exists. */
    val state: StateFlow<DetaljiUiState?> = combine(repository.tasks, repository.settings, minuteTicks()) { items, settings, _ ->
        items.firstOrNull { it.task.id == taskId }?.let { buildDetalji(it, settings, clock.instant(), clock.zone) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _outcome = MutableStateFlow<DoneOutcome?>(null)
    val outcome: StateFlow<DoneOutcome?> = _outcome.asStateFlow()

    fun done(occurrenceId: Long) {
        viewModelScope.launch { _outcome.value = actions.done(occurrenceId) }
    }

    fun undo(occurrenceId: Long) {
        _outcome.value = null
        viewModelScope.launch { actions.undo(occurrenceId) }
    }

    fun outcomeShown() {
        _outcome.value = null
    }

    fun snooze(occurrenceId: Long, until: LocalDateTime) {
        viewModelScope.launch { actions.snooze(occurrenceId, until) }
    }

    fun skip(occurrenceId: Long) {
        viewModelScope.launch { repository.skip(occurrenceId) }
    }

    fun setPaused(paused: Boolean) {
        viewModelScope.launch { if (paused) repository.pause(taskId) else repository.resume(taskId) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            onDeleted()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = checkNotNull(this[APPLICATION_KEY]).container
                DetaljiViewModel(createSavedStateHandle(), container.taskRepository, container.taskActions, container.clock)
            }
        }
    }
}
