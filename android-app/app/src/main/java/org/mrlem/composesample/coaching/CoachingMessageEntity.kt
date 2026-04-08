package org.mrlem.composesample.coaching

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coaching_message")
data class CoachingMessageEntity(
    @PrimaryKey val id: String,
    val contextType: String,  // "launch" | "inbox_batch" | "morning_suggest" | "review" | "theme_focus"
    val contextId: String,    // inboxEntryId or themeId
    val role: String,         // "user" | "assistant"
    val content: String,
    val createdAt: Long,
)
