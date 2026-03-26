package com.fatum.presentation.screens.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatum.data.db.entities.CalendarEventEntity
import com.fatum.presentation.components.*
import com.fatum.presentation.theme.FatumColors
import com.fatum.presentation.viewmodels.PlannerViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(vm: PlannerViewModel = hiltViewModel()) {
    val selectedDate by vm.selectedDate.collectAsStateWithLifecycle()
    val events       by vm.eventsForDay.collectAsStateWithLifecycle()
    val tasks        by vm.tasksForDay.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = FatumColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Agenda") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FatumColors.Background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = FatumColors.Accent) {
                Icon(Icons.Default.Add, null, tint = FatumColors.Background)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // ── Week strip ────────────────────────────────────────────
            WeekNavStrip(selectedDate = selectedDate, onDaySelected = vm::selectDate)
            HorizontalDivider(color = FatumColors.Divider)

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Events from GCal + local (RF-3.1)
                if (events.isNotEmpty()) {
                    item { SectionHeader("Eventos") }
                    items(events, key = { it.id }) { event ->
                        EventRow(event = event, onDelete = { vm.deleteEvent(event) })
                    }
                }
                // Tasks with due date (RF-3.1)
                if (tasks.isNotEmpty()) {
                    item { SectionHeader("Tareas del día") }
                    items(tasks, key = { it.id }) { task ->
                        FatumCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PriorityStars(value = task.priorityStars)
                                Spacer(Modifier.width(8.dp))
                                Text(task.title, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEventDialog(
            selectedDate = selectedDate,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, startTs, endTs, rrule, priority ->
                vm.addEvent(title, startTs, endTs, rrule, priority, null)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun WeekNavStrip(selectedDate: LocalDate, onDaySelected: (LocalDate) -> Unit) {
    val monday = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() - 1)
    val days = (0..6).map { monday.plusDays(it.toLong()) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEach { day ->
            val isSelected = day == selectedDate
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) FatumColors.Accent else FatumColors.Background)
                    .clickable { onDaySelected(day) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = day.dayOfWeek.name.take(1),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) FatumColors.Background else FatumColors.PrimaryVariant
                )
                Text(
                    text = day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) FatumColors.Background else FatumColors.Primary
                )
            }
        }
    }
}

@Composable
private fun EventRow(event: CalendarEventEntity, onDelete: () -> Unit) {
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    val start = java.time.Instant.ofEpochMilli(event.startTimestamp)
        .atZone(ZoneId.systemDefault()).toLocalTime().format(timeFmt)
    val end = java.time.Instant.ofEpochMilli(event.endTimestamp)
        .atZone(ZoneId.systemDefault()).toLocalTime().format(timeFmt)

    FatumCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.width(50.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(start, style = MaterialTheme.typography.labelSmall, color = FatumColors.Accent)
                Text(end, style = MaterialTheme.typography.labelSmall, color = FatumColors.PrimaryVariant)
            }
            Box(modifier = Modifier.width(2.dp).height(40.dp).background(FatumColors.Accent))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.titleMedium)
                PriorityStars(value = event.priorityStars)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = FatumColors.Error)
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
    var priority by remember { mutableStateOf(1) }
    var rrule    by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo evento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = startH, onValueChange = { startH = it }, label = { Text("Inicio h") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = startMin, onValueChange = { startMin = it }, label = { Text("min") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = endH, onValueChange = { endH = it }, label = { Text("Fin h") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = endMin, onValueChange = { endMin = it }, label = { Text("min") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = rrule, onValueChange = { rrule = it }, label = { Text("RRULE (opcional)") }, modifier = Modifier.fillMaxWidth())
                Text("Prioridad", style = MaterialTheme.typography.labelSmall)
                PriorityStars(value = priority, onValueChange = { priority = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) return@TextButton
                val startTs = selectedDate.atTime(
                    startH.toIntOrNull() ?: 9, startMin.toIntOrNull() ?: 0
                ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endTs = selectedDate.atTime(
                    endH.toIntOrNull() ?: 10, endMin.toIntOrNull() ?: 0
                ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                onConfirm(title, startTs, endTs, rrule.ifBlank { null }, priority)
            }) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
