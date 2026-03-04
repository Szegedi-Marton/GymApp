package com.example.gymapp.ui.workoutexecution

import com.example.gymapp.domain.timer.RestTimer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WorkoutViewModelTest {

    @Test
    fun `LogSet increases set number and rep count`() {
        val viewModel = WorkoutViewModel(RestTimer())

        viewModel.onIntent(WorkoutExecutionContract.Intent.LogSet(reps = 12))

        val state = viewModel.state.value
        assertEquals(2, state.currentSet)
        assertEquals(12, state.repCount)
    }

    @Test
    fun `SkipExercise advances exercise and resets timer related state`() {
        val viewModel = WorkoutViewModel(RestTimer())

        viewModel.onIntent(WorkoutExecutionContract.Intent.SkipExercise)

        val state = viewModel.state.value
        assertEquals(1, state.activeExerciseIndex)
        assertEquals(1, state.currentSet)
        assertFalse(state.restTimerRunning)
        assertEquals(0L, state.restMillisRemaining)
    }
}
