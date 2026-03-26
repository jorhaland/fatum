package com.fatum.presentation.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatum.data.db.entities.LogEntity
import com.fatum.presentation.components.*
import com.fatum.presentation.theme.FatumColors
import com.fatum.presentation.viewmodels.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel = hiltViewModel()) {
    val logs          by vm.logs.collectAsStateWithLifecycle()
    val inputText     by vm.inputText.collectAsStateWithLifecycle()
    val todayMood     by vm.todayMood.collectAsStateWithLifecycle()
    val activeTag     by vm.activeTag.collectAsStateWithLifecycle()
    val heatMap       by vm.heatMap.collectAsStateWithLifecycle()
    val timeCapsule   by vm.timeCapsule.collectAsStateWithLifecycle()

    var showMoodBar    by remember { mutableStateOf(false) }
    var showTimeCapsule by remember { mutableStateOf(false) }
    var editTarget     by remember { mutableStateOf<LogEntity?>(null) }
    var editText       by remember { mutableStateOf("") }

    // Group logs by date header
    val grouped = logs.groupBy { it.dateString }

    Scaffold(
        containerColor = FatumColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("FATUM", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FatumColors.Background),
                actions = {
                    TextButton(onClick = { showTimeCapsule = !showTimeCapsule }) {
                        Text("📅 Hoy", color = FatumColors.Accent)
                    }
                    TextButton(onClick = { showMoodBar = !showMoodBar }) {
                        Text(
                            text = if (todayMood != null) "😊 ${todayMood!!.score}" else "😶 Humor",
                            color = FatumColors.PrimaryVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Mood bar (RF-1.3) ─────────────────────────────────────────
            item {
                AnimatedVisibility(visible = showMoodBar) {
                    FatumCard {
                        Text("¿Cómo estás hoy?", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        MoodSelector(current = todayMood?.score, onSelect = { vm.setMood(it) })
                    }
                }
            }

            // ── Heat map (RF-6.1) ─────────────────────────────────────────
            item {
                FatumCard {
                    Text("Actividad del año", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    HeatMapGrid(data = heatMap, onDayClick = { /* jump to date */ })
                }
            }

            // ── Quick-add input (RF-1.1) ──────────────────────────────────
            item {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = vm::onInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("¿Qué está pasando? Usa #etiquetas…") },
                    trailingIcon = {
                        IconButton(onClick = vm::submitLog, enabled = inputText.isNotBlank()) {
                            Icon(Icons.Default.Send, contentDescription = "Enviar", tint = FatumColors.Accent)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = FatumColors.Divider,
                        focusedBorderColor = FatumColors.Accent,
                        focusedTextColor = FatumColors.Primary,
                        unfocusedTextColor = FatumColors.Primary
                    ),
                    singleLine = false,
                    maxLines = 4
                )
            }

            // ── Time capsule (RF-1.5) ─────────────────────────────────────
            item {
                AnimatedVisibility(visible = showTimeCapsule && timeCapsule.isNotEmpty()) {
                    FatumCard {
                        SectionHeader("Un día como hoy")
                        timeCapsule.groupBy { it.dateString }.forEach { (date, entries) ->
                            Text(
                                text = date,
                                style = MaterialTheme.typography.labelSmall,
                                color = FatumColors.Accent,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            entries.forEach { log ->
                                Text(
                                    text = log.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FatumColors.PrimaryVariant,
                                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                )
                            }
                            HorizontalDivider(color = FatumColors.Divider)
                        }
                    }
                }
            }

            // ── Timeline (RF-1.4) ─────────────────────────────────────────
            grouped.forEach { (date, dayLogs) ->
                item {
                    SectionHeader(formatDateHeader(date))
                }
                items(dayLogs, key = { it.id }) { log ->
                    LogItem(
                        content = log.content,
                        timeLabel = formatTime(log.timestamp),
                        tags = log.tags.split(",").filter { it.isNotBlank() },
                        onTagClick = { vm.setTagFilter(it) },
                        onEdit = {
                            editTarget = log
                            editText = log.content
                        },
                        onDelete = { vm.deleteLog(log) }
                    )
                }
            }
        }
    }

    // ── Edit dialog (RF-1.6) ──────────────────────────────────────────────
    if (editTarget != null) {
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("Editar log") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val newTags = Regex("#(\\w+)").findAll(editText)
                        .map { it.groupValues[1].lowercase() }.joinToString(",")
                    vm.updateLog(editTarget!!.copy(content = editText, tags = newTags))
                    editTarget = null
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { editTarget = null }) { Text("Cancelar") }
            }
        )
    }
}

private fun formatDateHeader(dateString: String): String {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val yesterday = run {
        val cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -1)
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }
    return when (dateString) {
        today     -> "Hoy"
        yesterday -> "Ayer"
        else      -> try {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
            SimpleDateFormat("d 'de' MMMM", Locale("es")).format(d!!)
        } catch (_: Exception) { dateString }
    }
}

private fun formatTime(epochMs: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMs))
