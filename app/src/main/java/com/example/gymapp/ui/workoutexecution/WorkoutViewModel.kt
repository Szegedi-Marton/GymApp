package com.example.gymapp.ui.workoutexecution

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymapp.domain.timer.RestTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val restTimer: RestTimer,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkoutExecutionContract.State())
    val state: StateFlow<WorkoutExecutionContract.State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            restTimer.remainingMillis.collectLatest { millis ->
                _state.update {
                    it.copy(
                        restMillisRemaining = millis,
                        restTimerRunning = millis > 0,
                    )
                }
            }
        }
    }

    fun onIntent(intent: WorkoutExecutionContract.Intent) {
        when (intent) {
            is WorkoutExecutionContract.Intent.LogSet -> handleLogSet(intent.reps)
            WorkoutExecutionContract.Intent.SkipExercise -> handleSkipExercise()
            is WorkoutExecutionContract.Intent.StartRest -> restTimer.start(intent.durationMillis)
            WorkoutExecutionContract.Intent.StopRest -> restTimer.stop()
        }
    }

    private fun handleLogSet(reps: Int) {
        _state.update {
            it.copy(
                currentSet = it.currentSet + 1,
                repCount = it.repCount + reps,
            )
        }
    }

    private fun handleSkipExercise() {
        restTimer.stop()
        _state.update {
            it.copy(
                activeExerciseIndex = it.activeExerciseIndex + 1,
                currentSet = 1,
                restTimerRunning = false,
                restMillisRemaining = 0,
            )
        }
    }
}
