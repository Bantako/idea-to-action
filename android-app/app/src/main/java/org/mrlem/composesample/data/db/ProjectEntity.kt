package org.mrlem.composesample.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val background: String? = null,
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    val focusedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
