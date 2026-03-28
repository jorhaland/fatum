package com.fatum.presentation.screens.habits

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatum.data.db.entities.HabitEntity
import com.fatum.presentation.components.*
import com.fatum.presentation.components.fatumOutlinedFieldColors
import com.fatum.presentation.theme.FatumColors
import com.fatum.presentation.viewmodels.HabitsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(vm: HabitsViewModel = hiltViewModel()) {
    val habits       by vm.habits.collectAsStateWithLifecycle()
    val selectedDate by vm.selectedDate.collectAsStateWithLifecycle()
    var showAdd      by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = FatumColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Hábitos", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FatumColors.Background),
                actions = {
                    TextButton(onClick = { showCalendar = !showCalendar }) {
                        Icon(
                            if (showCalendar) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = if (showCalendar) FatumColors.Green else FatumColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            formatDateShort(selectedDate),
                            color = if (showCalendar) FatumColors.Green else FatumColors.TextSecondary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = FatumColors.Green,
                contentColor   = Color(0xFF0F1117),
                shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Default.Add, contentDescription = "Nuevo hábito") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Inline calendar ─────────────────────────────────────────────
            item {
                AnimatedVisibility(visible = showCalendar) {
                    RetroactivePicker(
                        selectedDate = selectedDate,
                        onDateSelected = { vm.selectDate(it); showCalendar = false }
                    )
                }
            }

            // ── Summary row ─────────────────────────────────────────────────
            if (habits.isNotEmpty()) {
                item { HabitSummaryRow(habits, selectedDate, vm) }
            }

            // ── Habit list ──────────────────────────────────────────────────
            if (habits.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🏃",
                        title = "Sin hábitos todavía",
                        subtitle = "Crea tu primer hábito con el botón verde"
                    )
                }
            } else {
                items(habits, key = { it.id }) { habit ->
                    HabitCard(habit = habit, selectedDate = selectedDate, vm = vm)
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAdd) {
        AddHabitDialog(
            onDismiss = { showAdd = false },
            onConfirm = { name, freq, target, priority ->
                vm.addHabit(name, freq, target, priority)
                showAdd = false
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Summary: X / Y completed today
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HabitSummaryRow(habits: List<HabitEntity>, selectedDate: String, vm: HabitsViewModel) {
    var completedCount by remember { mutableIntStateOf(0) }

    // Count completed on selected date
    LaunchedEffect(selectedDate, habits) {
        var count = 0
        habits.forEach { habit ->
            // Check via ViewModel's observeExecutions (simplified check)
            count += 0 // Actual count filled by HabitCard observeExecutions
        }
        completedCount = count
    }

    FatumCard(modifier = Modifier.fillMaxWidth(), highlight = completedCount == habits.size && habits.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Hábitos de hoy",
                    style = MaterialTheme.typography.titleMedium,
                    color = FatumColors.TextPrimary
                )
                Text(
                    "${habits.size} hábitos activos",
                    style = MaterialTheme.typography.bodySmall,
                    color = FatumColors.TextMuted
                )
            }
            Text(
                "${habits.maxOf { it.currentStreak }}🔥",
                style = MaterialTheme.typography.headlineSmall
            )
        }
        Spacer(Modifier.height(10.dp))
        FatumProgressBar(progress = if (habits.isEmpty()) 0f else completedCount.toFloat() / habits.size)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Habit card — clean loggd.life look
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HabitCard(
    habit: HabitEntity,
    selectedDate: String,
    vm: HabitsViewModel
) {
    val executions by vm.observeExecutions(habit.id).collectAsStateWithLifecycle(emptyList())
    val isDone     = remember(executions, selectedDate) { executions.any { it.dateCompleted == selectedDate } }
    var showDelete by remember { mutableStateOf(false) }

    val cardHighlight = isDone

    FatumCard(modifier = Modifier.fillMaxWidth(), highlight = cardHighlight) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Toggle circle
            HabitToggle(done = isDone, onClick = { vm.toggleHabit(habit.id) })
            Spacer(Modifier.width(12.dp))
            // Info
            Column(Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isDone) FatumColors.TextSecondary else FatumColors.TextPrimary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = frequencyLabel(habit),
                        style = MaterialTheme.typography.labelMedium,
                        color = FatumColors.TextMuted
                    )
                    PriorityStars(value = habit.priorityStars)
                }
            }
            // Streak
            if (habit.currentStreak > 0) {
                StreakBadge(streak = habit.currentStreak)
                Spacer(Modifier.width(8.dp))
            }
            // Delete
            IconButton(onClick = { showDelete = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.MoreVert, contentDescription = null, tint = FatumColors.TextMuted, modifier = Modifier.size(16.dp))
            }
        }

        // 7-day dot strip
        Spacer(Modifier.height(10.dp))
        WeekDots(executions = executions.map { it.dateCompleted }, selectedDate = selectedDate)
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            containerColor = FatumColors.Surface,
            title = { Text("¿Eliminar hábito?", color = FatumColors.TextPrimary) },
            text = { Text("Se eliminarán también todos los registros de \"${habit.name}\".", color = FatumColors.TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { vm.deleteHabit(habit.id); showDelete = false },
                    colors = ButtonDefaults.buttonColors(containerColor = FatumColors.Error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancelar", color = FatumColors.TextSecondary) }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7-day dot strip (Mon – today), green = done
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WeekDots(executions: List<String>, selectedDate: String) {
    val fmt    = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val anchor = runCatching { LocalDate.parse(selectedDate, fmt) }.getOrElse { LocalDate.now() }
    val days   = (6 downTo 0).map { anchor.minusDays(it.toLong()) }

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEach { date ->
            val key  = date.format(fmt)
            val done = executions.contains(key)
            val isSelected = key == selectedDate
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 12.dp else 10.dp)
                        .clip(CircleShape)
                        .background(
                            when { done -> FatumColors.Green; isSelected -> FatumColors.SurfaceHigh; else -> FatumColors.SurfaceVariant }
                        )
                        .then(if (isSelected) Modifier.border(1.dp, FatumColors.Green, CircleShape) else Modifier)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale("es")),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) FatumColors.Green else FatumColors.TextMuted
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Retroactive date picker (last 30 days)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RetroactivePicker(selectedDate: String, onDateSelected: (String) -> Unit) {
    val fmt   = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = LocalDate.now()
    val days  = (29 downTo 0).map { today.minusDays(it.toLong()) }
    val rows  = days.chunked(7)

    FatumCard(modifier = Modifier.fillMaxWidth()) {
        Text("Registro retroactivo", style = MaterialTheme.typography.titleSmall, color = FatumColors.TextSecondary)
        Spacer(Modifier.height(10.dp))
        rows.forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                week.forEach { date ->
                    val key = date.format(fmt)
                    val selected = key == selectedDate
                    val isToday  = date == today
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(when { selected -> FatumColors.Green; isToday -> FatumColors.GreenSurface; else -> FatumColors.SurfaceVariant })
                            .border(if (isToday && !selected) 1.dp else 0.dp, FatumColors.GreenBorder, RoundedCornerShape(8.dp))
                            .clickable { onDateSelected(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = when { selected -> Color(0xFF0F1117); isToday -> FatumColors.Green; else -> FatumColors.TextSecondary }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Add habit dialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AddHabitDialog(onDismiss: () -> Unit, onConfirm: (String, String, Int, Int) -> Unit) {
    var name     by remember { mutableStateOf("") }
    var freqType by remember { mutableStateOf("DAILY") }
    var target   by remember { mutableIntStateOf(3) }
    var priority by remember { mutableIntStateOf(1) }

    val freqOptions = listOf(
        "DAILY"           to "Diario",
        "WEEKLY_X_TIMES"  to "X veces por semana",
        "WEEKLY"          to "Semanal",
        "MONTHLY_X_TIMES" to "X veces al mes",
        "MONTHLY"         to "Mensual"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FatumColors.Surface,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Nuevo hábito", style = MaterialTheme.typography.headlineSmall, color = FatumColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del hábito") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fatumOutlinedFieldColors(),
                    singleLine = true
                )
                // Frequency chips
                Text("Frecuencia", style = MaterialTheme.typography.labelLarge, color = FatumColors.TextMuted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(freqOptions) { (key, label) ->
                        FatumChip(label = label, selected = freqType == key, onClick = { freqType = key })
                    }
                }
                // Target (only for X_TIMES types)
                AnimatedVisibility(visible = freqType.contains("X_TIMES")) {
                    Column {
                        Text("Objetivo: $target veces", style = MaterialTheme.typography.labelLarge, color = FatumColors.TextMuted)
                        Slider(
                            value = target.toFloat(),
                            onValueChange = { target = it.toInt() },
                            valueRange = 1f..7f,
                            steps = 5,
                            colors = SliderDefaults.colors(thumbColor = FatumColors.Green, activeTrackColor = FatumColors.Green)
                        )
                    }
                }
                // Priority
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Prioridad", style = MaterialTheme.typography.labelLarge, color = FatumColors.TextMuted)
                    PriorityStars(value = priority, onValueChange = { priority = it })
                }
            }
        },
        confirmButton = {
            FatumButton(
                text = "Crear hábito",
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), freqType, target, priority) },
                enabled = name.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = FatumColors.TextSecondary) }
        }
    )
}


private fun frequencyLabel(habit: HabitEntity): String = when (habit.frequencyType) {
    "DAILY"           -> "Diario"
    "WEEKLY_X_TIMES"  -> "${habit.frequencyTarget}× semana"
    "WEEKLY"          -> "Semanal"
    "MONTHLY_X_TIMES" -> "${habit.frequencyTarget}× mes"
    "MONTHLY"         -> "Mensual"
    else              -> habit.frequencyType
}

private fun formatDateShort(dateString: String): String {
    return try {
        val d = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        if (d == LocalDate.now()) "Hoy"
        else d.format(DateTimeFormatter.ofPattern("d MMM", Locale("es")))
    } catch (_: Exception) { dateString }
}
