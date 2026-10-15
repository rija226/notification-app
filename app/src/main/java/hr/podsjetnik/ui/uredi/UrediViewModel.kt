package hr.podsjetnik.ui.uredi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import hr.podsjetnik.container
import hr.podsjetnik.data.TaskRepository
import hr.podsjetnik.domain.TaskType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

data class UrediUiState(
    val form: UrediForm,
    val today: LocalDate,
    val nextDue: LocalDate,
    val titleError: Boolean,
)

class UrediViewModel(private val repository: TaskRepository, private val clock: Clock) : ViewModel() {
    private val today = LocalDate.now(clock)
    private val form = MutableStateFlow(initialForm(today))

    val state: StateFlow<UrediUiState> = form
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, form.value.toUiState())

    fun onTitle(value: String) = form.update { it.copy(title = value) }

    fun onType(value: TaskType) = form.update { it.copy(type = value) }

    fun onDay(value: Int) = form.update { it.copy(day = value, lastDay = false) }

    fun onLastDay(value: Boolean) = form.update { it.copy(lastDay = value) }

    fun onDate(value: LocalDate) = form.update { it.copy(date = value) }

    fun onTime(value: LocalTime?) = form.update { it.copy(time = value) }

    fun save(onSaved: () -> Unit) {
        val current = form.value
        if (!isTitleValid(current.title)) {
            form.update { it.copy(showErrors = true) }
            return
        }
        viewModelScope.launch {
            val task = buildTask(current, repository.currentSettings(), clock.instant())
            repository.createTask(task, oneOffDate = current.date.takeIf { current.type == TaskType.ONE_OFF })
            onSaved()
        }
    }

    private fun UrediForm.toUiState() = UrediUiState(
        form = this,
        today = today,
        nextDue = nextDueDate(this, today),
        titleError = showErrors && !isTitleValid(title),
    )

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = checkNotNull(this[APPLICATION_KEY]).container
                UrediViewModel(container.taskRepository, container.clock)
            }
        }
    }
}
