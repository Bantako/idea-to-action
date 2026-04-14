package org.mrlem.composesample.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NodeDao {

    @Query("SELECT * FROM nodes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<NodeEntity>>

    @Query("SELECT * FROM nodes WHERE status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: NodeStatus): Flow<List<NodeEntity>>

    @Query("SELECT * FROM nodes WHERE id = :id")
    suspend fun getById(id: Long): NodeEntity?

    @Insert
    suspend fun insert(node: NodeEntity): Long

    @Update
    suspend fun update(node: NodeEntity)

    @Delete
    suspend fun delete(node: NodeEntity)

    @Query("UPDATE nodes SET themeId = :themeId WHERE id = :nodeId")
    suspend fun updateTheme(nodeId: Long, themeId: Long?)

    @Query("UPDATE nodes SET themeId = NULL WHERE themeId = :themeId")
    suspend fun clearTheme(themeId: Long)
}
