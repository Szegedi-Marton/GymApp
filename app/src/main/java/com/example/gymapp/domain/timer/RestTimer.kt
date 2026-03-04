package com.example.gymapp.domain.timer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class RestTimer @Inject constructor(
) {
    private val timerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _remainingMillis = MutableStateFlow(0L)
    val remainingMillis: StateFlow<Long> = _remainingMillis.asStateFlow()

    private var timerJob: Job? = null

    fun start(durationMillis: Long) {
        timerJob?.cancel()
        val endTime = System.nanoTime() + durationMillis * 1_000_000
        _remainingMillis.value = durationMillis

        timerJob = timerScope.launch {
            while (isActive) {
                val now = System.nanoTime()
                val millisLeft = max(0L, (endTime - now) / 1_000_000)
                _remainingMillis.value = millisLeft
                if (millisLeft == 0L) break
                delay(16)
            }
        }
    }

    fun stop() {
        timerJob?.cancel()
        timerJob = null
        _remainingMillis.value = 0L
    }
}
