package org.mrlem.composesample.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "edges",
    primaryKeys = ["fromId", "toId"],
    foreignKeys = [
        ForeignKey(
            entity = NodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = NodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["toId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("fromId"), Index("toId")],
)
data class EdgeEntity(
    val fromId: Long,
    val toId: Long,
    val type: EdgeType = EdgeType.PREREQUISITE,
)
