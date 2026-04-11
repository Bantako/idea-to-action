package org.mrlem.composesample.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nodes")
data class NodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String = "",
    val status: NodeStatus = NodeStatus.IDEA,
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val doneAt: Long? = null,
)
