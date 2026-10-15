package hr.podsjetnik.ui.uredi

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
import hr.podsjetnik.domain.Task
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.ui.UrediRoute
import hr.podsjetnik.ui.components.addDayTime
import hr.podsjetnik.ui.components.removeDayTime
import hr.podsjetnik.ui.components.stepEarlyDays
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
) {
    val isEdit: Boolean get() = form.taskId != null
}

class UrediViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: TaskRepository,
    private val clock: Clock,
) : ViewModel() {
    private val taskId = savedStateHandle.toRoute<UrediRoute>().taskId
    private val today = LocalDate.now(clock)
    private var original: Task? = null
    private val form = MutableStateFlow<UrediForm?>(null)

    /** null while loading. */
    val state: StateFlow<UrediUiState?> = form
        .map { it?.toUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            form.value = if (taskId != null) {
                repository.getTask(taskId)?.let {
                    original = it.task
                    formFromTask(it, today)
                }
            } else {
                initialForm(today, repository.currentSettings())
            }
        }
    }

    private fun edit(change: (UrediForm) -> UrediForm) = form.update { it?.let(change) }

    fun onTitle(value: String) = edit { it.copy(title = value) }

    /** Type is fixed after creation (PRD FR-1). */
    fun onType(value: TaskType) = edit { if (it.taskId == null) it.copy(type = value) else it }

    fun onDay(value: Int) = edit { it.copy(day = value, lastDay = false) }

    fun onLastDay(value: Boolean) = edit { it.copy(lastDay = value) }

    fun onDate(value: LocalDate) = edit { it.copy(date = value) }

    fun onTime(value: LocalTime?) = edit { it.copy(time = value) }

    fun onEarlyStep(delta: Int) = edit { it.copy(earlyDays = stepEarlyDays(it.earlyDays, delta)) }

    fun onEarlyTime(value: LocalTime) = edit { it.copy(earlyTime = value) }

    fun onAddDayTime(value: LocalTime) = edit { it.copy(dayTimes = addDayTime(it.dayTimes, value)) }

    fun onRemoveDayTime(value: LocalTime) = edit { it.copy(dayTimes = removeDayTime(it.dayTimes, value)) }

    fun onInterval(hours: Int) = edit { it.copy(overdueIntervalHours = hours) }

    fun onNote(value: String) = edit { it.copy(note = value) }

    fun save(onSaved: () -> Unit) {
        val current = form.value ?: return
        if (!isTitleValid(current.title)) {
            edit { it.copy(showErrors = true) }
            return
        }
        viewModelScope.launch {
            val task = buildTask(current, clock.instant(), original)
            val oneOffDate = current.date.takeIf { task.type == TaskType.ONE_OFF }
            if (original == null) repository.createTask(task, oneOffDate) else repository.updateTask(task, oneOffDate)
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
                UrediViewModel(createSavedStateHandle(), container.taskRepository, container.clock)
            }
        }
    }
}
