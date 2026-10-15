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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock

class DetaljiViewModel(
    savedStateHandle: SavedStateHandle,
    repository: TaskRepository,
    clock: Clock,
) : ViewModel() {
    private val taskId = savedStateHandle.toRoute<DetaljiRoute>().taskId

    /** null while loading or when the task no longer exists. */
    val state: StateFlow<DetaljiUiState?> = combine(repository.tasks, repository.settings) { items, settings ->
        items.firstOrNull { it.task.id == taskId }?.let { buildDetalji(it, settings, clock.instant(), clock.zone) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = checkNotNull(this[APPLICATION_KEY]).container
                DetaljiViewModel(createSavedStateHandle(), container.taskRepository, container.clock)
            }
        }
    }
}
