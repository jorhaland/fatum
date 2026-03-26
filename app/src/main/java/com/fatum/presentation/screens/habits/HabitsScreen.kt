package com.fatum.presentation.screens.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatum.data.db.entities.HabitEntity
import com.fatum.presentation.components.*
import com.fatum.presentation.theme.FatumColors
import com.fatum.presentation.viewmodels.HabitsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(vm: HabitsViewModel = hiltViewModel()) {
    val habits      by vm.habits.collectAsStateWithLifecycle()
    val selectedDate by vm.selectedDate.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showCalendar  by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = FatumColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Hábitos") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FatumColors.Background),
                actions = {
                    TextButton(onClick = { showCalendar = !showCalendar }) {
                        Text(selectedDate, color = FatumColors.Accent, style = MaterialTheme.typography.labelSmall)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = FatumColors.Accent
            ) { Icon(Icons.Default.Add, contentDescription = "Añadir hábito", tint = FatumColors.Background) }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Retroactive date picker (RF-2.2) ────────────────────────
            if (showCalendar) {
                item { MiniCalendarPicker(selectedDate = selectedDate, onDateSelected = { vm.selectDate(it) }) }
            }
            // ── Habit rows ─────────────────────────────────────────────
            items(habits, key = { it.id }) { habit ->
                HabitRow(
                    habit = habit,
                    selectedDate = selectedDate,
                    vm = vm,
                    onDelete = { vm.deleteHabit(habit.id) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddHabitDialog(onDismiss = { showAddDialog = false }, onConfirm = { n, t, target, p ->
            vm.addHabit(n, t, target, p)
            showAddDialog = false
        })
    }
}

@Composable
private fun HabitRow(
    habit: HabitEntity,
    selectedDate: String,
    vm: HabitsViewModel,
    onDelete: () -> Unit
) {
    val executions by vm.observeExecutions(habit.id).collectAsStateWithLifecycle(emptyList())
    val isDoneOnDate = executions.any { it.dateCompleted == selectedDate }

    FatumCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Completion toggle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDoneOnDate) FatumColors.AccentSecondary else FatumColors.SurfaceVariant)
                    .clickable { vm.toggleHabit(habit.id) },
                contentAlignment = Alignment.Center
            ) {
                if (isDoneOnDate) Icon(Icons.Default.Check, contentDescription = null, tint = FatumColors.Background, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium, color = FatumColors.Primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    PriorityStars(value = habit.priorityStars)
                    Text(frequencyLabel(habit), style = MaterialTheme.typography.labelSmall, color = FatumColors.PrimaryVariant)
                }
            }
            StreakBadge(streak = habit.currentStreak)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = FatumColors.Error)
            }
        }
        // Week strip: last 7 days dots
        Spacer(Modifier.height(8.dp))
        WeekStrip(executions = executions.map { it.dateCompleted })
    }
}

@Composable
private fun WeekStrip(executions: List<String>) {
    val today = LocalDate.now()
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (6 downTo 0).forEach { offset ->
            val date = today.minusDays(offset.toLong()).format(fmt)
            val done = executions.contains(date)
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (done) FatumColors.AccentSecondary else FatumColors.SurfaceVariant)
            )
        }
    }
}

@Composable
private fun MiniCalendarPicker(selectedDate: String, onDateSelected: (String) -> Unit) {
    val today = LocalDate.now()
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    FatumCard {
        Text("Registro retroactivo", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        // Display last 30 days in a grid
        val days = (29 downTo 0).map { today.minusDays(it.toLong()) }
        val rows = days.chunked(7)
        rows.forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    val key = date.format(fmt)
                    val isSelected = key == selectedDate
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) FatumColors.Accent else FatumColors.SurfaceVariant)
                            .clickable { onDateSelected(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) FatumColors.Background else FatumColors.PrimaryVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun AddHabitDialog(onDismiss: () -> Unit, onConfirm: (String, String, Int, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var freqType by remember { mutableStateOf("DAILY") }
    var target by remember { mutableStateOf(1) }
    var priority by remember { mutableStateOf(1) }

    val freqOptions = listOf("DAILY","WEEKLY_X_TIMES","WEEKLY","MONTHLY_X_TIMES","MONTHLY")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo hábito") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                Text("Frecuencia", style = MaterialTheme.typography.labelSmall, color = FatumColors.PrimaryVariant)
                freqOptions.forEach { opt ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = freqType == opt, onClick = { freqType = opt })
                        Text(opt.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (freqType.contains("X_TIMES")) {
                    OutlinedTextField(value = target.toString(), onValueChange = { target = it.toIntOrNull() ?: 1 }, label = { Text("Objetivo (veces)") })
                }
                Text("Prioridad", style = MaterialTheme.typography.labelSmall, color = FatumColors.PrimaryVariant)
                PriorityStars(value = priority, onValueChange = { priority = it })
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onConfirm(name, freqType, target, priority) }) { Text("Crear") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private fun frequencyLabel(habit: HabitEntity): String = when (habit.frequencyType) {
    "DAILY"           -> "Diario"
    "WEEKLY_X_TIMES"  -> "${habit.frequencyTarget}x semana"
    "WEEKLY"          -> "Semanal"
    "MONTHLY_X_TIMES" -> "${habit.frequencyTarget}x mes"
    "MONTHLY"         -> "Mensual"
    else              -> habit.frequencyType
}
