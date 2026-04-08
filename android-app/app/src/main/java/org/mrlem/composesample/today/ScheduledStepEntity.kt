package org.mrlem.composesample.today

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_step")
data class ScheduledStepEntity(
    @PrimaryKey val id: String,
    val stepId: String,
    val date: String,              // "yyyy-MM-dd"
    val startTime: Int? = null,    // minutes from midnight
    val durationMinutes: Int? = null,
    val started: Boolean = false,
    val done: Boolean = false,
    val memo: String? = null,
    val actualStartedAt: Long? = null,
    val actualEndedAt: Long? = null,
    val notificationEnabled: Boolean = false,
    val result: String? = null,  // "done" | "started" | "not_done"
)
