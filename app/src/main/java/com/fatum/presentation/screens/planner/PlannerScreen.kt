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
import androidx.compose.ui.text.style.TextAlign
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

    var showDatePicker by remember { mutableStateOf(false) }

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
                        else Icon(Icons.Default.Sync, "Sincronizar", tint = FatumColors.TextMuted)
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
            AnimatedVisibility(syncMsg != null) {
                Row(Modifier.fillMaxWidth().background(FatumColors.SurfaceVariant).padding(12.dp)) {
                    Text(syncMsg ?: "", style = MaterialTheme.typography.bodySmall, color = FatumColors.Green)
                }
            }

            // Navegador Mensual
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { vm.selectDate(selectedDate.minusWeeks(1)) }) {
                    Icon(Icons.Default.ChevronLeft, "Semana anterior", tint = FatumColors.TextPrimary)
                }
                TextButton(onClick = { showDatePicker = true }) {
                    val month = selectedDate.month.getDisplayName(TextStyle.FULL, Locale("es")).replaceFirstChar { it.uppercase() }
                    Text("$month ${selectedDate.year}", style = MaterialTheme.typography.titleMedium, color = FatumColors.TextPrimary)
                }
                IconButton(onClick = { vm.selectDate(selectedDate.plusWeeks(1)) }) {
                    Icon(Icons.Default.ChevronRight, "Semana siguiente", tint = FatumColors.TextPrimary)
                }
            }

            WeekStrip(selectedDate, vm::selectDate)
            HorizontalDivider(color = FatumColors.DividerLine)

            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (events.isEmpty()) {
                    item { EmptyState("📅", "Sin eventos", "Añade un evento o pulsa el icono para sincronizar.") }
                } else {
                    items(events, key = { it.id }) { event ->
                        EventCard(event, onEdit = { editEvent = event }, onDelete = { vm.deleteEvent(event) })
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { vm.selectDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    showDatePicker = false
                }) { Text("Aceptar", color = FatumColors.Green) }
            }
        ) { DatePicker(state = dpState) }
    }

    if (showAdd) EventDialog(null, selectedDate, { showAdd = false }) { t, d, imp, s, e, rrule ->
        vm.addEvent(t, d, imp, s, e, rrule); showAdd = false
    }

    editEvent?.let { ev ->
        EventDialog(ev, selectedDate, { editEvent = null }) { t, d, imp, s, e, rrule ->
            vm.updateEvent(ev.copy(title = t, description = d, importance = imp, startTimestamp = s, endTimestamp = e, recurrenceRule = rrule))
            editEvent = null
        }
    }
}

