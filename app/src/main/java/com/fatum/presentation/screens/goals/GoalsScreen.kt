package com.fatum.presentation.screens.goals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatum.data.db.entities.GoalEntity
import com.fatum.data.db.entities.TaskEntity
import com.fatum.presentation.components.*
import com.fatum.presentation.theme.FatumColors
import com.fatum.presentation.viewmodels.GoalsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(vm: GoalsViewModel = hiltViewModel()) {
    val goals         by vm.goals.collectAsStateWithLifecycle()
    val orphanedTasks by vm.orphanedTasks.collectAsStateWithLifecycle()
    var showAddGoal   by remember { mutableStateOf(false) }
    var showAddTask   by remember { mutableStateOf<Int?>(null) }  // goalId or -1 for orphan

    Scaffold(
        containerColor = FatumColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Metas y Tareas") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FatumColors.Background),
                actions = {
                    IconButton(onClick = { showAddGoal = true }) {
                        Icon(Icons.Default.Add, "Nueva meta", tint = FatumColors.Accent)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Goals (RF-4.1) ──────────────────────────────────────────
            items(goals, key = { it.id }) { goal ->
                GoalCard(goal = goal, vm = vm, onAddTask = { showAddTask = goal.id })
            }
            // ── Orphaned tasks (RF-4.3) ─────────────────────────────────
            item { SectionHeader("Tareas sueltas") }
            items(orphanedTasks, key = { it.id }) { task ->
                TaskRow(task = task, onToggle = { vm.toggleTask(task) }, onDelete = { vm.deleteTask(task) })
            }
            item {
                TextButton(onClick = { showAddTask = -1 }) {
                    Icon(Icons.Default.Add, null, tint = FatumColors.Accent)
                    Spacer(Modifier.width(4.dp))
                    Text("Añadir tarea suelta", color = FatumColors.Accent)
                }
            }
        }
    }

    if (showAddGoal) {
        AddGoalDialog(
            onDismiss = { showAddGoal = false },
            onConfirm = { title, desc, p, dl -> vm.addGoal(title, desc, p, dl); showAddGoal = false }
        )
    }
    showAddTask?.let { goalId ->
        AddTaskDialog(
            goalId = if (goalId == -1) null else goalId,
            onDismiss = { showAddTask = null },
            onConfirm = { title, gId, p, due -> vm.addTask(title, gId, p, due); showAddTask = null }
        )
    }
}

@Composable
private fun GoalCard(goal: GoalEntity, vm: GoalsViewModel, onAddTask: () -> Unit) {
    val tasks by vm.observeTasksForGoal(goal.id).collectAsStateWithLifecycle(emptyList())
    var expanded by remember { mutableStateOf(true) }

    FatumCard(modifier = Modifier.fillMaxWidth()) {
        // Header row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(goal.title, style = MaterialTheme.typography.titleMedium)
                PriorityStars(value = goal.priorityStars)
            }
            Text("${goal.progressPercentage.toInt()}%", style = MaterialTheme.typography.titleMedium, color = FatumColors.AccentSecondary)
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, tint = FatumColors.PrimaryVariant
                )
            }
            IconButton(onClick = { vm.deleteGoal(goal) }) {
                Icon(Icons.Default.Delete, null, tint = FatumColors.Error)
            }
        }
        // Progress bar (RF-4.2)
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { goal.progressPercentage / 100f },
            modifier = Modifier.fillMaxWidth(),
            color = FatumColors.AccentSecondary,
            trackColor = FatumColors.SurfaceVariant
        )
        // Task list
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                tasks.forEach { task ->
                    TaskRow(task = task, onToggle = { vm.toggleTask(task) }, onDelete = { vm.deleteTask(task) })
                    Spacer(Modifier.height(4.dp))
                }
                TextButton(onClick = onAddTask) {
                    Icon(Icons.Default.Add, null, tint = FatumColors.Accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Añadir tarea", color = FatumColors.Accent, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: TaskEntity, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = FatumColors.AccentSecondary,
                uncheckedColor = FatumColors.PrimaryVariant
            )
        )
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (task.isCompleted) FatumColors.PrimaryVariant else FatumColors.Primary,
            modifier = Modifier.weight(1f)
        )
        PriorityStars(value = task.priorityStars)
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, null, tint = FatumColors.Error, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AddGoalDialog(onDismiss: () -> Unit, onConfirm: (String, String?, Int, Long?) -> Unit) {
    var title    by remember { mutableStateOf("") }
    var desc     by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva meta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descripción (opcional)") }, modifier = Modifier.fillMaxWidth())
                Text("Prioridad", style = MaterialTheme.typography.labelSmall)
                PriorityStars(value = priority, onValueChange = { priority = it })
            }
        },
        confirmButton = { TextButton(onClick = { if (title.isNotBlank()) onConfirm(title, desc.ifBlank { null }, priority, null) }) { Text("Crear") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun AddTaskDialog(goalId: Int?, onDismiss: () -> Unit, onConfirm: (String, Int?, Int, Long?) -> Unit) {
    var title    by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva tarea") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                Text("Prioridad", style = MaterialTheme.typography.labelSmall)
                PriorityStars(value = priority, onValueChange = { priority = it })
            }
        },
        confirmButton = { TextButton(onClick = { if (title.isNotBlank()) onConfirm(title, goalId, priority, null) }) { Text("Crear") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
