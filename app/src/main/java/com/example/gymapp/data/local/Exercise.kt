package com.example.gymapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val instructions: String,
    val setScheme: List<ExerciseSet>,
)

data class ExerciseSet(
    val reps: Int,
    val weightKg: Float,
)
