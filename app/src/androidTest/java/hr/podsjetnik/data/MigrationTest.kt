package hr.podsjetnik.data

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The spike's v1 database (alarm log) must survive the v2 upgrade (PLAN Phase 2). */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = instrumentation,
        file = instrumentation.targetContext.getDatabasePath("migration-test.db"),
        driver = AndroidSQLiteDriver(),
        databaseClass = AppDatabase::class,
    )

    @Test
    fun migrate1To2KeepsAlarmLog() = runTest {
        helper.createDatabase(1).apply {
            execSQL(
                "INSERT INTO alarm_log (id, scheduledFor, firedAt, postedCount, mode) " +
                    "VALUES (1, 1791976929114, 1791976929121, 1, 'EXACT_WHILE_IDLE')",
            )
            close()
        }
        val migrated = helper.runMigrationsAndValidate(2)
        migrated.prepare("SELECT COUNT(*) FROM alarm_log").use { statement ->
            statement.step()
            check(statement.getLong(0) == 1L) { "alarm log row lost in migration" }
        }
        migrated.close()
    }
}
