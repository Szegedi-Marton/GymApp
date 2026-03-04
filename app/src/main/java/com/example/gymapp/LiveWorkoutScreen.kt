package com.example.gymapp

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Stable
data class WorkoutState(
    val totalSetsCompleted: Int = 0,
    val exercises: List<ExerciseOption> = defaultExercises,
    val plan: List<PlannedExercise> = emptyList(),
    val selectedPlanExerciseId: String? = null,
    val isSetRunning: Boolean = false,
    val currentSetTimeSeconds: Int = 0,
)

data class ExerciseOption(
    val id: String,
    val name: String,
    val targetSets: Int,
    val targetReps: Int,
    val description: String,
)

data class PlannedExercise(
    val id: String,
    val name: String,
    val targetSets: Int,
    val targetReps: Int,
    val description: String,
    val completedSets: Int = 0,
)

private val defaultExercises = listOf(
    ExerciseOption("bench_press", "Bench Press", 4, 10, "A classic upper-body exercise targeting the chest, shoulders, and triceps."),
    ExerciseOption("lat_pulldown", "Lat Pulldown", 4, 12, "A back exercise that primarily targets the latissimus dorsi muscles."),
    ExerciseOption("barbell_squat", "Barbell Squat", 5, 8, "A fundamental lower-body compound exercise for legs and core."),
    ExerciseOption("romanian_deadlift", "Romanian Deadlift", 3, 10, "Focuses on the hamstrings and glutes with a controlled hinge."),
    ExerciseOption("overhead_press", "Overhead Press", 4, 10, "Presses weight overhead to build strong shoulders and upper arms."),
    ExerciseOption("barbell_row", "Barbell Row", 4, 10, "A pull exercise that builds thickness in the back and improves posture."),
    ExerciseOption("bicep_curl", "Bicep Curl", 3, 15, "Isolation exercise specifically targeting the biceps brachii."),
    ExerciseOption("tricep_pushdown", "Tricep Pushdown", 3, 15, "Targets the triceps using a cable machine for consistent tension."),
    ExerciseOption("leg_press", "Leg Press", 4, 12, "Machine-based leg exercise that focuses on quads and glutes."),
    ExerciseOption("leg_extension", "Leg Extension", 3, 15, "Isolation exercise that targets the quadriceps on a seated machine."),
    ExerciseOption("leg_curl", "Leg Curl", 3, 15, "Isolation exercise focusing on the hamstrings, usually seated or lying."),
    ExerciseOption("calf_raise", "Calf Raise", 4, 20, "Builds the muscles in the lower leg (gastrocnemius and soleus)."),
    ExerciseOption("lateral_raise", "Lateral Raise", 4, 16, "Isolation move for the side delts to create shoulder width."),
    ExerciseOption("front_raise", "Front Raise", 3, 12, "Targets the front deltoids using dumbbells or a barbell."),
    ExerciseOption("shrugs", "Shrugs", 3, 15, "Builds the upper trapezius muscles by shrugging the shoulders."),
    ExerciseOption("face_pull", "Face Pull", 3, 15, "Improves shoulder health and targets the rear delts and upper back."),
    ExerciseOption("hammer_curl", "Hammer Curl", 3, 12, "Bicep variation that also builds the brachialis and forearm."),
    ExerciseOption("incline_bench", "Incline Bench Press", 4, 10, "Focuses on the upper portion of the pectoral muscles."),
    ExerciseOption("decline_bench", "Decline Bench Press", 3, 10, "Focuses on the lower portion of the pectoral muscles."),
    ExerciseOption("dumbbell_fly", "Dumbbell Fly", 3, 12, "Chest isolation exercise that emphasizes the stretch at the bottom."),
    ExerciseOption("pull_up", "Pull Up", 3, 8, "Challenging bodyweight exercise for back width and arm strength."),
    ExerciseOption("chin_up", "Chin Up", 3, 8, "Bodyweight move similar to pull-ups but with more bicep involvement."),
    ExerciseOption("dip", "Dips", 3, 12, "Bodyweight compound move for triceps, chest, and shoulders."),
    ExerciseOption("lunges", "Lunges", 3, 12, "Lower body exercise that improves balance and leg strength."),
)

