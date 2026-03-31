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
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(vm: GoalsViewModel = hiltViewModel()) {
    val goals    by vm.goals.collectAsStateWithLifecycle()
    var showAdd  by remember { mutableStateOf(false) }
    var editGoal by remember { mutableStateOf<GoalEntity?>(null) }

    Scaffold(
        containerColor = FatumColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Metas", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FatumColors.Background),
                actions = {
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Default.Add, null, tint = FatumColors.Green)
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
            if (goals.isEmpty()) {
                item { EmptyState("🎯", "Sin metas", "Crea tu primera meta con el botón +") }
            } else {
                items(goals, key = { it.id }) { goal ->
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

// ─────────────────────────────────────────────────────────────────────────────
// Goal card
// ─────────────────────────────────────────────────────────────────────────────
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

    FatumCard(modifier = Modifier.fillMaxWidth()) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PriorityDot(goal.priority)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(goal.title, style = MaterialTheme.typography.titleLarge, color = FatumColors.TextPrimary)
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

        // ── Progress bar ──────────────────────────────────────────────────────
        Spacer(Modifier.height(8.dp))
        FatumProgressBar(progress)

        // ── Expanded content ──────────────────────────────────────────────────
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 12.dp)) {

                if (goal.goalType == "MILESTONE") {
                    // ── Milestones ────────────────────────────────────────────
                    milestones.forEach { m ->
                        MilestoneRow(m, vm)
                    }
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
                    // ── VALUE goal ────────────────────────────────────────────
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${goal.currentValue.toLong()} / ${goal.targetValue.toLong()}",
                             style = MaterialTheme.typography.titleMedium, color = FatumColors.TextPrimary)
                        Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    // Input row: ─ [value] + with sign toggle
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
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = {}, contentPadding = PaddingValues(0.dp)) {
                        Text("Actualizar objetivo", style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestoneRow(m: MilestoneEntity, vm: GoalsViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(m.isCompleted, { vm.toggleMilestone(m) },
            colors = CheckboxDefaults.colors(checkedColor = FatumColors.Green,
                uncheckedColor = FatumColors.Border, checkmarkColor = Color(0xFF0F1117)))
        Text(m.title,
             modifier = Modifier.weight(1f),
             style = MaterialTheme.typography.bodyMedium.copy(
                 textDecoration = if (m.isCompleted) TextDecoration.LineThrough else null),
             color = if (m.isCompleted) FatumColors.TextMuted else FatumColors.TextPrimary)
        IconButton(onClick = { vm.deleteMilestone(m) }, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, null, tint = FatumColors.TextMuted, modifier = Modifier.size(14.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Add/Edit goal dialog
// ─────────────────────────────────────────────────────────────────────────────
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
    var deadline by remember { mutableStateOf("") }

    AlertDialog(onDismissRequest = onDismiss,
        containerColor = FatumColors.Surface, shape = RoundedCornerShape(20.dp),
        title = { Text(if (goal == null) "Nueva meta" else "Editar meta", color = FatumColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(), colors = fatumOutlinedFieldColors(), singleLine = true)
                OutlinedTextField(desc, { desc = it }, label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth(), colors = fatumOutlinedFieldColors(), minLines = 2, maxLines = 3)
                Text("Prioridad", style = MaterialTheme.typography.labelLarge, color = FatumColors.TextMuted)
                PriorityButton(priority, { priority = it })
                OutlinedTextField(deadline, { deadline = it },
                    label = { Text("Fecha límite (opcional, dd/mm/aaaa)") },
                    modifier = Modifier.fillMaxWidth(), colors = fatumOutlinedFieldColors(), singleLine = true)
                Text("Tipo de seguimiento", style = MaterialTheme.typography.labelLarge, color = FatumColors.TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FatumChip("Hitos", selected = goalType == "MILESTONE", onClick = { goalType = "MILESTONE" })
                    FatumChip("Valor numérico", selected = goalType == "VALUE", onClick = { goalType = "VALUE" })
                }
                AnimatedVisibility(goalType == "VALUE") {
                    OutlinedTextField(target, { target = it }, label = { Text("Valor objetivo") },
                        modifier = Modifier.fillMaxWidth(), colors = fatumOutlinedFieldColors(), singleLine = true)
                }
            }
        },
        confirmButton = { FatumButton(if (goal == null) "Crear" else "Guardar", enabled = title.isNotBlank(),
            onClick = {
                if (title.isNotBlank()) {
                    val dl = parseDeadline(deadline)
                    val t  = target.toFloatOrNull() ?: 0f
                    onConfirm(title.trim(), desc.ifBlank { null }, priority, dl, goalType, t)
                }
            }) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = FatumColors.TextSecondary) } }
    )
}

private fun parseDeadline(s: String): Long? = try {
    if (s.isBlank()) null
    else SimpleDateFormat("dd/MM/yyyy", Locale("es")).parse(s)?.time
} catch (_: Exception) { null }

private fun formatDate(ts: Long) =
    SimpleDateFormat("d MMM yyyy", Locale("es")).format(Date(ts))
