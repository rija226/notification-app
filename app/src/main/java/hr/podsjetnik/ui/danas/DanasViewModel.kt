package hr.podsjetnik.ui.danas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import hr.podsjetnik.container
import hr.podsjetnik.data.TaskRepository
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

data class DanasUiState(val sections: DanasSections, val now: LocalDateTime)

class DanasViewModel(
    repository: TaskRepository,
    private val actions: TaskActions,
    private val clock: Clock,
) : ViewModel() {
    /** null until the first load, so the empty state doesn't flash. */
    val state: StateFlow<DanasUiState?> = combine(repository.tasks, repository.settings, minuteTicks()) { items, settings, _ ->
        DanasUiState(buildDanas(items, settings, clock.instant(), clock.zone), LocalDateTime.now(clock))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _outcome = MutableStateFlow<DoneOutcome?>(null)

    /** Odrađeno result to show: dialog (Stalni) or snackbar (Povremeni). */
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

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = checkNotNull(this[APPLICATION_KEY]).container
                DanasViewModel(container.taskRepository, container.taskActions, container.clock)
            }
        }
    }
}
