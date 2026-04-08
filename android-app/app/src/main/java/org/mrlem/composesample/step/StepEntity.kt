package org.mrlem.composesample.step

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "step")
data class StepEntity(
    @PrimaryKey val id: String,
    val themeId: String,
    val title: String,
    val starterAction: String? = null,
    val weight: String = "medium",  // "light" | "medium" | "heavy"
    val status: String = "pending",  // "pending" | "today" | "archived"
    val createdAt: Long,
    val archivedAt: Long? = null,
)
