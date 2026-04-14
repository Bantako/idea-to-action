package org.mrlem.composesample.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ThemeDao {

    @Query("SELECT * FROM themes ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<ThemeEntity>>

    @Query("SELECT * FROM themes WHERE id = :id")
    suspend fun getById(id: Long): ThemeEntity?

    @Insert
    suspend fun insert(theme: ThemeEntity): Long

    @Update
    suspend fun update(theme: ThemeEntity)

    @Delete
    suspend fun delete(theme: ThemeEntity)
}
