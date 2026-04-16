package org.mrlem.composesample.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val projectId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
