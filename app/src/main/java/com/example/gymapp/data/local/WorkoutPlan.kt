package com.example.gymapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_plans")
data class WorkoutPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val exerciseIds: List<Long>,
)
