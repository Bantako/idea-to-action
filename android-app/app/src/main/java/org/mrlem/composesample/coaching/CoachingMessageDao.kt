package org.mrlem.composesample.coaching

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CoachingMessageDao {
    @Query("SELECT * FROM coaching_message WHERE contextId = :contextId AND contextType = :contextType ORDER BY createdAt ASC")
    suspend fun getByContext(contextId: String, contextType: String): List<CoachingMessageEntity>

    @Insert
    suspend fun insert(message: CoachingMessageEntity)
}
