package com.example.gymapp.data.repository

import com.example.gymapp.data.local.WorkoutDao
import com.example.gymapp.data.local.WorkoutSession
import com.example.gymapp.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
) : WorkoutRepository {
    override fun observeWorkoutSessions(): Flow<List<WorkoutSession>> = workoutDao.observeSessions()

    override suspend fun upsertWorkoutSession(workoutSession: WorkoutSession) {
        workoutDao.upsertSession(workoutSession)
    }
}
