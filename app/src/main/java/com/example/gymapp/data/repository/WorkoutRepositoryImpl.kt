package com.example.gymapp.data.repository

import com.example.gymapp.data.local.UserSession
import com.example.gymapp.data.local.WorkoutDao
import com.example.gymapp.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
) : WorkoutRepository {
    override fun observeWorkoutSessions(): Flow<List<UserSession>> = workoutDao.observeSessions()

    override suspend fun upsertWorkoutSession(userSession: UserSession) {
        workoutDao.upsertSession(userSession)
    }
}
