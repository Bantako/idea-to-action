package org.mrlem.composesample.inbox

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inbox_entry")
data class InboxEntryEntity(
    @PrimaryKey val id: String,
    val text: String,
    val createdAt: Long,
    val status: String = "active",  // "active" | "archived"
    val themeId: String? = null,
)
