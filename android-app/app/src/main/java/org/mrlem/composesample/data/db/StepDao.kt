package org.mrlem.composesample.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {

    @Query("SELECT * FROM steps WHERE projectId = :projectId ORDER BY sortOrder")
    fun observeByProject(projectId: Long): Flow<List<StepEntity>>

    @Query("SELECT * FROM steps WHERE projectId = :projectId AND status = 'PENDING' ORDER BY sortOrder")
    fun observePendingByProject(projectId: Long): Flow<List<StepEntity>>

    @Query("SELECT * FROM steps WHERE projectId IS NULL AND status = 'PENDING' ORDER BY createdAt")
    fun observePendingNoProject(): Flow<List<StepEntity>>

    @Insert
    suspend fun insert(step: StepEntity): Long

    @Update
    suspend fun update(step: StepEntity)

    @Delete
    suspend fun delete(step: StepEntity)
}