@Composable
private fun WeekStrip(selectedDate: LocalDate, onSelect: (LocalDate) -> Unit) {
    val monday = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() - 1)
    val days   = (0..6).map { monday.plusDays(it.toLong()) }
    val today  = LocalDate.now()
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        days.forEach { day ->
            val isSel = day == selectedDate
            val isTd  = day == today
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(12.dp))
                .background(when { isSel -> FatumColors.Green; isTd -> FatumColors.GreenSurface; else -> Color.Transparent })
                .clickable { onSelect(day) }.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale("es")), style = MaterialTheme.typography.labelSmall,
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
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    val start = try { Instant.ofEpochMilli(event.startTimestamp).atZone(ZoneId.systemDefault()).toLocalTime().format(fmt) } catch(e:Exception){ "--:--" }
    val end   = try { Instant.ofEpochMilli(event.endTimestamp).atZone(ZoneId.systemDefault()).toLocalTime().format(fmt) } catch(e:Exception){ "--:--" }
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
                    if (!event.recurrenceRule.isNullOrBlank()) Text("↻ Recurrente", style = MaterialTheme.typography.labelSmall, color = FatumColors.Info)
                    ImportancePill(event.importance)
                }
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.Edit, null, tint = FatumColors.TextMuted, modifier = Modifier.size(16.dp)) }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.Delete, null, tint = FatumColors.Error, modifier = Modifier.size(16.dp)) }
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
        Text(when(importance) { "HIGH" -> "Alta"; "MEDIUM" -> "Media"; else -> "Baja" }, style = MaterialTheme.typography.labelSmall, color = txt)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDialog(
    event: CalendarEventEntity?, currentDate: LocalDate, onDismiss: () -> Unit,
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

    // Selectores de Fecha
    var eventDate      by remember { mutableStateOf(currentDate) }
    var showEventDateDp by remember { mutableStateOf(false) }

    var recurrenceEnd  by remember { mutableStateOf<LocalDate?>(null) }
    var showEndDp      by remember { mutableStateOf(false) }

    LaunchedEffect(event) {
        event?.let {
            try {
                val sInstant = Instant.ofEpochMilli(it.startTimestamp).atZone(ZoneId.systemDefault())
                val eInstant = Instant.ofEpochMilli(it.endTimestamp).atZone(ZoneId.systemDefault())
                eventDate = sInstant.toLocalDate()
                startH = "%02d".format(sInstant.hour); startM = "%02d".format(sInstant.minute)
                endH   = "%02d".format(eInstant.hour); endM   = "%02d".format(eInstant.minute)
            } catch (e: Exception) {}
        }
    }

    if (showEventDateDp) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = eventDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { showEventDateDp = false }, confirmButton = {
            TextButton(onClick = { dpState.selectedDateMillis?.let { eventDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }; showEventDateDp = false }) { Text("Aceptar") }
        }) { DatePicker(dpState) }
    }

    if (showEndDp) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = (recurrenceEnd ?: eventDate.plusMonths(1)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { showEndDp = false }, confirmButton = {
            TextButton(onClick = { dpState.selectedDateMillis?.let { recurrenceEnd = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }; showEndDp = false }) { Text("Aceptar") }
        }) { DatePicker(dpState) }
    }

    AlertDialog(onDismissRequest = onDismiss, containerColor = FatumColors.Surface, shape = RoundedCornerShape(20.dp),
        title = { Text(if (event == null) "Nuevo evento" else "Editar evento", color = FatumColors.TextPrimary) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { OutlinedTextField(title, { title = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), colors = fatumOutlinedFieldColors(), singleLine = true) }

                // Botón para elegir la fecha del evento
                item {
                    Text("Fecha del evento", style = MaterialTheme.typography.labelLarge, color = FatumColors.TextMuted)
                    OutlinedButton(onClick = { showEventDateDp = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.CalendarMonth, null, tint = FatumColors.TextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(eventDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), color = FatumColors.TextPrimary)
                    }
                }

                item {
                    Text("Hora inicio – fin", style = MaterialTheme.typography.labelLarge, color = FatumColors.TextMuted)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(startH, { startH = it.take(2) }, label = { Text("H") }, modifier = Modifier.weight(1f), colors = fatumOutlinedFieldColors())
                        OutlinedTextField(startM, { startM = it.take(2) }, label = { Text("M") }, modifier = Modifier.weight(1f), colors = fatumOutlinedFieldColors())
                        Text("—", color = FatumColors.TextMuted, modifier = Modifier.align(Alignment.CenterVertically))
                        OutlinedTextField(endH, { endH = it.take(2) }, label = { Text("H") }, modifier = Modifier.weight(1f), colors = fatumOutlinedFieldColors())
                        OutlinedTextField(endM, { endM = it.take(2) }, label = { Text("M") }, modifier = Modifier.weight(1f), colors = fatumOutlinedFieldColors())
                    }
                }

                item {
                    Text("Repetición", style = MaterialTheme.typography.labelLarge, color = FatumColors.TextMuted)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("none" to "Ninguna", "daily" to "Diaria", "weekly" to "Semanal", "monthly" to "Mensual")) { (k, l) ->
                            FatumChip(l, rruleType == k, { rruleType = k; rrule = toRrule(k, recurrenceEnd) })
                        }
                    }

                    if (rruleType != "none") {
                        Spacer(Modifier.height(12.dp))
                        Text("Finaliza el (Opcional)", style = MaterialTheme.typography.labelLarge, color = FatumColors.TextMuted)
                        OutlinedButton(onClick = { showEndDp = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(recurrenceEnd?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "Para siempre", color = FatumColors.TextPrimary)
                        }
                        LaunchedEffect(recurrenceEnd) { rrule = toRrule(rruleType, recurrenceEnd) }
                    }
                }

                item { OutlinedTextField(desc, { desc = it }, label = { Text("Descripción (opcional)") }, modifier = Modifier.fillMaxWidth(), colors = fatumOutlinedFieldColors(), minLines = 2, maxLines = 3) }
            }
        },
        confirmButton = { FatumButton(if (event == null) "Crear" else "Guardar", enabled = title.isNotBlank(), onClick = {
            if (title.isBlank()) return@FatumButton
            val zone = ZoneId.systemDefault()
            val sTs = try { eventDate.atTime(startH.toIntOrNull() ?: 9, startM.toIntOrNull() ?: 0).atZone(zone).toInstant().toEpochMilli() } catch (e: Exception) { System.currentTimeMillis() }
            val eTs = try { eventDate.atTime(endH.toIntOrNull() ?: 10, endM.toIntOrNull() ?: 0).atZone(zone).toInstant().toEpochMilli() } catch (e: Exception) { System.currentTimeMillis() + 3600000 }
            onConfirm(title.trim(), desc.ifBlank { null }, importance, sTs, eTs, rrule.ifBlank { null })
        }) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = FatumColors.TextSecondary) } }
    )
}

private fun toRrule(type: String, end: LocalDate?): String {
    val base = when (type) {
        "daily"   -> "FREQ=DAILY"
        "weekly"  -> "FREQ=WEEKLY"
        "monthly" -> "FREQ=MONTHLY"
        else      -> return ""
    }
    return if (end != null) "$base;UNTIL=${end.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'235959'Z'"))}" else base
}