package com.example.gymapp.data.repository

import app.cash.turbine.test
import com.example.gymapp.data.local.UserSession
import com.example.gymapp.data.local.WorkoutDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutRepositoryImplTest {

    @Test
    fun `observeWorkoutSessions emits updates when dao data changes`() = runTest {
        val dao = FakeWorkoutDao()
        val repository = WorkoutRepositoryImpl(dao)

        val firstSession = UserSession(
            workoutPlanId = 1,
            startedAtEpochMs = 1_700_000_000_000,
            completedAtEpochMs = 1_700_000_001_800,
            totalReps = 64,
        )

        repository.observeWorkoutSessions().test {
            assertEquals(emptyList(), awaitItem())

            repository.upsertWorkoutSession(firstSession)
            val updated = awaitItem()

            assertEquals(1, updated.size)
            assertEquals(1, updated.first().workoutPlanId)
            assertEquals(64, updated.first().totalReps)

            cancelAndIgnoreRemainingEvents()
        }
    }
}

private class FakeWorkoutDao : WorkoutDao {
    private val sessions = MutableStateFlow<List<UserSession>>(emptyList())

    override suspend fun upsertSession(userSession: UserSession) {
        sessions.update { current ->
            current + userSession.copy(id = current.size.toLong() + 1)
        }
    }

    override fun observeSessions(): Flow<List<UserSession>> = sessions
}
