package org.mrlem.composesample.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val stepId: Long? = null,
    val what: String,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
