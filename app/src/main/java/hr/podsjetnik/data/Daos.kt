package hr.podsjetnik.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface TaskDao {
    @Query("SELECT * FROM task")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task")
    suspend fun getAll(): List<TaskEntity>

    @Query("SELECT * FROM task WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Insert
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM task")
    suspend fun deleteAll()
}

@Dao
interface OccurrenceDao {
    @Query("SELECT * FROM occurrence")
    fun observeAll(): Flow<List<OccurrenceEntity>>

    @Query("SELECT * FROM occurrence")
    suspend fun getAll(): List<OccurrenceEntity>

    @Query("SELECT * FROM occurrence WHERE id = :id")
    suspend fun getById(id: Long): OccurrenceEntity?

    @Query("SELECT * FROM occurrence WHERE taskId = :taskId")
    suspend fun getForTask(taskId: Long): List<OccurrenceEntity>

    @Insert
    suspend fun insertAll(occurrences: List<OccurrenceEntity>)

    @Update
    suspend fun update(occurrence: OccurrenceEntity)

    @Update
    suspend fun updateAll(occurrences: List<OccurrenceEntity>)

    @Query("UPDATE occurrence SET lastNotifiedAt = :at WHERE id = :id")
    suspend fun setLastNotifiedAt(id: Long, at: Instant)

    @Query("UPDATE occurrence SET snoozedUntil = :until WHERE id = :id")
    suspend fun setSnoozedUntil(id: Long, until: Instant)

    @Query("DELETE FROM occurrence WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM occurrence")
    suspend fun deleteAll()
}

@Dao
interface AlarmLogDao {
    @Insert
    suspend fun insert(entries: List<AlarmLogEntity>)

    @Query("SELECT * FROM alarm_log WHERE firedAt IS NULL ORDER BY scheduledFor LIMIT 1")
    suspend fun nextPending(): AlarmLogEntity?

    @Query("UPDATE alarm_log SET firedAt = :firedAt, postedCount = :postedCount WHERE id = :id")
    suspend fun markFired(id: Long, firedAt: Instant, postedCount: Int)

    @Query("DELETE FROM alarm_log WHERE firedAt IS NULL")
    suspend fun deletePending()

    /** Keeps the newest [keep] rows (PRD §8.6). */
    @Query("DELETE FROM alarm_log WHERE id NOT IN (SELECT id FROM alarm_log ORDER BY scheduledFor DESC LIMIT :keep)")
    suspend fun trim(keep: Int)

    @Query("SELECT * FROM alarm_log ORDER BY scheduledFor DESC LIMIT 200")
    fun observeRecent(): Flow<List<AlarmLogEntity>>
}
