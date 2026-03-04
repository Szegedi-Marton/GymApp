package com.example.gymapp.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymapp.data.local.UserSession
import com.example.gymapp.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class WorkoutDataViewModel @Inject constructor(
    repository: WorkoutRepository,
) : ViewModel() {
    val workoutSessions: StateFlow<List<UserSession>> = repository.observeWorkoutSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
