package org.mrlem.composesample.inbox

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InboxDao {
    @Query("SELECT * FROM inbox_entry WHERE status = 'active' ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<InboxEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: InboxEntryEntity)

    @Query("SELECT * FROM inbox_entry WHERE id = :id")
    fun observeById(id: String): Flow<InboxEntryEntity?>

    @Query("UPDATE inbox_entry SET status = 'archived' WHERE id = :id")
    suspend fun archive(id: String)

    @Query("UPDATE inbox_entry SET status = 'archived', themeId = :themeId WHERE id = :id")
    suspend fun archiveWithTheme(id: String, themeId: String)

    @Query("SELECT * FROM inbox_entry WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): InboxEntryEntity?
}
