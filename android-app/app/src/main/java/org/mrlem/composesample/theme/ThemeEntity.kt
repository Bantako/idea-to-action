package org.mrlem.composesample.theme

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "theme")
data class ThemeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val goal: String? = null,
    val weight: String = "medium",  // "light" | "medium" | "heavy"
    val status: String = "active",  // "active" | "archived"
    val createdAt: Long,
    val archivedAt: Long? = null,
)
