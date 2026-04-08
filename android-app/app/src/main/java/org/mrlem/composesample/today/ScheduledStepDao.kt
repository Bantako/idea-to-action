package org.mrlem.composesample.today

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledStepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ScheduledStepEntity)

    @Query("SELECT * FROM scheduled_step WHERE date = :date ORDER BY startTime ASC")
    fun observeByDate(date: String): Flow<List<ScheduledStepEntity>>

    @Query("SELECT * FROM scheduled_step WHERE date = :date ORDER BY startTime ASC")
    suspend fun getByDate(date: String): List<ScheduledStepEntity>

    @Query("SELECT * FROM scheduled_step WHERE stepId = :stepId ORDER BY date DESC")
    fun observeByStep(stepId: String): Flow<List<ScheduledStepEntity>>

    @Query("SELECT * FROM scheduled_step WHERE stepId = :stepId ORDER BY date DESC")
    suspend fun getByStep(stepId: String): List<ScheduledStepEntity>

    @Query("UPDATE scheduled_step SET started = 1, memo = :memo, actualStartedAt = :actualStartedAt WHERE id = :id")
    suspend fun markStarted(id: String, memo: String?, actualStartedAt: Long)

    @Query("UPDATE scheduled_step SET done = 1, memo = :memo, actualEndedAt = :actualEndedAt WHERE id = :id")
    suspend fun markDone(id: String, memo: String?, actualEndedAt: Long)

    @Query("UPDATE scheduled_step SET result = :result WHERE id = :id")
    suspend fun markResult(id: String, result: String)

    @Query("""
        SELECT ss.id, ss.stepId, ss.date, ss.startTime, ss.durationMinutes,
               ss.started, ss.done, ss.memo, ss.actualStartedAt, ss.actualEndedAt,
               ss.notificationEnabled, ss.result
        FROM scheduled_step ss
        INNER JOIN step s ON ss.stepId = s.id
        WHERE s.themeId = :themeId AND ss.result IS NOT NULL
        ORDER BY ss.date DESC
    """)
    fun observeByTheme(themeId: String): Flow<List<ScheduledStepEntity>>
}
