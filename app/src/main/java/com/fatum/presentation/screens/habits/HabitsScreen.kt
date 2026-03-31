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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatum.data.db.entities.HabitEntity
import com.fatum.data.db.entities.HabitExecutionEntity
import com.fatum.presentation.components.*
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
    var showPicker   by remember { mutableStateOf(false) }
    var expandedId   by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        containerColor = FatumColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Hábitos", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FatumColors.Background),
                actions = {
                    TextButton(onClick = { showPicker = !showPicker }) {
                        Icon(Icons.Outlined.CalendarMonth, null,
                             tint = if (showPicker) FatumColors.Green else FatumColors.TextMuted,
                             modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(formatDateShort(selectedDate),
                             color = if (showPicker) FatumColors.Green else FatumColors.TextMuted,
                             style = MaterialTheme.typography.labelLarge)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true },
                containerColor = FatumColors.Green, contentColor = Color(0xFF0F1117),
                shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Add, "Nuevo hábito")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Date picker ─────────────────────────────────────────────────
            item {
                AnimatedVisibility(showPicker) {
                    RetroactivePicker(selectedDate) { vm.selectDate(it); showPicker = false }
                }
            }
            // ── Habit list ──────────────────────────────────────────────────
            if (habits.isEmpty()) {
                item { EmptyState("🏃", "Sin hábitos", "Añade tu primer hábito con el botón +") }
            } else {
                items(habits, key = { it.id }) { habit ->
                    HabitCard(
                        habit        = habit,
                        selectedDate = selectedDate,
                        expanded     = expandedId == habit.id,
                        onExpand     = { expandedId = if (expandedId == habit.id) null else habit.id },
                        vm           = vm
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAdd) AddHabitDialog(onDismiss = { showAdd = false }) { name, type, target, fType, fTarget ->
        vm.addHabit(name, type, target, fType, fTarget); showAdd = false
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Habit card — loggd.life style with expandable heatmap
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HabitCard(
    habit: HabitEntity, selectedDate: String, expanded: Boolean,
    onExpand: () -> Unit, vm: HabitsViewModel
) {
    val executions by vm.observeExecutions(habit.id).collectAsStateWithLifecycle(emptyList())
    val todayExec  = executions.find { it.dateCompleted == selectedDate }
    val isDone     = when (habit.habitType) {
        "VALUE" -> (todayExec?.valueLogged ?: 0) >= habit.valueTarget
        else    -> todayExec != null
    }
    val valueToday = todayExec?.valueLogged ?: 0

    // Heatmap data (loaded once when expanded)
    var heatmapData by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var maxVal      by remember { mutableIntStateOf(1) }
    LaunchedEffect(expanded, habit.id) {
        if (expanded) {
            val raw = vm.getHeatmap(habit.id)
            heatmapData = raw.associate { it.date_string to it.count }
            maxVal = raw.maxOfOrNull { it.count } ?: 1
        }
    }

    var showDelete by remember { mutableStateOf(false) }
    var showInput  by remember { mutableStateOf(false) }
    var inputText  by remember { mutableStateOf("") }

    FatumCard(modifier = Modifier.fillMaxWidth(), highlight = isDone) {
        // ── Header row ───────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            HabitToggle(done = isDone, onClick = {
                if (habit.habitType == "BOOLEAN") vm.toggleBoolean(habit.id)
                else showInput = !showInput
            })
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium,
                     color = if (isDone) FatumColors.TextMuted else FatumColors.TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(freqLabel(habit), style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted)
                    if (habit.habitType == "VALUE") {
                        Text("$valueToday / ${habit.valueTarget}",
                             style = MaterialTheme.typography.labelSmall, color = FatumColors.Green)
                    }
                }
            }
            if (habit.currentStreak > 0) StreakBadge(habit.currentStreak)
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onExpand, modifier = Modifier.size(32.dp)) {
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null,
                     tint = FatumColors.TextMuted, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { showDelete = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.MoreVert, null, tint = FatumColors.TextMuted, modifier = Modifier.size(16.dp))
            }
        }

        // ── Value input (for VALUE habits) ───────────────────────────────────
        AnimatedVisibility(showInput && habit.habitType == "VALUE") {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                OutlinedTextField(
                    value = inputText, onValueChange = { inputText = it },
                    label = { Text("Añadir ${if (habit.habitType == "VALUE") "valor" else ""}") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = fatumOutlinedFieldColors(),
                    trailingIcon = {
                        Row {
                            IconButton(onClick = {
                                val v = inputText.toIntOrNull() ?: 0
                                if (v != 0) { vm.logValue(habit.id, v); inputText = ""; showInput = false }
                            }) { Icon(Icons.Default.Add, null, tint = FatumColors.Green) }
                        }
                    }
                )
                if (valueToday > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                        Text("Hoy: $valueToday", style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted)
                        TextButton(onClick = { vm.logValue(habit.id, -valueToday) }, contentPadding = PaddingValues(0.dp)) {
                            Text("Resetear día", style = MaterialTheme.typography.labelSmall, color = FatumColors.Error)
                        }
                    }
                }
            }
        }

        // ── 7-day dots ───────────────────────────────────────────────────────
        Spacer(Modifier.height(10.dp))
        WeekDots(executions = executions, selectedDate = selectedDate, habit = habit)

        // ── Expanded: heatmap + streak info ──────────────────────────────────
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                HorizontalDivider(color = FatumColors.DividerLine, modifier = Modifier.padding(bottom = 10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Racha actual", style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted)
                        Text("${habit.currentStreak} días", style = MaterialTheme.typography.titleMedium, color = FatumColors.Green, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Récord", style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted)
                        Text("${habit.maxStreak} días", style = MaterialTheme.typography.titleMedium, color = FatumColors.TextPrimary)
                    }
                }
                Spacer(Modifier.height(10.dp))
                HabitHeatmap(data = heatmapData, maxVal = maxVal)
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text("Menos", style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted)
                    Spacer(Modifier.width(4.dp))
                    listOf(FatumColors.Heat0, FatumColors.Heat1, FatumColors.Heat2, FatumColors.Heat3, FatumColors.Heat4).forEach { c ->
                        Box(Modifier.padding(horizontal = 1.dp).size(10.dp).clip(RoundedCornerShape(2.dp)).background(c))
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("Más", style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted)
                }
            }
        }
    }

    if (showDelete) AlertDialog(
        onDismissRequest = { showDelete = false },
        containerColor = FatumColors.Surface, shape = RoundedCornerShape(20.dp),
        title = { Text("¿Eliminar hábito?", color = FatumColors.TextPrimary) },
        text  = { Text("Se eliminarán todos los registros de \"${habit.name}\".", color = FatumColors.TextSecondary) },
        confirmButton = {
            Button(onClick = { vm.deleteHabit(habit.id); showDelete = false },
                colors = ButtonDefaults.buttonColors(containerColor = FatumColors.Error)) { Text("Eliminar") }
        },
        dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancelar", color = FatumColors.TextSecondary) } }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 7-day dot strip
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WeekDots(executions: List<HabitExecutionEntity>, selectedDate: String, habit: HabitEntity) {
    val fmt    = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val anchor = runCatching { LocalDate.parse(selectedDate, fmt) }.getOrElse { LocalDate.now() }
    val days   = (6 downTo 0).map { anchor.minusDays(it.toLong()) }

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEach { date ->
            val key  = date.format(fmt)
            val exec = executions.find { it.dateCompleted == key }
            val done = when (habit.habitType) {
                "VALUE" -> (exec?.valueLogged ?: 0) >= habit.valueTarget
                else    -> exec != null
            }
            val isSel = key == selectedDate
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(if (isSel) 12.dp else 10.dp).clip(CircleShape)
                    .background(when { done -> FatumColors.Green; isSel -> FatumColors.SurfaceHigh; else -> FatumColors.SurfaceVariant })
                    .then(if (isSel && !done) Modifier.border(1.dp, FatumColors.Green, CircleShape) else Modifier))
                Spacer(Modifier.height(2.dp))
                Text(date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale("es")),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSel) FatumColors.Green else FatumColors.TextMuted)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Retroactive date picker
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RetroactivePicker(selectedDate: String, onDateSelected: (String) -> Unit) {
    val fmt   = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = LocalDate.now()
    val rows  = (29 downTo 0).map { today.minusDays(it.toLong()) }.chunked(7)

    FatumCard(modifier = Modifier.fillMaxWidth()) {
        Text("Registro retroactivo", style = MaterialTheme.typography.titleSmall, color = FatumColors.TextSecondary)
        Spacer(Modifier.height(10.dp))
        rows.forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                week.forEach { date ->
                    val key = date.format(fmt)
                    val sel = key == selectedDate
                    Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                        .background(when { sel -> FatumColors.Green; date == today -> FatumColors.GreenSurface; else -> FatumColors.SurfaceVariant })
                        .border(if (date == today && !sel) 1.dp else 0.dp, FatumColors.GreenBorder, RoundedCornerShape(8.dp))
                        .clickable { onDateSelected(key) },
                        contentAlignment = Alignment.Center) {
                        Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelLarge,
                            color = when { sel -> Color(0xFF0F1117); date == today -> FatumColors.Green; else -> FatumColors.TextSecondary })
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
private fun AddHabitDialog(onDismiss: () -> Unit, onConfirm: (String, String, Int, String, Int) -> Unit) {
    var name       by remember { mutableStateOf("") }
    var type       by remember { mutableStateOf("BOOLEAN") }  // BOOLEAN | VALUE
    var valueTarget by remember { mutableIntStateOf(60) }
    var freqType   by remember { mutableStateOf("DAILY") }
    var freqTarget by remember { mutableIntStateOf(3) }

    AlertDialog(onDismissRequest = onDismiss,
        containerColor = FatumColors.Surface, shape = RoundedCornerShape(20.dp),
        title = { Text("Nuevo hábito", color = FatumColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(),
                    colors = fatumOutlinedFieldColors(), singleLine = true)

                // Habit type
                Text("Tipo", style = MaterialTheme.typography.labelLarge, color = FatumColors.TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FatumChip("Hecho/No hecho", selected = type == "BOOLEAN", onClick = { type = "BOOLEAN" })
                    FatumChip("Valor", selected = type == "VALUE", onClick = { type = "VALUE" })
                }

                AnimatedVisibility(visible = type == "VALUE") {
                    OutlinedTextField(value = valueTarget.toString(), onValueChange = { valueTarget = it.toIntOrNull() ?: 60 },
                        label = { Text("Objetivo diario (ej: 60 minutos)") }, modifier = Modifier.fillMaxWidth(),
                        colors = fatumOutlinedFieldColors(), singleLine = true)
                }

                // Frequency
                Text("Frecuencia", style = MaterialTheme.typography.labelLarge, color = FatumColors.TextMuted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val opts = listOf("DAILY" to "Diario", "WEEKLY_X" to "X/semana", "WEEKLY" to "Semanal", "MONTHLY" to "Mensual")
                    items(opts) { (k, l) -> FatumChip(l, selected = freqType == k, onClick = { freqType = k }) }
                }
                AnimatedVisibility(freqType == "WEEKLY_X") {
                    Column {
                        Text("Veces por semana: $freqTarget", style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted)
                        Slider(value = freqTarget.toFloat(), onValueChange = { freqTarget = it.toInt() },
                            valueRange = 1f..6f, steps = 4,
                            colors = SliderDefaults.colors(thumbColor = FatumColors.Green, activeTrackColor = FatumColors.Green))
                    }
                }
            }
        },
        confirmButton = { FatumButton("Crear hábito", enabled = name.isNotBlank(),
            onClick = { if (name.isNotBlank()) onConfirm(name.trim(), type, if (type == "VALUE") valueTarget else 1, freqType, freqTarget) }) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = FatumColors.TextSecondary) } }
    )
}

private fun freqLabel(h: HabitEntity) = when (h.frequencyType) {
    "DAILY"    -> "Diario"
    "WEEKLY_X" -> "${h.frequencyTarget}× semana"
    "WEEKLY"   -> "Semanal"
    "MONTHLY"  -> "Mensual"
    else       -> h.frequencyType
}

private fun formatDateShort(ds: String) = try {
    val d = LocalDate.parse(ds, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    if (d == LocalDate.now()) "Hoy" else d.format(DateTimeFormatter.ofPattern("d MMM", Locale("es")))
} catch (_: Exception) { ds }
