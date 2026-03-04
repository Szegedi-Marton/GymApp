package com.example.gymapp.ui.workoutexecution

object WorkoutExecutionContract {
    data class State(
        val currentSet: Int = 1,
        val restTimerRunning: Boolean = false,
        val activeExerciseIndex: Int = 0,
        val repCount: Int = 0,
        val restMillisRemaining: Long = 0,
    )

    sealed interface Intent {
        data class LogSet(val reps: Int) : Intent
        data object SkipExercise : Intent
        data class StartRest(val durationMillis: Long) : Intent
        data object StopRest : Intent
    }
}
