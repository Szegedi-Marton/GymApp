package com.example.gymapp.data.repository

import app.cash.turbine.test
import com.example.gymapp.data.local.WorkoutDao
import com.example.gymapp.data.local.WorkoutSession
import com.example.gymapp.domain.model.Exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.assertEquals

class WorkoutRepositoryImplTest {

    @Test
    fun `observeWorkoutSessions emits updates when dao data changes`() = runTest {
        val dao = FakeWorkoutDao()
        val repository = WorkoutRepositoryImpl(dao)

        val firstSession = WorkoutSession(
            sessionDuration = 1800,
            totalCalories = 250,
            exercises = listOf(Exercise(name = "Pushups", repetitions = 20)),
        )

        repository.observeWorkoutSessions().test {
            assertEquals(emptyList(), awaitItem())

            repository.upsertWorkoutSession(firstSession)
            val updated = awaitItem()

            assertEquals(1, updated.size)
            assertEquals(1800, updated.first().sessionDuration)
            assertEquals(250, updated.first().totalCalories)

            cancelAndIgnoreRemainingEvents()
        }
    }
}

private class FakeWorkoutDao : WorkoutDao {
    private val sessions = MutableStateFlow<List<WorkoutSession>>(emptyList())

    override suspend fun upsertSession(workoutSession: WorkoutSession) {
        sessions.update { current ->
            current + workoutSession.copy(id = current.size.toLong() + 1)
        }
    }

    override fun observeSessions(): Flow<List<WorkoutSession>> = sessions
}
