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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
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
    val exercises: List<ExerciseOption> = defaultExercises,
    val plan: List<PlannedExercise> = emptyList(),
    val selectedPlanExerciseId: String? = null,
)

data class ExerciseOption(
    val id: String,
    val name: String,
    val targetSets: Int,
)

data class PlannedExercise(
    val id: String,
    val name: String,
    val targetSets: Int,
    val completedSets: Int = 0,
)

private val defaultExercises = listOf(
    ExerciseOption("bench_press", "Bench Press", 4),
    ExerciseOption("lat_pulldown", "Lat Pulldown", 4),
    ExerciseOption("barbell_squat", "Barbell Squat", 5),
    ExerciseOption("romanian_deadlift", "Romanian Deadlift", 3),
)

private enum class GymTab(val title: String) {
    Home("Home"),
    Exercises("Exercises"),
    Plan("Plan"),
    CurrentSet("Current Set"),
}

sealed interface UserIntent {
    data object IncrementRep : UserIntent
    data class AddExerciseToPlan(val exercise: ExerciseOption) : UserIntent
    data class SelectPlanExercise(val planExerciseId: String) : UserIntent
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

                    _state.update { current ->
                        val selectedExerciseId = current.selectedPlanExerciseId
                        val nextPlan = if (completedSet && selectedExerciseId != null) {
                            current.plan.map { exercise ->
                                if (exercise.id == selectedExerciseId) {
                                    exercise.copy(completedSets = exercise.completedSets + 1)
                                } else {
                                    exercise
                                }
                            }
                        } else {
                            current.plan
                        }

                        current.copy(
                            reps = if (completedSet) 0 else incrementedReps,
                            setCount = if (completedSet) current.setCount + 1 else current.setCount,
                            plan = nextPlan,
                            repTrigger = current.repTrigger + 1,
                        )
                    }
                }

                is UserIntent.AddExerciseToPlan -> {
                    _state.update { current ->
                        if (current.plan.any { it.id == intent.exercise.id }) {
                            current
                        } else {
                            val newPlanExercise = PlannedExercise(
                                id = intent.exercise.id,
                                name = intent.exercise.name,
                                targetSets = intent.exercise.targetSets,
                            )
                            current.copy(
                                plan = current.plan + newPlanExercise,
                                selectedPlanExerciseId = current.selectedPlanExerciseId ?: newPlanExercise.id,
                            )
                        }
                    }
                }

                is UserIntent.SelectPlanExercise -> {
                    _state.update { current ->
                        if (current.plan.any { it.id == intent.planExerciseId }) {
                            current.copy(selectedPlanExerciseId = intent.planExerciseId)
                        } else {
                            current
                        }
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
        onAddExerciseToPlan = { viewModel.onIntent(UserIntent.AddExerciseToPlan(it)) },
        onSelectPlanExercise = { viewModel.onIntent(UserIntent.SelectPlanExercise(it)) },
        modifier = modifier,
    )
}

@Composable
fun GymHomeScreen(
    state: WorkoutState,
    onIncrementRep: () -> Unit,
    onAddExerciseToPlan: (ExerciseOption) -> Unit,
    onSelectPlanExercise: (String) -> Unit,
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
                GymTab.Exercises -> ExercisesTab(
                    state = state,
                    onAddExerciseToPlan = onAddExerciseToPlan,
                    onOpenPlan = { selectedTab = GymTab.Plan },
                )

                GymTab.Plan -> PlanTab(
                    plan = state.plan,
                    selectedPlanExerciseId = state.selectedPlanExerciseId,
                    onSelectPlanExercise = onSelectPlanExercise,
                    onOpenCurrentSet = { selectedTab = GymTab.CurrentSet },
                )

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
            text = "Build your plan from Exercises, then pick an exercise in Plan to make it your current set.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFB8C6DE),
        )
    }
}

@Composable
private fun ExercisesTab(
    state: WorkoutState,
    onAddExerciseToPlan: (ExerciseOption) -> Unit,
    onOpenPlan: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        Text(text = "Exercises", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Create your plan by selecting exercises below.", color = Color(0xFFB8C6DE))
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(state.exercises, key = { it.id }) { exercise ->
                val inPlan = state.plan.any { it.id == exercise.id }
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF101728))) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(exercise.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Text("${exercise.targetSets} target sets", color = Color(0xFF93A1B8))
                        }
                        Button(onClick = { onAddExerciseToPlan(exercise) }, enabled = !inPlan) {
                            Text(if (inPlan) "Added" else "Add to Plan")
                        }
                    }
                }
            }
        }

        OutlinedButton(onClick = onOpenPlan, enabled = state.plan.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text("Go to Plan (${state.plan.size})")
        }
    }
}

@Composable
private fun PlanTab(
    plan: List<PlannedExercise>,
    selectedPlanExerciseId: String?,
    onSelectPlanExercise: (String) -> Unit,
    onOpenCurrentSet: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        Text(text = "Workout Plan", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Select an exercise to make it your current set.", color = Color(0xFFB8C6DE))
        Spacer(modifier = Modifier.height(16.dp))

        if (plan.isEmpty()) {
            Text("No exercises in your plan yet. Add them from the Exercises tab.", color = Color(0xFF93A1B8))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                items(plan, key = { it.id }) { exercise ->
                    val isSelected = selectedPlanExerciseId == exercise.id
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF1A2D55) else Color(0xFF101728),
                        ),
                        onClick = { onSelectPlanExercise(exercise.id) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(exercise.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Sets: ${exercise.completedSets}/${exercise.targetSets}",
                                    color = Color(0xFF93A1B8),
                                )
                            }
                            Text(if (isSelected) "Selected" else "Select", color = Color(0xFF89F7FE))
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onOpenCurrentSet,
            enabled = selectedPlanExerciseId != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Start Current Set")
        }
    }
}

@Composable
private fun CurrentSetScreen(
    state: WorkoutState,
    onIncrementRep: () -> Unit,
) {
    val selectedExercise = state.plan.firstOrNull { it.id == state.selectedPlanExerciseId }

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

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = selectedExercise?.name ?: "Pick an exercise in Plan",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
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

        if (selectedExercise != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Plan progress: ${selectedExercise.completedSets}/${selectedExercise.targetSets} sets",
                color = Color(0xFF93A1B8),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onIncrementRep, enabled = selectedExercise != null) {
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
