package org.mrlem.composesample.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EdgeDao {

    @Query("SELECT * FROM edges")
    fun observeAll(): Flow<List<EdgeEntity>>

    @Query("SELECT * FROM edges WHERE toId = :nodeId")
    suspend fun getIncomingEdges(nodeId: Long): List<EdgeEntity>

    @Query("SELECT * FROM edges WHERE fromId = :nodeId")
    suspend fun getOutgoingEdges(nodeId: Long): List<EdgeEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(edge: EdgeEntity)

    @Delete
    suspend fun delete(edge: EdgeEntity)
}
