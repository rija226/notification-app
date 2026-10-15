package hr.podsjetnik.data

import android.content.Context
import androidx.room3.AutoMigration
import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver

/**
 * v1: alarm_log (Phase 1 spike). v2: task + occurrence (Phase 2).
 * Every schema change needs a migration; destructive migration is never enabled (CLAUDE.md).
 */
@Database(
    entities = [AlarmLogEntity::class, TaskEntity::class, OccurrenceEntity::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@ColumnTypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmLogDao(): AlarmLogDao
    abstract fun taskDao(): TaskDao
    abstract fun occurrenceDao(): OccurrenceDao

    companion object {
        const val NAME = "podsjetnik.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder<AppDatabase>(context, NAME)
                .setDriver(AndroidSQLiteDriver())
                .build()
    }
}
