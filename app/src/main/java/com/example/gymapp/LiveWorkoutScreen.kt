package com.example.gymapp

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt

@Stable
data class WorkoutState(
    val heartRate: Int = 128,
    val reps: Int = 0,
    val setCount: Int = 1,
    val motionProgress: Float = 0f,
    val repTrigger: Long = 0L,
)

private enum class GymTab(val title: String) {
    Home("Home"),
    Exercises("Exercises"),
    Plan("Plan"),
    CurrentSet("Current Set"),
}

sealed interface UserIntent {
    data object IncrementRep : UserIntent
}

class WorkoutViewModel : ViewModel() {
    private val _state = MutableStateFlow(WorkoutState())
    val state: StateFlow<WorkoutState> = _state.asStateFlow()

    private val reduceMutex = Mutex()

    fun onIntent(intent: UserIntent) {
        viewModelScope.launch {
            reduce(intent)
        }
    }

    private suspend fun reduce(intent: UserIntent) {
        reduceMutex.withLock {
            when (intent) {
                UserIntent.IncrementRep -> {
                    val latest = _state.value
                    val incrementedReps = latest.reps + 1
                    val completedSet = incrementedReps >= REPS_PER_SET

                    _state.update {
                        it.copy(
                            reps = if (completedSet) 0 else incrementedReps,
                            setCount = if (completedSet) it.setCount + 1 else it.setCount,
                            repTrigger = it.repTrigger + 1
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val REPS_PER_SET = 12
    }
}

@Composable
fun LiveWorkoutRoute(modifier: Modifier = Modifier) {
    val viewModel: WorkoutViewModel = viewModel(factory = remember { workoutViewModelFactory() })
    val state by viewModel.state.collectAsState()

    val animatedMotion = remember { Animatable(0f) }

    LaunchedEffect(state.repTrigger) {
        if (state.repTrigger > 0) {
            animatedMotion.snapTo(0f)
            animatedMotion.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
            animatedMotion.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 180, easing = LinearEasing),
            )
        }
    }

    GymHomeScreen(
        state = state.copy(motionProgress = animatedMotion.value),
        onIncrementRep = { viewModel.onIntent(UserIntent.IncrementRep) },
        modifier = modifier,
    )
}

@Composable
fun GymHomeScreen(
    state: WorkoutState,
    onIncrementRep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(GymTab.Home) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF05070D),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 20.dp),
        ) {
            NavigationRail(
                containerColor = Color(0xFF0C1324),
                modifier = Modifier.fillMaxHeight(),
            ) {
                GymTab.entries.forEach { tab ->
                    NavigationRailItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.title.take(1), color = Color.White) },
                        label = { Text(tab.title, textAlign = TextAlign.Center) },
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            when (selectedTab) {
                GymTab.Home -> HomeGreetingScreen()
                GymTab.Exercises -> TabPlaceholder(title = "Exercises", description = "Browse and track your exercises here.")
                GymTab.Plan -> TabPlaceholder(title = "Plan", description = "Build your weekly training plan.")
                GymTab.CurrentSet -> CurrentSetScreen(state = state, onIncrementRep = onIncrementRep)
            }
        }
    }
}

@Composable
private fun HomeGreetingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Welcome to GymApp 👋", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Let's crush today's workout! Use the left sidebar to jump into exercises, your plan, or your current set.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFB8C6DE),
        )
    }
}

@Composable
private fun TabPlaceholder(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = description, style = MaterialTheme.typography.bodyLarge, color = Color(0xFFB8C6DE))
    }
}

@Composable
private fun CurrentSetScreen(
    state: WorkoutState,
    onIncrementRep: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "CURRENT SET",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF89F7FE),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "${state.heartRate} BPM",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.pulseEffect(state.heartRate),
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            WorkoutMetric(title = "REPS", value = state.reps.toString())
            WorkoutMetric(title = "SETS", value = state.setCount.toString())
            WorkoutMetric(title = "MOTION", value = "${(state.motionProgress * 100).roundToInt()}%")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onIncrementRep) {
            Text(text = "Add Rep")
        }
    }
}

@Composable
private fun WorkoutMetric(title: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF101728))
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(text = title, color = Color(0xFF6F7D95), style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = value, color = Color.White, style = MaterialTheme.typography.titleLarge)
    }
}

fun Modifier.pulseEffect(heartRate: Int): Modifier = composed {
    val pulseStrength = ((heartRate - 55) / 110f).coerceIn(0f, 1f)
    val infiniteTransition = rememberInfiniteTransition(label = "heartPulse")
    val wave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (1200 - (pulseStrength * 500f)).roundToInt(),
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heartPulseWave",
    )

    val neonScale = 1f + (0.04f + pulseStrength * 0.08f) * wave
    val glowAlpha = 0.25f + pulseStrength * 0.55f * wave
    val blurRadius = with(LocalDensity.current) { (8.dp + (20.dp * pulseStrength * wave)).toPx() }

    this
        .graphicsLayer {
            scaleX = neonScale
            scaleY = neonScale
            shadowElevation = 20f + (44f * pulseStrength * wave)
            ambientShadowColor = Color(0xFF00F5FF).copy(alpha = glowAlpha)
            spotShadowColor = Color(0xFF9D4DFF).copy(alpha = glowAlpha)
        }
        .blur(radius = with(LocalDensity.current) { blurRadius.toDp() * 0.35f })
}

private fun workoutViewModelFactory(): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return WorkoutViewModel() as T
            }
            error("Unknown ViewModel class: ${modelClass.name}")
        }
    }
