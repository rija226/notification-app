package hr.podsjetnik.data

import androidx.room3.withWriteTransaction
import hr.podsjetnik.domain.Occurrence
import hr.podsjetnik.domain.OccurrenceStatus
import hr.podsjetnik.domain.Settings
import hr.podsjetnik.domain.Task
import hr.podsjetnik.domain.TaskType
import hr.podsjetnik.domain.TaskWithOccurrences
import hr.podsjetnik.domain.applyEdit
import hr.podsjetnik.domain.markDone
import hr.podsjetnik.domain.occurrenceOnResume
import hr.podsjetnik.domain.skip
import hr.podsjetnik.domain.occurrencesToCreate
import hr.podsjetnik.domain.occurrencesToDeleteOnUndo
import hr.podsjetnik.domain.reopen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Instant
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

    suspend fun currentTasks(): List<TaskWithOccurrences> = tasks.first()

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

    suspend fun getTask(taskId: Long): TaskWithOccurrences? {
        val task = taskDao.getById(taskId)?.toDomain() ?: return null
        return TaskWithOccurrences(task, occurrenceDao.getForTask(taskId).map { it.toDomain() })
    }

    /** Saves an edited task and adjusts its occurrences (PRD §8.4, [applyEdit]). */
    suspend fun updateTask(task: Task, oneOffDate: LocalDate?) {
        db.withWriteTransaction {
            val previous = taskDao.getById(task.id)?.toDomain() ?: return@withWriteTransaction
            val now = clock.instant()
            val existing = occurrenceDao.getForTask(task.id).map { it.toDomain() }
            val edit = applyEdit(task, previous.recurrence, existing, oneOffDate, LocalDate.now(clock), now)
            taskDao.update(task.copy(createdAt = previous.createdAt, updatedAt = now).toEntity())
            if (edit.deleteIds.isNotEmpty()) occurrenceDao.deleteByIds(edit.deleteIds)
            occurrenceDao.updateAll(edit.update.map { it.toEntity() })
            occurrenceDao.insertAll(edit.insert.map { it.toEntity() })
        }
    }

    /** Deletes a task with its whole history (PRD AC-1.3, H13). */
    suspend fun deleteTask(taskId: Long) {
        db.withWriteTransaction {
            occurrenceDao.deleteForTask(taskId)
            taskDao.delete(taskId)
        }
    }

    /** Preskoči ovaj mjesec (PRD FR-3); the next occurrence follows in the next refresh. */
    suspend fun skip(occurrenceId: Long) {
        val occurrence = occurrenceDao.getById(occurrenceId)?.toDomain() ?: return
        if (occurrence.status == OccurrenceStatus.OPEN) occurrenceDao.update(occurrence.skip(clock.instant()).toEntity())
    }

    /** Pauziraj (PRD FR-3, H7): open occurrences stay open and silent. */
    suspend fun pause(taskId: Long) {
        val task = taskDao.getById(taskId)?.toDomain() ?: return
        val now = clock.instant()
        taskDao.update(task.copy(pausedAt = now, updatedAt = now).toEntity())
    }

    /** Nastavi: reminders continue; months during the pause are not recorded. */
    suspend fun resume(taskId: Long) {
        db.withWriteTransaction {
            val task = taskDao.getById(taskId)?.toDomain() ?: return@withWriteTransaction
            val now = clock.instant()
            val resumed = task.copy(pausedAt = null, updatedAt = now)
            taskDao.update(resumed.toEntity())
            val existing = occurrenceDao.getForTask(taskId).map { it.toDomain() }
            occurrenceOnResume(resumed, existing, LocalDate.now(clock), now)?.let { occurrenceDao.insertAll(listOf(it.toEntity())) }
        }
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

    /** Task + occurrence for a notification action; null when either no longer exists. */
    suspend fun find(occurrenceId: Long): Pair<Task, Occurrence>? {
        val occurrence = occurrenceDao.getById(occurrenceId)?.toDomain() ?: return null
        val task = taskDao.getById(occurrence.taskId)?.toDomain() ?: return null
        return task to occurrence
    }

    /** Open occurrences of a task, oldest first. */
    suspend fun openOccurrences(taskId: Long): List<Occurrence> =
        occurrenceDao.getForTask(taskId).map { it.toDomain() }
            .filter { it.status == OccurrenceStatus.OPEN }
            .sortedBy { it.dueDate }

    /** Odrađeno (PRD FR-3). Returns the closed occurrence, or null when it was already closed (stale action). */
    suspend fun markDone(occurrenceId: Long): Occurrence? = db.withWriteTransaction {
        val (task, occurrence) = find(occurrenceId) ?: return@withWriteTransaction null
        if (occurrence.status != OccurrenceStatus.OPEN) return@withWriteTransaction null
        val now = clock.instant()
        val done = occurrence.markDone(task, now, clock.zone)
        occurrenceDao.update(done.toEntity())
        if (task.type == TaskType.ONE_OFF) taskDao.update(task.copy(archivedAt = now, updatedAt = now).toEntity())
        // RECURRING: the next occurrence is created by ensureOccurrences() in the following refresh.
        done
    }

    /** Poništi (PRD FR-3): reopen, drop the untouched occurrence the close created, un-archive a one-off. */
    suspend fun undo(occurrenceId: Long) {
        db.withWriteTransaction {
            val (task, occurrence) = find(occurrenceId) ?: return@withWriteTransaction
            if (occurrence.status == OccurrenceStatus.OPEN) return@withWriteTransaction
            val all = occurrenceDao.getForTask(task.id).map { it.toDomain() }
            val created = occurrencesToDeleteOnUndo(occurrence, all)
            if (created.isNotEmpty()) occurrenceDao.deleteByIds(created.map { it.id })
            occurrenceDao.update(occurrence.reopen().toEntity())
            if (task.archivedAt != null) taskDao.update(task.copy(archivedAt = null, updatedAt = clock.instant()).toEntity())
        }
    }

    suspend fun snooze(occurrenceId: Long, until: Instant) = occurrenceDao.setSnoozedUntil(occurrenceId, until)

    /** Persists what the engine posted (PRD §8.1 step 4). */
    suspend fun markNotified(notified: Map<Long, Instant>) {
        if (notified.isEmpty()) return
        db.withWriteTransaction { notified.forEach { (id, at) -> occurrenceDao.setLastNotifiedAt(id, at) } }
    }

    suspend fun setLastSummaryDate(date: LocalDate) = settingsStore.update { it.copy(lastSummaryDate = date) }

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
