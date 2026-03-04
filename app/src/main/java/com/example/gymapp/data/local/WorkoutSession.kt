package com.example.gymapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.gymapp.domain.model.Exercise

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionDuration: Long,
    val totalCalories: Int,
    val exercises: List<Exercise>,
)
