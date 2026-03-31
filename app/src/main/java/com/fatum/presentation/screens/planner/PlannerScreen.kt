package com.fatum.presentation.screens.planner

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatum.data.db.entities.CalendarEventEntity
import com.fatum.presentation.components.*
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
    val syncState    by vm.syncState.collectAsStateWithLifecycle()
    val syncMsg      by vm.syncMsg.collectAsStateWithLifecycle()
    var showAdd      by remember { mutableStateOf(false) }
    var editEvent    by remember { mutableStateOf<CalendarEventEntity?>(null) }

    Scaffold(
        containerColor = FatumColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Agenda", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FatumColors.Background),
                actions = {
                    IconButton(onClick = { vm.syncCalendar() }) {
                        if (syncState == PlannerViewModel.SyncState.SYNCING)
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = FatumColors.Green)
                        else Icon(Icons.Default.Sync, "Sincronizar",
                            tint = when(syncState) {
                                PlannerViewModel.SyncState.SUCCESS -> FatumColors.Green
                                PlannerViewModel.SyncState.ERROR   -> FatumColors.Error
                                else -> FatumColors.TextMuted
                            })
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
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Sync status banner
            AnimatedVisibility(syncMsg != null) {
                Row(Modifier.fillMaxWidth()
                    .background(when(syncState) {
                        PlannerViewModel.SyncState.SUCCESS -> FatumColors.Green.copy(.1f)
                        PlannerViewModel.SyncState.ERROR   -> FatumColors.Error.copy(.1f)
                        else -> FatumColors.SurfaceVariant
                    }).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(syncMsg ?: "", style = MaterialTheme.typography.bodySmall,
                         color = if (syncState == PlannerViewModel.SyncState.ERROR) FatumColors.Error else FatumColors.Green)
                }
            }
            // Week strip
            WeekStrip(selectedDate, vm::selectDate)
            HorizontalDivider(color = FatumColors.DividerLine)
            // Events
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (events.isEmpty()) {
                    item { EmptyState("📅", "Sin eventos", "Añade un evento o sincroniza con Google Calendar\npulsando el icono de arriba") }
                } else {
                    items(events, key = { it.id }) { event ->
                        EventCard(event, onEdit = { editEvent = event }, onDelete = { vm.deleteEvent(event) })
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showAdd) EventDialog(null, selectedDate, { showAdd = false }) { t, d, imp, s, e, rrule ->
        vm.addEvent(t, d, imp, s, e, rrule); showAdd = false
    }
    editEvent?.let { ev ->
        EventDialog(ev, selectedDate, { editEvent = null }) { t, d, imp, s, e, rrule ->
            vm.updateEvent(ev.copy(title = t, description = d, importance = imp,
                startTimestamp = s, endTimestamp = e, recurrenceRule = rrule))
            editEvent = null
        }
    }
}

@Composable
private fun WeekStrip(selectedDate: LocalDate, onSelect: (LocalDate) -> Unit) {
    val monday = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() - 1)
    val days   = (0..6).map { monday.plusDays(it.toLong()) }
    val today  = LocalDate.now()
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly) {
        days.forEach { day ->
            val isSel = day == selectedDate
            val isTd  = day == today
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    .background(when { isSel -> FatumColors.Green; isTd -> FatumColors.GreenSurface; else -> Color.Transparent })
                    .clickable { onSelect(day) }.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale("es")),
                    style = MaterialTheme.typography.labelSmall,
                    color = when { isSel -> Color(0xFF0F1117); isTd -> FatumColors.Green; else -> FatumColors.TextMuted })
                Spacer(Modifier.height(2.dp))
                Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium,
                    color = when { isSel -> Color(0xFF0F1117); isTd -> FatumColors.Green; else -> FatumColors.TextPrimary })
            }
        }
    }
}

@Composable
private fun EventCard(event: CalendarEventEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    val fmt   = DateTimeFormatter.ofPattern("HH:mm")
    val start = Instant.ofEpochMilli(event.startTimestamp).atZone(ZoneId.systemDefault()).toLocalTime().format(fmt)
    val end   = Instant.ofEpochMilli(event.endTimestamp).atZone(ZoneId.systemDefault()).toLocalTime().format(fmt)
    val impColor = importanceColor(event.importance)

    FatumCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(start, style = MaterialTheme.typography.labelLarge, color = impColor)
                Text(end,   style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted)
            }
            Box(Modifier.width(3.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(impColor))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.titleSmall, color = FatumColors.TextPrimary)
                if (!event.description.isNullOrBlank())
                    Text(event.description, style = MaterialTheme.typography.bodySmall, color = FatumColors.TextMuted, maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (event.isFromGcal) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CalendarMonth, null, tint = FatumColors.TextMuted, modifier = Modifier.size(10.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("GCal", style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted)
                        }
                    }
                    if (!event.recurrenceRule.isNullOrBlank())
                        Text("↻ Recurrente", style = MaterialTheme.typography.labelSmall, color = FatumColors.Info)
                    ImportancePill(event.importance)
                }
            }
            IconButton(onClick = onEdit,   modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.Edit,   null, tint = FatumColors.TextMuted, modifier = Modifier.size(16.dp)) }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.Delete, null, tint = FatumColors.Error,    modifier = Modifier.size(16.dp)) }
        }
    }
}

