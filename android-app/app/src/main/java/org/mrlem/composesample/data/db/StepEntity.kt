package org.mrlem.composesample.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "steps")
data class StepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val sortOrder: Int = 0,
    val status: StepStatus = StepStatus.PENDING,
    val doneAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
