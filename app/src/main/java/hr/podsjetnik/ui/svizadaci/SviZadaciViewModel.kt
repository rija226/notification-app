package hr.podsjetnik.ui.svizadaci

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import hr.podsjetnik.container
import hr.podsjetnik.data.TaskRepository
import hr.podsjetnik.domain.TaskType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate

data class SviZadaciUiState(
    val recurring: List<TaskRow> = emptyList(),
    val oneOff: List<TaskRow> = emptyList(),
    val today: LocalDate = LocalDate.now(),
)

class SviZadaciViewModel(repository: TaskRepository, clock: Clock) : ViewModel() {
    val state: StateFlow<SviZadaciUiState> = repository.tasks
        .map { items ->
            val now = clock.instant()
            SviZadaciUiState(
                recurring = buildRows(items, TaskType.RECURRING, now, clock.zone),
                oneOff = buildRows(items, TaskType.ONE_OFF, now, clock.zone),
                today = LocalDate.now(clock),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SviZadaciUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = checkNotNull(this[APPLICATION_KEY]).container
                SviZadaciViewModel(container.taskRepository, container.clock)
            }
        }
    }
}
