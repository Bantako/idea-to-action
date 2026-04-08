package org.mrlem.composesample.theme

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ThemeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(theme: ThemeEntity)

    @Query("SELECT * FROM theme ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ThemeEntity>>

    @Query("SELECT * FROM theme WHERE id = :id")
    fun observeById(id: String): Flow<ThemeEntity?>

    @Query("SELECT * FROM theme WHERE status = 'active' ORDER BY createdAt ASC")
    fun observeActive(): Flow<List<ThemeEntity>>

    @Query("SELECT * FROM theme WHERE status = 'archived' ORDER BY archivedAt DESC")
    fun observeArchived(): Flow<List<ThemeEntity>>

    @Query("UPDATE theme SET status = 'archived', archivedAt = :archivedAt WHERE id = :id")
    suspend fun archive(id: String, archivedAt: Long)

    @Query("UPDATE theme SET goal = :goal WHERE id = :id")
    suspend fun updateGoal(id: String, goal: String)

    @Query("UPDATE theme SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: String, title: String)

    @Query("UPDATE theme SET weight = :weight WHERE id = :id")
    suspend fun updateWeight(id: String, weight: String)

    @Query("SELECT * FROM theme WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ThemeEntity?

    @Query("SELECT * FROM theme WHERE status = 'active' ORDER BY createdAt ASC")
    suspend fun getActive(): List<ThemeEntity>
}
