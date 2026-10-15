package hr.podsjetnik.ui.spike

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import hr.podsjetnik.R
import hr.podsjetnik.container
import hr.podsjetnik.data.AlarmLogDao
import hr.podsjetnik.data.AlarmLogEntity
import hr.podsjetnik.data.DebugSeed
import hr.podsjetnik.data.TaskRepository
import hr.podsjetnik.domain.Task
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.ui.components.formatTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

// ponytail: temporary debug screen in the Postavke tab until Phase 6 (status, alarm log, test tasks).
class SpikeViewModel(
    private val repository: TaskRepository,
    private val clock: Clock,
    dao: AlarmLogDao,
    private val testTitle: (LocalTime) -> String,
    /** Loads handoff sample data; null in release builds (no asset). */
    private val seed: (suspend () -> Unit)?,
) : ViewModel() {
    val log: StateFlow<List<AlarmLogEntity>> = dao.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val seedAvailable = seed != null

    /** A real one-off task due in [minutes] (rounded up to the minute), reminding hourly once overdue. */
    fun addTest(minutes: Long) {
        viewModelScope.launch {
            val due = LocalDateTime.now(clock).plusMinutes(minutes + 1).truncatedTo(ChronoUnit.MINUTES)
            val now = clock.instant()
            val task = Task(
                title = testTitle(due.toLocalTime()),
                type = TaskType.ONE_OFF,
                recurrence = null,
                dueTime = due.toLocalTime(),
                earlyDays = 0,
                earlyTime = due.toLocalTime(),
                dayTimes = listOf(due.toLocalTime()), // no due-day reminders before the due time
                overdueIntervalHours = 1,
                note = null,
                createdAt = now,
                updatedAt = now,
            )
            repository.createTask(task, due.toLocalDate())
        }
    }

    fun loadSeed() {
        seed?.let { viewModelScope.launch { it() } }
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
                SpikeViewModel(
                    repository = container.taskRepository,
                    clock = container.clock,
                    dao = container.database.alarmLogDao(),
                    testTitle = { app.getString(R.string.spike_test_naslov, formatTime(it)) },
                    seed = seed,
                )
            }
        }
    }
}
