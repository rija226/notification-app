package hr.podsjetnik.data

import androidx.room3.withWriteTransaction
import hr.podsjetnik.domain.Occurrence
import hr.podsjetnik.domain.Settings
import hr.podsjetnik.domain.Task
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.domain.TaskWithOccurrences
import hr.podsjetnik.domain.occurrencesToCreate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.LocalDate

/** The only data entry point for UI and engine (CODING_STANDARDS: Data). */
class TaskRepository(
    private val db: AppDatabase,
    private val settingsStore: SettingsStore,
    private val clock: Clock,
) {
    private val taskDao = db.taskDao()
    private val occurrenceDao = db.occurrenceDao()

    val tasks: Flow<List<TaskWithOccurrences>> =
        combine(taskDao.observeAll(), occurrenceDao.observeAll()) { tasks, occurrences ->
            val byTask = occurrences.map { it.toDomain() }.groupBy { it.taskId }
            tasks.map { TaskWithOccurrences(it.toDomain(), byTask[it.id].orEmpty()) }
        }

    val settings: Flow<Settings> = settingsStore.settings

    suspend fun currentSettings(): Settings = settingsStore.current()

    /** Saves a new task with its first occurrence ([oneOffDate] for ONE_OFF, rule date for RECURRING). */
    suspend fun createTask(task: Task, oneOffDate: LocalDate?): Long = db.withWriteTransaction {
        val now = clock.instant()
        val id = taskDao.insert(task.copy(id = 0, createdAt = now, updatedAt = now).toEntity())
        val first = when (task.type) {
            TaskType.ONE_OFF -> listOf(Occurrence(taskId = id, dueDate = checkNotNull(oneOffDate), createdAt = now))
            TaskType.RECURRING -> occurrencesToCreate(task.copy(id = id), emptyList(), LocalDate.now(clock), now)
        }
        occurrenceDao.insertAll(first.map { it.toEntity() })
        id
    }

    /** Creates missing occurrences for every task (PRD FR-3). Runs on each refresh. */
    suspend fun ensureOccurrences() {
        db.withWriteTransaction {
            val now = clock.instant()
            val today = LocalDate.now(clock)
            val byTask = occurrenceDao.getAll().map { it.toDomain() }.groupBy { it.taskId }
            val created = taskDao.getAll().flatMap {
                occurrencesToCreate(it.toDomain(), byTask[it.id].orEmpty(), today, now)
            }
            if (created.isNotEmpty()) occurrenceDao.insertAll(created.map { it.toEntity() })
        }
    }

    /** Replaces all data: debug seed now, backup import in Phase 7. */
    suspend fun replaceAll(tasks: List<Task>, occurrences: List<Occurrence>, settings: Settings) {
        db.withWriteTransaction {
            occurrenceDao.deleteAll()
            taskDao.deleteAll()
            taskDao.insertAll(tasks.map { it.toEntity() })
            occurrenceDao.insertAll(occurrences.map { it.toEntity() })
        }
        settingsStore.update { settings }
        ensureOccurrences()
    }
}
