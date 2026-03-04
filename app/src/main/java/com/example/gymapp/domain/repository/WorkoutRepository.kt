package com.example.gymapp.domain.repository

import com.example.gymapp.data.local.WorkoutSession
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun observeWorkoutSessions(): Flow<List<WorkoutSession>>
    suspend fun upsertWorkoutSession(workoutSession: WorkoutSession)
}
