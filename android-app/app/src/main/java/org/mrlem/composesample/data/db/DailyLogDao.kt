package org.mrlem.composesample.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {

    @Query("SELECT * FROM daily_logs WHERE date = :date ORDER BY createdAt DESC")
    fun observeByDate(date: String): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs WHERE stepId IN (SELECT id FROM steps WHERE projectId = :projectId) ORDER BY createdAt DESC")
    fun observeByProject(projectId: Long): Flow<List<DailyLogEntity>>

    @Insert
    suspend fun insert(log: DailyLogEntity): Long

    @Delete
    suspend fun delete(log: DailyLogEntity)
}