@Composable
private fun ImportancePill(importance: String) {
    val (bg, txt) = when (importance) {
        "HIGH"   -> FatumColors.Error.copy(.15f)   to FatumColors.Error
        "MEDIUM" -> FatumColors.Warning.copy(.15f) to FatumColors.Warning
        else     -> FatumColors.SurfaceVariant      to FatumColors.TextMuted
    }
    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(bg).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(when(importance) { "HIGH" -> "Alta"; "MEDIUM" -> "Media"; else -> "Baja" },
             style = MaterialTheme.typography.labelSmall, color = txt)
    }
}

@Composable
private fun EventDialog(
    event: CalendarEventEntity?, selectedDate: LocalDate, onDismiss: () -> Unit,
    onConfirm: (String, String?, String, Long, Long, String?) -> Unit
) {
    var title      by remember { mutableStateOf(event?.title ?: "") }
    var desc       by remember { mutableStateOf(event?.description ?: "") }
    var importance by remember { mutableStateOf(event?.importance ?: "MEDIUM") }
    var startH     by remember { mutableStateOf("09") }
    var startM     by remember { mutableStateOf("00") }
    var endH       by remember { mutableStateOf("10") }
    var endM       by remember { mutableStateOf("00") }
    var rrule      by remember { mutableStateOf(event?.recurrenceRule ?: "") }
    var rruleType  by remember { mutableStateOf("none") }

    // Preset times from existing event
    LaunchedEffect(event) {
        event?.let {
            val sTime = Instant.ofEpochMilli(it.startTimestamp).atZone(ZoneId.systemDefault()).toLocalTime()
            val eTime = Instant.ofEpochMilli(it.endTimestamp).atZone(ZoneId.systemDefault()).toLocalTime()
            startH = "%02d".format(sTime.hour); startM = "%02d".format(sTime.minute)
            endH   = "%02d".format(eTime.hour);   endM = "%02d".format(eTime.minute)
        }
    }

    AlertDialog(onDismissRequest = onDismiss,
        containerColor = FatumColors.Surface, shape = RoundedCornerShape(20.dp),
        title = { Text(if (event == null) "Nuevo evento" else "Editar evento", color = FatumColors.TextPrimary) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { OutlinedTextField(title, { title = it }, label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(), colors = fatumOutlinedFieldColors(), singleLine = true) }
                item { OutlinedTextField(desc, { desc = it }, label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth(), colors = fatumOutlinedFieldColors(), minLines = 2, maxLines = 3) }
                item {
                    Text("Importancia", style = MaterialTheme.typography.labelLarge, color = FatumColors.TextMuted)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("HIGH" to "Alta","MEDIUM" to "Media","LOW" to "Baja").forEach { (k, l) ->
                            FatumChip(l, importance == k, { importance = k })
                        }
                    }
                }
                item {
                    Text("Hora inicio – fin", style = MaterialTheme.typography.labelLarge, color = FatumColors.TextMuted)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(startH, { startH = it.take(2) }, label = { Text("H") }, modifier = Modifier.weight(1f), colors = fatumOutlinedFieldColors(), singleLine = true)
                        OutlinedTextField(startM, { startM = it.take(2) }, label = { Text("M") }, modifier = Modifier.weight(1f), colors = fatumOutlinedFieldColors(), singleLine = true)
                        Text("—", color = FatumColors.TextMuted, modifier = Modifier.align(Alignment.CenterVertically))
                        OutlinedTextField(endH, { endH = it.take(2) }, label = { Text("H") }, modifier = Modifier.weight(1f), colors = fatumOutlinedFieldColors(), singleLine = true)
                        OutlinedTextField(endM, { endM = it.take(2) }, label = { Text("M") }, modifier = Modifier.weight(1f), colors = fatumOutlinedFieldColors(), singleLine = true)
                    }
                }
                item {
                    Text("Repetición", style = MaterialTheme.typography.labelLarge, color = FatumColors.TextMuted)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val opts = listOf(
                            "none"    to "Sin repetición",
                            "daily"   to "Cada día",
                            "weekly"  to "Cada semana",
                            "workday" to "Lun–Vie",
                            "monthly" to "Cada mes",
                            "custom"  to "Personalizar"
                        )
                        items(opts) { (k, l) -> FatumChip(l, rruleType == k, { rruleType = k; rrule = toRrule(k) }) }
                    }
                    if (rruleType == "custom") {
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(rrule, { rrule = it }, label = { Text("RRULE (RFC 5545)") },
                            modifier = Modifier.fillMaxWidth(), colors = fatumOutlinedFieldColors(), singleLine = true,
                            supportingText = { Text("Ej: FREQ=WEEKLY;BYDAY=MO,WE,FR", style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted) })
                    }
                }
            }
        },
        confirmButton = { FatumButton(if (event == null) "Crear" else "Guardar", enabled = title.isNotBlank(), onClick = {
            if (title.isBlank()) return@FatumButton
            val zone = ZoneId.systemDefault()
            val sTs  = selectedDate.atTime(startH.toIntOrNull() ?: 9, startM.toIntOrNull() ?: 0).atZone(zone).toInstant().toEpochMilli()
            val eTs  = selectedDate.atTime(endH.toIntOrNull() ?: 10,   endM.toIntOrNull() ?: 0).atZone(zone).toInstant().toEpochMilli()
            onConfirm(title.trim(), desc.ifBlank { null }, importance, sTs, eTs, rrule.ifBlank { null })
        }) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = FatumColors.TextSecondary) } }
    )
}

private fun toRrule(type: String) = when (type) {
    "daily"   -> "FREQ=DAILY"
    "weekly"  -> "FREQ=WEEKLY"
    "workday" -> "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"
    "monthly" -> "FREQ=MONTHLY"
    else      -> ""
}