private enum class GymTab(val title: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Exercises("Exercises", Icons.Default.Search),
    Plan("Plan", Icons.Default.Build),
    CurrentSet("Current Set", Icons.Default.PlayArrow),
}

sealed interface UserIntent {
    data object StartSet : UserIntent
    data object FinishSet : UserIntent
    data class AddExerciseToPlan(val exercise: ExerciseOption) : UserIntent
    data class SelectPlanExercise(val planExerciseId: String) : UserIntent
}

class WorkoutViewModel : ViewModel() {
    private val _state = MutableStateFlow(WorkoutState())
    val state: StateFlow<WorkoutState> = _state.asStateFlow()

    private val reduceMutex = Mutex()
    private var timerJob: Job? = null

    fun onIntent(intent: UserIntent) {
        viewModelScope.launch {
            reduce(intent)
        }
    }

    private suspend fun reduce(intent: UserIntent) {
        reduceMutex.withLock {
            when (intent) {
                UserIntent.StartSet -> {
                    if (!_state.value.isSetRunning && _state.value.selectedPlanExerciseId != null) {
                        _state.update { it.copy(isSetRunning = true, currentSetTimeSeconds = 0) }
                        startTimer()
                    }
                }
                UserIntent.FinishSet -> {
                    if (_state.value.isSetRunning) {
                        stopTimer()
                        _state.update { current ->
                            val selectedExerciseId = current.selectedPlanExerciseId
                            val nextPlan = current.plan.map { exercise ->
                                if (exercise.id == selectedExerciseId) {
                                    exercise.copy(completedSets = exercise.completedSets + 1)
                                } else {
                                    exercise
                                }
                            }
                            current.copy(
                                isSetRunning = false,
                                totalSetsCompleted = current.totalSetsCompleted + 1,
                                plan = nextPlan
                            )
                        }
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
                                targetReps = intent.exercise.targetReps,
                                description = intent.exercise.description,
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

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _state.update { it.copy(currentSetTimeSeconds = it.currentSetTimeSeconds + 1) }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
}

@Composable
fun LiveWorkoutRoute(modifier: Modifier = Modifier) {
    val viewModel: WorkoutViewModel = viewModel(factory = remember { workoutViewModelFactory() })
    val state by viewModel.state.collectAsState()

    GymHomeScreen(
        state = state,
        onStartSet = { viewModel.onIntent(UserIntent.StartSet) },
        onFinishSet = { viewModel.onIntent(UserIntent.FinishSet) },
        onAddExerciseToPlan = { viewModel.onIntent(UserIntent.AddExerciseToPlan(it)) },
        onSelectPlanExercise = { viewModel.onIntent(UserIntent.SelectPlanExercise(it)) },
        modifier = modifier,
    )
}

@Composable
fun GymHomeScreen(
    state: WorkoutState,
    onStartSet: () -> Unit,
    onFinishSet: () -> Unit,
    onAddExerciseToPlan: (ExerciseOption) -> Unit,
    onSelectPlanExercise: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(GymTab.Home) }
    var isSidebarExpanded by remember { mutableStateOf(false) }
    // Sidebar width: 64dp collapsed / 180dp expanded
    val sidebarWidth by animateDpAsState(if (isSidebarExpanded) 180.dp else 64.dp, label = "sidebarWidth")

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF05070D),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar: all the way to left, takes all bottom, has top padding
            Column(
                modifier = Modifier
                    .width(sidebarWidth)
                    .fillMaxHeight()
                    .padding(top = 24.dp)
                    .clip(RoundedCornerShape(topEnd = 24.dp))
                    .background(Color(0xFF0C1324)),
                horizontalAlignment = Alignment.Start
            ) {
                // Fixed Expand Button aligned with icons (using same 12dp horizontal padding)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    IconButton(
                        onClick = { isSidebarExpanded = !isSidebarExpanded },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Toggle Sidebar", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                GymTab.entries.forEach { tab ->
                    SidebarItem(
                        tab = tab,
                        isSelected = selectedTab == tab,
                        isExpanded = isSidebarExpanded,
                        onClick = { selectedTab = tab }
                    )
                }
            }

            // Main Content Area - Responsive layout
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                when (selectedTab) {
                    GymTab.Home -> HomeGreetingScreen(isCompact = isSidebarExpanded)
                    GymTab.Exercises -> ExercisesTab(
                        state = state,
                        isCompact = isSidebarExpanded,
                        onAddExerciseToPlan = onAddExerciseToPlan,
                        onOpenPlan = { selectedTab = GymTab.Plan },
                    )
                    GymTab.Plan -> PlanTab(
                        plan = state.plan,
                        selectedPlanExerciseId = state.selectedPlanExerciseId,
                        isCompact = isSidebarExpanded,
                        onSelectPlanExercise = onSelectPlanExercise,
                        onOpenCurrentSet = { selectedTab = GymTab.CurrentSet },
                    )
                    GymTab.CurrentSet -> CurrentSetScreen(
                        state = state,
                        isCompact = isSidebarExpanded,
                        onStartSet = onStartSet,
                        onFinishSet = onFinishSet
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarItem(
    tab: GymTab,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 12.dp) // Consistent 12dp padding to align icons
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(40.dp) // Same size as IconButton container
                .clip(CircleShape)
                .background(if (isSelected) Color(0xFF89F7FE).copy(alpha = 0.2f) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                tab.icon,
                contentDescription = tab.title,
                tint = if (isSelected) Color(0xFF89F7FE) else Color(0xFF6F7D95)
            )
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = tab.title,
                color = if (isSelected) Color.White else Color(0xFF6F7D95),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeGreetingScreen(isCompact: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isCompact) 16.dp else 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Welcome to GymApp 👋",
            style = if (isCompact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Build your plan from Exercises, then pick an exercise in Plan to make it your current set.",
            style = if (isCompact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            color = Color(0xFFB8C6DE),
        )
    }
}

@Composable
private fun ExercisesTab(
    state: WorkoutState,
    isCompact: Boolean,
    onAddExerciseToPlan: (ExerciseOption) -> Unit,
    onOpenPlan: () -> Unit,
) {
    var selectedExerciseInfo by remember { mutableStateOf<ExerciseOption?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isCompact) 12.dp else 24.dp),
    ) {
        Text(
            text = "Exercises",
            style = if (isCompact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        if (!isCompact) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Tap an exercise to see description.", color = Color(0xFFB8C6DE))
        }
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(state.exercises, key = { it.id }) { exercise ->
                val inPlan = state.plan.any { it.id == exercise.id }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101728)),
                    modifier = Modifier.clickable { selectedExerciseInfo = exercise }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(if (isCompact) 10.dp else 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = exercise.name,
                                color = Color.White,
                                style = if (isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!isCompact) {
                                Text("${exercise.targetSets} target sets", color = Color(0xFF93A1B8))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onAddExerciseToPlan(exercise) },
                            enabled = !inPlan,
                            contentPadding = if (isCompact) ButtonDefaults.TextButtonContentPadding else ButtonDefaults.ContentPadding,
                            modifier = if (isCompact) Modifier.height(32.dp) else Modifier
                        ) {
                            Text(
                                if (inPlan) "Added" else if (isCompact) "+" else "Add to Plan",
                                style = if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onOpenPlan,
            enabled = state.plan.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isCompact) "Plan (${state.plan.size})" else "Go to Plan (${state.plan.size})")
        }
    }

    selectedExerciseInfo?.let { exercise ->
        ExerciseInfoDialog(
            name = exercise.name,
            description = exercise.description,
            onDismiss = { selectedExerciseInfo = null }
        )
    }
}

@Composable
private fun ExerciseInfoDialog(name: String, description: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF101728),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFB8C6DE),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun PlanTab(
    plan: List<PlannedExercise>,
    selectedPlanExerciseId: String?,
    isCompact: Boolean,
    onSelectPlanExercise: (String) -> Unit,
    onOpenCurrentSet: () -> Unit,
) {
    var selectedExerciseInfo by remember { mutableStateOf<PlannedExercise?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isCompact) 12.dp else 24.dp),
    ) {
        Text(
            text = "Workout Plan",
            style = if (isCompact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        if (!isCompact) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Tap an exercise to select it. Tap (i) for info.", color = Color(0xFFB8C6DE))
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (plan.isEmpty()) {
            Text(
                "No exercises in plan.",
                color = Color(0xFF93A1B8),
                style = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                items(plan, key = { it.id }) { exercise ->
                    val isSelected = selectedPlanExerciseId == exercise.id
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF1A2D55) else Color(0xFF101728),
                        ),
                        modifier = Modifier.clickable { onSelectPlanExercise(exercise.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(if (isCompact) 10.dp else 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exercise.name,
                                    color = Color.White,
                                    style = if (isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "Sets: ${exercise.completedSets}/${exercise.targetSets}",
                                    color = Color(0xFF93A1B8),
                                    style = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
                                )
                            }
                            IconButton(
                                onClick = { selectedExerciseInfo = exercise },
                                modifier = Modifier.size(if (isCompact) 32.dp else 40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = Color(0xFF89F7FE),
                                    modifier = Modifier.size(if (isCompact) 18.dp else 24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onOpenCurrentSet,
            enabled = selectedPlanExerciseId != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isCompact) "Start" else "Start Current Set")
        }
    }

    selectedExerciseInfo?.let { exercise ->
        ExerciseInfoDialog(
            name = exercise.name,
            description = exercise.description,
            onDismiss = { selectedExerciseInfo = null }
        )
    }
}

@Composable
private fun CurrentSetScreen(
    state: WorkoutState,
    isCompact: Boolean,
    onStartSet: () -> Unit,
    onFinishSet: () -> Unit,
) {
    val selectedExercise = state.plan.firstOrNull { it.id == state.selectedPlanExerciseId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isCompact) 16.dp else 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "CURRENT SET",
            style = if (isCompact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            color = Color(0xFF89F7FE),
        )

        Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 12.dp))

        Text(
            text = if (selectedExercise != null) "${selectedExercise.name} x${selectedExercise.targetReps}" else "Pick an exercise in Plan",
            style = if (isCompact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(if (isCompact) 24.dp else 48.dp))

        if (isCompact) {
            // Stacked metrics for compact view
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                WorkoutMetric(title = "TIMER", value = formatTime(state.currentSetTimeSeconds), isCompact = true)
                Spacer(modifier = Modifier.height(12.dp))
                WorkoutMetric(title = "TOTAL SETS", value = state.totalSetsCompleted.toString(), isCompact = true)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                WorkoutMetric(title = "SET TIMER", value = formatTime(state.currentSetTimeSeconds))
                WorkoutMetric(title = "SESSION SETS", value = state.totalSetsCompleted.toString())
            }
        }

        if (selectedExercise != null) {
            Spacer(modifier = Modifier.height(if (isCompact) 16.dp else 24.dp))
            Text(
                text = "Progress: ${selectedExercise.completedSets}/${selectedExercise.targetSets} sets",
                color = Color(0xFF93A1B8),
                style = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(if (isCompact) 24.dp else 48.dp))

        Button(
            onClick = if (!state.isSetRunning) onStartSet else onFinishSet,
            enabled = selectedExercise != null,
            modifier = Modifier
                .height(if (isCompact) 48.dp else 56.dp)
                .fillMaxWidth(if (isCompact) 0.8f else 0.6f),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (!state.isSetRunning) Color(0xFF00C853) else Color(0xFFD50000)
            )
        ) {
            Text(
                text = if (!state.isSetRunning) "Start Set" else "Finish Set",
                style = if (isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun WorkoutMetric(title: String, value: String, isCompact: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF101728))
            .padding(
                horizontal = if (isCompact) 16.dp else 20.dp,
                vertical = if (isCompact) 8.dp else 14.dp
            ),
    ) {
        Text(
            text = title,
            color = Color(0xFF6F7D95),
            style = if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 6.dp))
        Text(
            text = value,
            color = Color.White,
            style = if (isCompact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge
        )
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
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
