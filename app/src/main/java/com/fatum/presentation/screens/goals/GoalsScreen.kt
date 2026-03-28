package com.fatum.presentation.screens.goals

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatum.data.db.entities.GoalEntity
import com.fatum.data.db.entities.TaskEntity
import com.fatum.presentation.components.*
import com.fatum.presentation.components.fatumOutlinedFieldColors

import com.fatum.presentation.theme.FatumColors
import com.fatum.presentation.viewmodels.GoalsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(vm: GoalsViewModel = hiltViewModel()) {
    val goals         by vm.goals.collectAsStateWithLifecycle()
    val orphanedTasks by vm.orphanedTasks.collectAsStateWithLifecycle()
    var showAddGoal   by remember { mutableStateOf(false) }
    var addTaskGoalId by remember { mutableStateOf<Int?>(null) }
    var showAddOrphan by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = FatumColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Metas", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FatumColors.Background),
                actions = {
                    IconButton(onClick = { showAddGoal = true }) {
                        Icon(Icons.Default.Add, "Nueva meta", tint = FatumColors.Green)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Goals ───────────────────────────────────────────────────────
            if (goals.isEmpty() && orphanedTasks.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🎯",
                        title = "Sin metas todavía",
                        subtitle = "Crea tu primera meta con el botón +\nLas metas contienen tareas que muestran tu progreso"
                    )
                }
            }

            items(goals, key = { it.id }) { goal ->
                GoalCard(
                    goal = goal,
                    vm = vm,
                    onAddTask = { addTaskGoalId = goal.id }
                )
            }

            // ── Orphaned tasks ───────────────────────────────────────────────
            item { SectionHeader("Tareas sueltas") }

            if (orphanedTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sin tareas pendientes ✓", style = MaterialTheme.typography.bodyMedium, color = FatumColors.TextMuted)
                    }
                }
            } else {
                items(orphanedTasks, key = { "orphan_${it.id}" }) { task ->
                    TaskRow(task = task, onToggle = { vm.toggleTask(task) }, onDelete = { vm.deleteTask(task) })
                }
            }

            item {
                TextButton(
                    onClick = { showAddOrphan = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null, tint = FatumColors.Green, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Añadir tarea suelta", color = FatumColors.Green)
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Dialogs
    if (showAddGoal) {
        AddGoalDialog(
            onDismiss = { showAddGoal = false },
            onConfirm = { title, desc, p, dl -> vm.addGoal(title, desc, p, dl); showAddGoal = false }
        )
    }
    addTaskGoalId?.let { gId ->
        AddTaskDialog(
            goalId = gId,
            onDismiss = { addTaskGoalId = null },
            onConfirm = { title, goalId, p, due -> vm.addTask(title, goalId, p, due); addTaskGoalId = null }
        )
    }
    if (showAddOrphan) {
        AddTaskDialog(
            goalId = null,
            onDismiss = { showAddOrphan = false },
            onConfirm = { title, goalId, p, due -> vm.addTask(title, goalId, p, due); showAddOrphan = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Goal card with expandable task list
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GoalCard(goal: GoalEntity, vm: GoalsViewModel, onAddTask: () -> Unit) {
    val tasks    by vm.observeTasksForGoal(goal.id).collectAsStateWithLifecycle(emptyList())
    var expanded by remember { mutableStateOf(true) }
    val progress = goal.progressPercentage / 100f

    FatumCard(modifier = Modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(goal.title, style = MaterialTheme.typography.titleLarge, color = FatumColors.TextPrimary)
                if (!goal.description.isNullOrBlank()) {
                    Text(goal.description, style = MaterialTheme.typography.bodySmall, color = FatumColors.TextMuted, maxLines = 1)
                }
            }
            Text(
                "${goal.progressPercentage.toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = FatumColors.Green
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, tint = FatumColors.TextMuted, modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = { vm.deleteGoal(goal) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Delete, null, tint = FatumColors.Error, modifier = Modifier.size(16.dp))
            }
        }

        // Progress bar
        Spacer(Modifier.height(8.dp))
        FatumProgressBar(progress = progress)
        Spacer(Modifier.height(4.dp))
        Text(
            "${tasks.count { it.isCompleted }} de ${tasks.size} tareas completadas",
            style = MaterialTheme.typography.labelSmall,
            color = FatumColors.TextMuted
        )

        // Task list
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                tasks.forEach { task ->
                    TaskRow(
                        task = task,
                        onToggle = { vm.toggleTask(task) },
                        onDelete = { vm.deleteTask(task) }
                    )
                }
                TextButton(onClick = onAddTask) {
                    Icon(Icons.Default.Add, null, tint = FatumColors.Green, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Añadir tarea", color = FatumColors.Green, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Task row — checkbox + title + priority
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TaskRow(task: TaskEntity, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor   = FatumColors.Green,
                uncheckedColor = FatumColors.Border,
                checkmarkColor = Color(0xFF0F1117)
            )
        )
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
            ),
            color = if (task.isCompleted) FatumColors.TextMuted else FatumColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        PriorityStars(value = task.priorityStars)
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, null, tint = FatumColors.TextMuted, modifier = Modifier.size(14.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Add Goal dialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AddGoalDialog(onDismiss: () -> Unit, onConfirm: (String, String?, Int, Long?) -> Unit) {
    var title    by remember { mutableStateOf("") }
    var desc     by remember { mutableStateOf("") }
    var priority by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FatumColors.Surface,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Nueva meta", color = FatumColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("¿Qué quieres lograr?") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fatumOutlinedFieldColors(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fatumOutlinedFieldColors()
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Prioridad", style = MaterialTheme.typography.labelLarge, color = FatumColors.TextMuted)
                    PriorityStars(value = priority, onValueChange = { priority = it })
                }
            }
        },
        confirmButton = {
            FatumButton("Crear meta", onClick = { if (title.isNotBlank()) onConfirm(title.trim(), desc.ifBlank { null }, priority, null) }, enabled = title.isNotBlank())
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = FatumColors.TextSecondary) } }
    )
}

@Composable
private fun AddTaskDialog(goalId: Int?, onDismiss: () -> Unit, onConfirm: (String, Int?, Int, Long?) -> Unit) {
    var title    by remember { mutableStateOf("") }
    var priority by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FatumColors.Surface,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Nueva tarea", color = FatumColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la tarea") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fatumOutlinedFieldColors(),
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Prioridad", style = MaterialTheme.typography.labelLarge, color = FatumColors.TextMuted)
                    PriorityStars(value = priority, onValueChange = { priority = it })
                }
            }
        },
        confirmButton = {
            FatumButton("Crear", onClick = { if (title.isNotBlank()) onConfirm(title.trim(), goalId, priority, null) }, enabled = title.isNotBlank())
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = FatumColors.TextSecondary) } }
    )
}