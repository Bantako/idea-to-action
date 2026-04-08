package org.mrlem.composesample.step

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(step: StepEntity)

    @Query("SELECT * FROM step WHERE themeId = :themeId AND status != 'archived' ORDER BY createdAt ASC")
    fun observeByTheme(themeId: String): Flow<List<StepEntity>>

    @Query("SELECT * FROM step WHERE id = :id")
    fun observeById(id: String): Flow<StepEntity?>

    @Query("SELECT * FROM step WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): StepEntity?

    @Query("SELECT * FROM step WHERE status = :status ORDER BY createdAt ASC")
    fun observeByStatus(status: String): Flow<List<StepEntity>>

    @Query("UPDATE step SET title = :title, starterAction = :starterAction, weight = :weight WHERE id = :id")
    suspend fun update(id: String, title: String, starterAction: String?, weight: String)

    @Query("UPDATE step SET status = 'archived', archivedAt = :archivedAt WHERE id = :id")
    suspend fun archive(id: String, archivedAt: Long)

    @Query("UPDATE step SET status = 'today' WHERE id = :id")
    suspend fun markToday(id: String)

    @Query("UPDATE step SET status = 'pending' WHERE id = :id")
    suspend fun markPending(id: String)

    @Query("SELECT COUNT(*) FROM step WHERE themeId = :themeId AND status = 'pending'")
    fun observePendingCount(themeId: String): Flow<Int>

    @Query("SELECT * FROM step WHERE themeId = :themeId AND status != 'archived' ORDER BY createdAt ASC")
    suspend fun getActiveByTheme(themeId: String): List<StepEntity>
}
