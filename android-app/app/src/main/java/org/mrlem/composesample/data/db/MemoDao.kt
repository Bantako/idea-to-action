package org.mrlem.composesample.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {

    @Query("SELECT * FROM memos WHERE projectId IS NULL ORDER BY createdAt DESC")
    fun observeUnorganized(): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeByProject(projectId: Long): Flow<List<MemoEntity>>

    @Insert
    suspend fun insert(memo: MemoEntity): Long

    @Update
    suspend fun update(memo: MemoEntity)

    @Delete
    suspend fun delete(memo: MemoEntity)
}
