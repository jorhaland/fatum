package com.fatum.presentation.screens.tasks

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatum.data.db.entities.TaskEntity
import com.fatum.presentation.components.*
import com.fatum.presentation.theme.FatumColors
import com.fatum.presentation.viewmodels.TasksViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(vm: TasksViewModel = hiltViewModel()) {
    val pending   by vm.pending.collectAsStateWithLifecycle()
    val completed by vm.completed.collectAsStateWithLifecycle()
    var showAdd   by remember { mutableStateOf(false) }
    var editTask  by remember { mutableStateOf<TaskEntity?>(null) }

    Scaffold(
        containerColor = FatumColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Tareas", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FatumColors.Background),
                actions = {
                    TextButton(onClick = { vm.showCompleted = !vm.showCompleted }) {
                        Text(if (vm.showCompleted) "Ocultar hechas" else "Ver hechas",
                            style = MaterialTheme.typography.labelLarge, color = FatumColors.TextMuted)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true },
                containerColor = FatumColors.Green, contentColor = Color(0xFF0F1117),
                shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (pending.isEmpty()) {
                item { EmptyState("✅", "Sin tareas pendientes", "Añade una tarea con el botón +") }
            } else {
                items(pending, key = { it.id }) { task ->
                    TaskRow(task, onToggle = { vm.toggle(task) },
                        onEdit   = { editTask = task },
                        onDelete = { vm.delete(task) })
                }
            }
            if (vm.showCompleted && completed.isNotEmpty()) {
                item { SectionHeader("Completadas", modifier = Modifier.padding(top = 8.dp)) }
                items(completed.take(20), key = { "done_${it.id}" }) { task ->
                    TaskRow(task, onToggle = { vm.toggle(task) }, onEdit = null, onDelete = { vm.delete(task) })
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // AQUÍ ESTABA EL ERROR: se pasaba un '_' en lugar de 'dl'
    if (showAdd) {
        AddEditTaskDialog(null, { showAdd = false }) { t, d, p, dl ->
            vm.add(t, d, p, dl)
            showAdd = false
        }
    }

    editTask?.let { task ->
        AddEditTaskDialog(task, { editTask = null }) { t, d, p, dl ->
            vm.update(task.copy(title = t, description = d, priority = p, dueTimestamp = dl))
            editTask = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskRow(task: TaskEntity, onToggle: () -> Unit, onEdit: (() -> Unit)?, onDelete: () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false })
    SwipeToDismissBox(state = state, enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(FatumColors.Error.copy(.12f)),
                contentAlignment = Alignment.CenterEnd) {
                Icon(Icons.Outlined.Delete, null, tint = FatumColors.Error, modifier = Modifier.padding(end = 20.dp))
            }
        }
    ) {
        FatumCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(task.isCompleted, { onToggle() },
                    colors = CheckboxDefaults.colors(checkedColor = FatumColors.Green,
                        uncheckedColor = FatumColors.Border, checkmarkColor = Color(0xFF0F1117)))
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text(task.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null),
                        color = if (task.isCompleted) FatumColors.TextMuted else FatumColors.TextPrimary)
                    if (!task.description.isNullOrBlank())
                        Text(task.description, style = MaterialTheme.typography.bodySmall,
                            color = FatumColors.TextMuted, maxLines = 2)

                    // Ahora mostramos la fecha límite debajo del texto de la tarea
                    if (task.dueTimestamp != null)
                        Text("📅 ${formatDate(task.dueTimestamp)}", style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted)
                }
                PriorityPill(task.priority)
                if (onEdit != null) {
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Edit, null, tint = FatumColors.TextMuted, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PriorityPill(priority: String) {
    val (bg, txt) = when (priority) {
        "HIGH"   -> FatumColors.Error.copy(.15f)   to FatumColors.Error
        "MEDIUM" -> FatumColors.Warning.copy(.15f) to FatumColors.Warning
        else     -> FatumColors.SurfaceVariant      to FatumColors.TextMuted
    }
    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(when(priority) { "HIGH" -> "Alta"; "MEDIUM" -> "Media"; else -> "Baja" },
            style = MaterialTheme.typography.labelSmall, color = txt)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditTaskDialog(task: TaskEntity?, onDismiss: () -> Unit, onConfirm: (String, String?, String, Long?) -> Unit) {
    var title    by remember { mutableStateOf(task?.title ?: "") }
    var desc     by remember { mutableStateOf(task?.description ?: "") }
    var priority by remember { mutableStateOf(task?.priority ?: "MEDIUM") }
    var deadline by remember { mutableStateOf(task?.dueTimestamp) }
    var showDp   by remember { mutableStateOf(false) }

    if (showDp) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = deadline ?: System.currentTimeMillis())
        DatePickerDialog(onDismissRequest = { showDp = false }, confirmButton = {
            TextButton(onClick = { deadline = dpState.selectedDateMillis; showDp = false }) { Text("Aceptar") }
        }) { DatePicker(dpState) }
    }

    AlertDialog(onDismissRequest = onDismiss,
        containerColor = FatumColors.Surface, shape = RoundedCornerShape(20.dp),
        title = { Text(if (task == null) "Nueva tarea" else "Editar tarea", color = FatumColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth(), colors = fatumOutlinedFieldColors(), singleLine = true)
                OutlinedTextField(desc, { desc = it }, label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth(), colors = fatumOutlinedFieldColors(), minLines = 2, maxLines = 4)

                Text("Fecha límite (opcional)", style = MaterialTheme.typography.labelLarge, color = FatumColors.TextMuted)
                OutlinedButton(onClick = { showDp = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.CalendarMonth, null, tint = FatumColors.TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (deadline != null) formatDate(deadline!!) else "Sin fecha límite", color = FatumColors.TextPrimary)
                }

                Text("Prioridad", style = MaterialTheme.typography.labelLarge, color = FatumColors.TextMuted)
                PriorityButton(priority, { priority = it })
            }
        },
        confirmButton = { FatumButton(if (task == null) "Crear" else "Guardar", enabled = title.isNotBlank(),
            onClick = { if (title.isNotBlank()) onConfirm(title.trim(), desc.ifBlank { null }, priority, deadline) }) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = FatumColors.TextSecondary) } }
    )
}

private fun formatDate(ts: Long) = java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale("es")).format(java.util.Date(ts))