package com.example.gymapp.domain.repository

import com.example.gymapp.data.local.UserSession
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun observeWorkoutSessions(): Flow<List<UserSession>>
    suspend fun upsertWorkoutSession(userSession: UserSession)
}
