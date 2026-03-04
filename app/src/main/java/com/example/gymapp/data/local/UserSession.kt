package com.example.gymapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_sessions")
data class UserSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutPlanId: Long,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long? = null,
    val totalReps: Int,
)
