package hr.podsjetnik.data

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import hr.podsjetnik.domain.Occurrence
import hr.podsjetnik.domain.RecurrenceRule
import hr.podsjetnik.domain.Settings
import hr.podsjetnik.domain.Task
import hr.podsjetnik.domain.TaskType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class TaskRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val zone = ZoneId.of("Europe/Zagreb")
    private val clock = Clock.fixed(Instant.parse("2026-10-14T08:00:00Z"), zone)
    private lateinit var db: AppDatabase
    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder<AppDatabase>(context).setDriver(AndroidSQLiteDriver()).build()
        // Settings are not touched by these tests; the store is only a constructor dependency.
        repository = TaskRepository(db, SettingsStore(context), clock)
    }

    @After
    fun tearDown() = db.close()

    private fun newTask(type: TaskType, rule: RecurrenceRule? = null, time: LocalTime? = null) = Task(
        title = "Test",
        type = type,
        recurrence = rule,
        dueTime = time,
        earlyDays = Settings().defaultEarlyDays,
        earlyTime = Settings().defaultEarlyTime,
        dayTimes = Settings().defaultDayTimes,
        overdueIntervalHours = 2,
        note = null,
        createdAt = clock.instant(),
        updatedAt = clock.instant(),
    )

    @Test
    fun recurringTaskIsSavedWithItsFirstOccurrence() = runTest {
        repository.createTask(newTask(TaskType.RECURRING, RecurrenceRule.LastDayOfMonth), oneOffDate = null)
        val saved = repository.tasks.first().single()
        assertEquals(RecurrenceRule.LastDayOfMonth, saved.task.recurrence)
        assertEquals(listOf(LocalDate.parse("2026-10-31")), saved.occurrences.map { it.dueDate })
    }

    @Test
    fun oneOffTaskKeepsDateAndTime() = runTest {
        repository.createTask(newTask(TaskType.ONE_OFF, time = LocalTime.of(10, 0)), LocalDate.parse("2026-11-03"))
        val saved = repository.tasks.first().single()
        assertEquals(LocalTime.of(10, 0), saved.task.dueTime)
        assertEquals(listOf(LocalDate.parse("2026-11-03")), saved.occurrences.map { it.dueDate })
    }

    @Test
    fun ensureOccurrencesIsIdempotent() = runTest {
        repository.createTask(newTask(TaskType.RECURRING, RecurrenceRule.DayOfMonth(5)), oneOffDate = null)
        repository.ensureOccurrences()
        repository.ensureOccurrences()
        assertEquals(1, repository.tasks.first().single().occurrences.size)
    }

    @Test
    fun failedImportLeavesExistingDataUntouched() = runTest {
        repository.createTask(newTask(TaskType.RECURRING, RecurrenceRule.LastDayOfMonth), oneOffDate = null)
        val before = repository.tasks.first()
        val task = newTask(TaskType.RECURRING, RecurrenceRule.DayOfMonth(5)).copy(id = 50)
        val clash = Occurrence(taskId = 50, dueDate = LocalDate.parse("2026-11-05"), createdAt = clock.instant())
        // Two occurrences on the same date violate the unique index → the whole transaction rolls back.
        val failed = runCatching { repository.replaceAll(listOf(task), listOf(clash.copy(id = 1), clash.copy(id = 2)), Settings()) }
        assertTrue(failed.isFailure)
        assertEquals(before, repository.tasks.first())
    }

    @Test
    fun dayTimesAndRuleRoundTripThroughConverters() = runTest {
        repository.createTask(newTask(TaskType.RECURRING, RecurrenceRule.DayOfMonth(15)), oneOffDate = null)
        val saved = repository.tasks.first().single().task
        assertEquals(RecurrenceRule.DayOfMonth(15), saved.recurrence)
        assertEquals(Settings().defaultDayTimes, saved.dayTimes)
    }
}
