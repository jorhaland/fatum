package com.fatum.presentation.screens.planner

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatum.data.db.entities.CalendarEventEntity
import com.fatum.presentation.components.*
import com.fatum.presentation.components.fatumOutlinedFieldColors

import com.fatum.presentation.theme.FatumColors
import com.fatum.presentation.viewmodels.PlannerViewModel
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(vm: PlannerViewModel = hiltViewModel()) {
    val selectedDate by vm.selectedDate.collectAsStateWithLifecycle()
    val events       by vm.eventsForDay.collectAsStateWithLifecycle()
    val tasks        by vm.tasksForDay.collectAsStateWithLifecycle()
    val syncState    by vm.syncState.collectAsStateWithLifecycle()
    var showAdd      by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = FatumColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Agenda", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FatumColors.Background),
                actions = {
                    // Google Calendar sync button
                    IconButton(onClick = { vm.syncCalendar() }) {
                        if (syncState == PlannerViewModel.SyncState.SYNCING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = FatumColors.Green
                            )
                        } else {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = "Sincronizar con Google Calendar",
                                tint = when (syncState) {
                                    PlannerViewModel.SyncState.SUCCESS -> FatumColors.Green
                                    PlannerViewModel.SyncState.ERROR   -> FatumColors.Error
                                    else                               -> FatumColors.TextSecondary
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = FatumColors.Green,
                contentColor = Color(0xFF0F1117),
                shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ── Sync status banner ──────────────────────────────────────────
            if (syncState == PlannerViewModel.SyncState.ERROR) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(FatumColors.Error.copy(0.1f)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Warning, null, tint = FatumColors.Error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Error de sincronización. Verifica que iniciaste sesión con Google.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FatumColors.Error
                    )
                }
            }

            // ── Week strip ──────────────────────────────────────────────────
            WeekStrip(selectedDate = selectedDate, onDaySelected = vm::selectDate)
            HorizontalDivider(color = FatumColors.DividerLine)

            // ── Day content ─────────────────────────────────────────────────
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (events.isEmpty() && tasks.isEmpty()) {
                    item {
                        EmptyState(
                            emoji = "📅",
                            title = "Día libre",
                            subtitle = "Añade un evento o sincroniza con Google Calendar"
                        )
                    }
                }

                if (events.isNotEmpty()) {
                    item { SectionHeader("Eventos") }
                    items(events, key = { it.id }) { event ->
                        EventCard(event = event, onDelete = { vm.deleteEvent(event) })
                    }
                }

                if (tasks.isNotEmpty()) {
                    item { SectionHeader("Tareas del día") }
                    items(tasks, key = { it.id }) { task ->
                        FatumCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PriorityStars(value = task.priorityStars)
                                Spacer(Modifier.width(8.dp))
                                Text(task.title, style = MaterialTheme.typography.bodyMedium, color = FatumColors.TextPrimary)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showAdd) {
        AddEventDialog(
            selectedDate = selectedDate,
            onDismiss = { showAdd = false },
            onConfirm = { title, start, end, rrule, priority ->
                vm.addEvent(title, start, end, rrule, priority, null)
                showAdd = false
            }
        )
    }
}

@Composable
private fun WeekStrip(selectedDate: LocalDate, onDaySelected: (LocalDate) -> Unit) {
    val monday = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() - 1)
    val days   = (0..6).map { monday.plusDays(it.toLong()) }
    val today  = LocalDate.now()

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEach { day ->
            val isSelected = day == selectedDate
            val isToday    = day == today
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(when { isSelected -> FatumColors.Green; isToday -> FatumColors.GreenSurface; else -> Color.Transparent })
                    .clickable { onDaySelected(day) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale("es")),
                    style = MaterialTheme.typography.labelSmall,
                    color = when { isSelected -> Color(0xFF0F1117); isToday -> FatumColors.Green; else -> FatumColors.TextMuted }
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = when { isSelected -> Color(0xFF0F1117); isToday -> FatumColors.Green; else -> FatumColors.TextPrimary }
                )
            }
        }
    }
}

@Composable
private fun EventCard(event: CalendarEventEntity, onDelete: () -> Unit) {
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    val start = Instant.ofEpochMilli(event.startTimestamp).atZone(ZoneId.systemDefault()).toLocalTime().format(timeFmt)
    val end   = Instant.ofEpochMilli(event.endTimestamp).atZone(ZoneId.systemDefault()).toLocalTime().format(timeFmt)

    FatumCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Time column
            Column(
                modifier = Modifier.width(50.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(start, style = MaterialTheme.typography.labelLarge, color = FatumColors.Green)
                Text(end,   style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted)
            }
            Box(modifier = Modifier.width(2.dp).height(36.dp).background(FatumColors.Green).clip(RoundedCornerShape(1.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.titleSmall, color = FatumColors.TextPrimary)
                if (event.isFromGcal) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarMonth, null, tint = FatumColors.TextMuted, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Google Calendar", style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted)
                    }
                }
                PriorityStars(value = event.priorityStars)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Delete, null, tint = FatumColors.TextMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun AddEventDialog(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Long, String?, Int) -> Unit
) {
    var title    by remember { mutableStateOf("") }
    var startH   by remember { mutableStateOf("09") }
    var startMin by remember { mutableStateOf("00") }
    var endH     by remember { mutableStateOf("10") }
    var endMin   by remember { mutableStateOf("00") }
    var priority by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FatumColors.Surface,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Nuevo evento", color = FatumColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fatumOutlinedFieldColors(), singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = startH, onValueChange = { startH = it.take(2) },
                        label = { Text("Inicio h") }, modifier = Modifier.weight(1f), colors = fatumOutlinedFieldColors(), singleLine = true)
                    OutlinedTextField(value = startMin, onValueChange = { startMin = it.take(2) },
                        label = { Text("min") }, modifier = Modifier.weight(1f), colors = fatumOutlinedFieldColors(), singleLine = true)
                    OutlinedTextField(value = endH, onValueChange = { endH = it.take(2) },
                        label = { Text("Fin h") }, modifier = Modifier.weight(1f), colors = fatumOutlinedFieldColors(), singleLine = true)
                    OutlinedTextField(value = endMin, onValueChange = { endMin = it.take(2) },
                        label = { Text("min") }, modifier = Modifier.weight(1f), colors = fatumOutlinedFieldColors(), singleLine = true)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Prioridad", style = MaterialTheme.typography.labelLarge, color = FatumColors.TextMuted)
                    PriorityStars(value = priority, onValueChange = { priority = it })
                }
            }
        },
        confirmButton = {
            FatumButton("Crear", onClick = {
                if (title.isBlank()) return@FatumButton
                val zone = ZoneId.systemDefault()
                val startTs = selectedDate.atTime(startH.toIntOrNull() ?: 9, startMin.toIntOrNull() ?: 0)
                    .atZone(zone).toInstant().toEpochMilli()
                val endTs = selectedDate.atTime(endH.toIntOrNull() ?: 10, endMin.toIntOrNull() ?: 0)
                    .atZone(zone).toInstant().toEpochMilli()
                onConfirm(title.trim(), startTs, endTs, null, priority)
            }, enabled = title.isNotBlank())
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = FatumColors.TextSecondary) } }
    )
}