package org.mrlem.composesample.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UsageLogDao {

    @Insert
    suspend fun insert(log: UsageLogEntity)

    @Query("SELECT * FROM usage_logs WHERE timestamp >= :fromMs ORDER BY timestamp ASC")
    suspend fun queryFrom(fromMs: Long): List<UsageLogEntity>
}
