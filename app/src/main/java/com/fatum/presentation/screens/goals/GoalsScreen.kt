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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatum.data.db.entities.GoalEntity
import com.fatum.data.db.entities.MilestoneEntity
import com.fatum.presentation.components.*
import com.fatum.presentation.theme.FatumColors
import com.fatum.presentation.viewmodels.GoalsViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(vm: GoalsViewModel = hiltViewModel()) {
    val allGoals by vm.goals.collectAsStateWithLifecycle()
    val activeGoals = allGoals.filter { !it.isCompleted }
    val completedGoals = allGoals.filter { it.isCompleted }

    var showAdd  by remember { mutableStateOf(false) }
    var editGoal by remember { mutableStateOf<GoalEntity?>(null) }

    Scaffold(
        containerColor = FatumColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Metas", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FatumColors.Background)
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (activeGoals.isEmpty() && completedGoals.isEmpty()) {
                item { EmptyState("🎯", "Sin metas", "Crea tu primera meta con el botón +") }
            } else {
                items(activeGoals, key = { it.id }) { goal ->
                    GoalCard(goal, vm, onEdit = { editGoal = goal })
                }
            }

            if (completedGoals.isNotEmpty()) {
                item { SectionHeader("Completadas", modifier = Modifier.padding(top = 16.dp)) }
                items(completedGoals, key = { "comp_${it.id}" }) { goal ->
                    GoalCard(goal, vm, onEdit = { editGoal = goal })
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAdd) GoalDialog(null, { showAdd = false }) { title, desc, p, dl, type, target ->
        vm.addGoal(title, desc, p, dl, type, target); showAdd = false
    }
    editGoal?.let { g ->
        GoalDialog(g, { editGoal = null }) { title, desc, p, dl, type, target ->
            vm.updateGoal(g.copy(title = title, description = desc, priority = p,
                deadlineTimestamp = dl, goalType = type, targetValue = target))
            editGoal = null
        }
    }
}

@Composable
private fun GoalCard(goal: GoalEntity, vm: GoalsViewModel, onEdit: () -> Unit) {
    val milestones by vm.observeMilestones(goal.id).collectAsStateWithLifecycle(emptyList())
    var expanded   by remember { mutableStateOf(true) }
    var newMilestone by remember { mutableStateOf("") }
    var showMilestoneInput by remember { mutableStateOf(false) }
    var adjustInput by remember { mutableStateOf("") }
    var adjustSign  by remember { mutableStateOf(1) }

    val progress = when (goal.goalType) {
        "VALUE"     -> if (goal.targetValue > 0) (goal.currentValue / goal.targetValue).coerceIn(0f,1f) else 0f
        "MILESTONE" -> if (milestones.isEmpty()) 0f else milestones.count { it.isCompleted }.toFloat() / milestones.size
        else        -> 0f
    }

    FatumCard(modifier = Modifier.fillMaxWidth(), highlight = goal.isCompleted) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (goal.isCompleted) {
                Icon(Icons.Default.CheckCircle, null, tint = FatumColors.Green, modifier = Modifier.size(16.dp))
            } else {
                PriorityDot(goal.priority)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(goal.title, style = MaterialTheme.typography.titleLarge,
                    color = if (goal.isCompleted) FatumColors.TextMuted else FatumColors.TextPrimary,
                    textDecoration = if (goal.isCompleted) TextDecoration.LineThrough else null)
                if (!goal.description.isNullOrBlank())
                    Text(goal.description, style = MaterialTheme.typography.bodySmall, color = FatumColors.TextMuted, maxLines = 2)
                if (goal.deadlineTimestamp != null)
                    Text("📅 ${formatDate(goal.deadlineTimestamp)}", style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted)
            }
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, color = FatumColors.Green, fontWeight = FontWeight.Bold)
            IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = FatumColors.TextMuted)
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Edit, null, tint = FatumColors.TextMuted, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = { vm.deleteGoal(goal) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Delete, null, tint = FatumColors.Error, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
        FatumProgressBar(progress)

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                if (goal.goalType == "MILESTONE") {
                    milestones.forEach { m -> MilestoneRow(m, vm) }
                    AnimatedVisibility(showMilestoneInput) {
                        OutlinedTextField(
                            value = newMilestone, onValueChange = { newMilestone = it },
                            label = { Text("Nuevo hito") }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            colors = fatumOutlinedFieldColors(), singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (newMilestone.isNotBlank()) {
                                        vm.addMilestone(goal.id, newMilestone.trim())
                                        newMilestone = ""; showMilestoneInput = false
                                    }
                                }) { Icon(Icons.Default.Check, null, tint = FatumColors.Green) }
                            }
                        )
                    }
                    TextButton(onClick = { showMilestoneInput = !showMilestoneInput }) {
                        Icon(Icons.Default.Add, null, tint = FatumColors.Green, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Añadir hito", color = FatumColors.Green, style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${goal.currentValue.toLong()} / ${goal.targetValue.toLong()}",
                            style = MaterialTheme.typography.titleMedium, color = FatumColors.TextPrimary)
                        Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { adjustSign *= -1 },
                            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                .background(if (adjustSign < 0) FatumColors.Error.copy(.15f) else FatumColors.GreenSurface)
                                .size(40.dp)) {
                            Icon(if (adjustSign < 0) Icons.Default.Remove else Icons.Default.Add, null,
                                tint = if (adjustSign < 0) FatumColors.Error else FatumColors.Green)
                        }
                        OutlinedTextField(
                            value = adjustInput, onValueChange = { adjustInput = it },
                            label = { Text("Cantidad") }, modifier = Modifier.weight(1f),
                            colors = fatumOutlinedFieldColors(), singleLine = true
                        )
                        FatumButton("OK", modifier = Modifier.height(48.dp), onClick = {
                            val v = adjustInput.toFloatOrNull() ?: return@FatumButton
                            vm.adjustValue(goal, v * adjustSign)
                            adjustInput = ""
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestoneRow(m: MilestoneEntity, vm: GoalsViewModel) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(m.isCompleted, { vm.toggleMilestone(m) },
            colors = CheckboxDefaults.colors(checkedColor = FatumColors.Green, uncheckedColor = FatumColors.Border, checkmarkColor = Color(0xFF0F1117)))
        Text(m.title, modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(textDecoration = if (m.isCompleted) TextDecoration.LineThrough else null),
            color = if (m.isCompleted) FatumColors.TextMuted else FatumColors.TextPrimary)
        IconButton(onClick = { vm.deleteMilestone(m) }, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, null, tint = FatumColors.TextMuted, modifier = Modifier.size(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDialog(
    goal: GoalEntity?, onDismiss: () -> Unit,
    onConfirm: (String, String?, String, Long?, String, Float) -> Unit
) {
    var title    by remember { mutableStateOf(goal?.title ?: "") }
    var desc     by remember { mutableStateOf(goal?.description ?: "") }
    var priority by remember { mutableStateOf(goal?.priority ?: "MEDIUM") }
    var goalType by remember { mutableStateOf(goal?.goalType ?: "MILESTONE") }
    var target   by remember { mutableStateOf((goal?.targetValue ?: 0f).toString()) }
    var deadline by remember { mutableStateOf(goal?.deadlineTimestamp) }
    var showDp   by remember { mutableStateOf(false) }

    if (showDp) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = deadline ?: System.currentTimeMillis())
        DatePickerDialog(onDismissRequest = { showDp = false }, confirmButton = {
            TextButton(onClick = { deadline = dpState.selectedDateMillis; showDp = false }) { Text("Aceptar") }
        }) { DatePicker(dpState) }
    }

    AlertDialog(onDismissRequest = onDismiss, containerColor = FatumColors.Surface, shape = RoundedCornerShape(20.dp),
        title = { Text(if (goal == null) "Nueva meta" else "Editar meta", color = FatumColors.TextPrimary) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { OutlinedTextField(title, { title = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), colors = fatumOutlinedFieldColors(), singleLine = true) }
                item { OutlinedTextField(desc, { desc = it }, label = { Text("Descripción (opcional)") }, modifier = Modifier.fillMaxWidth(), colors = fatumOutlinedFieldColors(), minLines = 2, maxLines = 3) }
                item {
                    Text("Prioridad", style = MaterialTheme.typography.labelLarge, color = FatumColors.TextMuted)
                    PriorityButton(priority, { priority = it })
                }
                item {
                    Text("Fecha límite (opcional)", style = MaterialTheme.typography.labelLarge, color = FatumColors.TextMuted)
                    OutlinedButton(onClick = { showDp = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.CalendarMonth, null, tint = FatumColors.TextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (deadline != null) formatDate(deadline!!) else "Sin fecha límite", color = FatumColors.TextPrimary)
                    }
                }
                item {
                    Text("Tipo de seguimiento", style = MaterialTheme.typography.labelLarge, color = FatumColors.TextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FatumChip("Hitos", selected = goalType == "MILESTONE", onClick = { goalType = "MILESTONE" })
                        FatumChip("Valor numérico", selected = goalType == "VALUE", onClick = { goalType = "VALUE" })
                    }
                }
                if (goalType == "VALUE") {
                    item { OutlinedTextField(target, { target = it }, label = { Text("Valor objetivo") }, modifier = Modifier.fillMaxWidth(), colors = fatumOutlinedFieldColors(), singleLine = true) }
                }
            }
        },
        confirmButton = { FatumButton(if (goal == null) "Crear" else "Guardar", enabled = title.isNotBlank(),
            onClick = { if (title.isNotBlank()) onConfirm(title.trim(), desc.ifBlank { null }, priority, deadline, goalType, target.toFloatOrNull() ?: 0f) }) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = FatumColors.TextSecondary) } }
    )
}

private fun formatDate(ts: Long) = SimpleDateFormat("d MMM yyyy", Locale("es")).format(Date(ts))