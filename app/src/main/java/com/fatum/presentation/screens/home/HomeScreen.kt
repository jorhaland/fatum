package com.fatum.presentation.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatum.data.db.entities.LogEntity
import com.fatum.presentation.components.EmptyState
import com.fatum.presentation.components.FatumCard
import com.fatum.presentation.components.FatumChip
import com.fatum.presentation.components.HeatMapGrid
import com.fatum.presentation.theme.FatumColors
import com.fatum.presentation.viewmodels.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val logs        by vm.logs.collectAsStateWithLifecycle()
    val inputText   by vm.inputText.collectAsStateWithLifecycle()
    val activeTag   by vm.activeTag.collectAsStateWithLifecycle()
    val heatMap     by vm.heatMap.collectAsStateWithLifecycle()
    val timeCapsule by vm.timeCapsule.collectAsStateWithLifecycle()

    var showTimeCapsule by remember { mutableStateOf(false) }
    var editTarget      by remember { mutableStateOf<LogEntity?>(null) }
    var editText        by remember { mutableStateOf("") }

    val grouped = remember(logs) { logs.groupBy { it.dateString } }
    val allTags = remember(logs) {
        logs.flatMap { it.tags.split(",").filter { t -> t.isNotBlank() } }.distinct()
    }

    Scaffold(
        containerColor = FatumColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text("fatum", style = MaterialTheme.typography.headlineLarge, color = FatumColors.Green)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FatumColors.Background),
                actions = {
                    if (timeCapsule.isNotEmpty()) {
                        IconButton(onClick = { showTimeCapsule = !showTimeCapsule }) {
                            Icon(
                                Icons.Outlined.History,
                                contentDescription = "Un día como hoy",
                                tint = if (showTimeCapsule) FatumColors.Green else FatumColors.TextMuted
                            )
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = FatumColors.TextMuted)
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

            // ── Quick-add ────────────────────────────────────────────────────
            item {
                QuickAddInput(
                    value        = inputText,
                    onValueChange = vm::onInputChange,
                    onSubmit     = vm::submitLog
                )
            }

            // ── Tag filter chips ─────────────────────────────────────────────
            if (allTags.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FatumChip("Todo", selected = activeTag == null, onClick = { vm.setTagFilter(null) })
                        }
                        items(allTags) { tag ->
                            FatumChip(
                                "#$tag",
                                selected = activeTag == tag,
                                onClick  = { vm.setTagFilter(if (activeTag == tag) null else tag) }
                            )
                        }
                    }
                }
            }

            // ── Heat-map ─────────────────────────────────────────────────────
            item {
                FatumCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Actividad", style = MaterialTheme.typography.titleSmall, color = FatumColors.TextSecondary)
                        Text(
                            "${heatMap.values.sum()} acciones este año",
                            style = MaterialTheme.typography.labelSmall,
                            color = FatumColors.TextMuted
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    HeatMapGrid(data = heatMap, onDayClick = { /* future: jump to date */ })
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Menos", style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted)
                        Spacer(Modifier.width(4.dp))
                        listOf(
                            FatumColors.Heat0, FatumColors.Heat1, FatumColors.Heat2,
                            FatumColors.Heat3, FatumColors.Heat4
                        ).forEach { c ->
                            Box(
                                Modifier.padding(horizontal = 1.dp).size(10.dp)
                                    .clip(RoundedCornerShape(2.dp)).background(c)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("Más", style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted)
                    }
                }
            }

            // ── Time capsule ─────────────────────────────────────────────────
            item {
                AnimatedVisibility(visible = showTimeCapsule && timeCapsule.isNotEmpty()) {
                    FatumCard(modifier = Modifier.fillMaxWidth(), highlight = true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🕰️", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Un día como hoy",
                                style = MaterialTheme.typography.titleMedium,
                                color = FatumColors.Green
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        timeCapsule.groupBy { it.dateString }.entries.take(3).forEach { (date, entries) ->
                            Text(
                                formatCapsuleDate(date),
                                style = MaterialTheme.typography.labelMedium,
                                color = FatumColors.TextMuted,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            entries.take(2).forEach { log ->
                                Text(
                                    log.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FatumColors.TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                )
                            }
                            HorizontalDivider(
                                color = FatumColors.DividerLine,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // ── Timeline ─────────────────────────────────────────────────────
            if (logs.isEmpty()) {
                item {
                    EmptyState(
                        emoji    = "📝",
                        title    = "Sin registros todavía",
                        subtitle = "Escribe lo que está pasando arriba.\nUsa #etiqueta para organizar tus entradas."
                    )
                }
            } else {
                grouped.forEach { (date, dayLogs) ->
                    item(key = "hdr_$date") {
                        Text(
                            formatDateHeader(date),
                            style = MaterialTheme.typography.labelLarge,
                            color = FatumColors.TextMuted,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }
                    items(dayLogs, key = { "log_${it.id}" }) { log ->
                        LogCard(
                            log       = log,
                            onTagClick = vm::setTagFilter,
                            onEdit    = { editTarget = log; editText = log.content },
                            onDelete  = { vm.deleteLog(log) }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // ── Edit dialog ───────────────────────────────────────────────────────────
    if (editTarget != null) {
        AlertDialog(
            onDismissRequest = { editTarget = null },
            containerColor   = FatumColors.Surface,
            shape            = RoundedCornerShape(20.dp),
            title = { Text("Editar registro", color = FatumColors.TextPrimary) },
            text = {
                OutlinedTextField(
                    value         = editText,
                    onValueChange = { editText = it },
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = fatumFieldColors(),
                    minLines      = 2
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newTags = Regex("#(\\w+)").findAll(editText)
                            .map { it.groupValues[1].lowercase() }.joinToString(",")
                        vm.updateLog(editTarget!!.copy(content = editText.trim(), tags = newTags))
                        editTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FatumColors.Green, contentColor = Color(0xFF0F1117)),
                    shape  = RoundedCornerShape(12.dp)
                ) { Text("Guardar", style = MaterialTheme.typography.titleSmall) }
            },
            dismissButton = {
                TextButton(onClick = { editTarget = null }) {
                    Text("Cancelar", color = FatumColors.TextSecondary)
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick-add input
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun QuickAddInput(value: String, onValueChange: (String) -> Unit, onSubmit: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = FatumColors.Surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (value.isNotBlank()) FatumColors.GreenBorder else FatumColors.Border
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value         = value,
                onValueChange = onValueChange,
                modifier      = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "¿Qué está pasando?  #etiqueta",
                        color = FatumColors.TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor    = Color.Transparent,
                    unfocusedContainerColor  = Color.Transparent,
                    focusedIndicatorColor    = Color.Transparent,
                    unfocusedIndicatorColor  = Color.Transparent,
                    focusedTextColor         = FatumColors.TextPrimary,
                    unfocusedTextColor       = FatumColors.TextPrimary,
                    cursorColor              = FatumColors.Green
                ),
                singleLine = false,
                maxLines   = 5,
                textStyle  = MaterialTheme.typography.bodyLarge
            )
            AnimatedVisibility(
                visible = value.isNotBlank(),
                enter   = scaleIn() + fadeIn(),
                exit    = scaleOut() + fadeOut()
            ) {
                IconButton(
                    onClick  = onSubmit,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(FatumColors.Green)
                        .size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Guardar",
                        tint = Color(0xFF0F1117),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Log card with swipe-to-delete
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogCard(
    log: LogEntity,
    onTagClick: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val tags = remember(log.tags) { log.tags.split(",").filter { it.isNotBlank() } }

    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        }
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(FatumColors.Error.copy(alpha = 0.12f)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Outlined.Delete, null,
                    tint = FatumColors.Error,
                    modifier = Modifier.padding(end = 20.dp)
                )
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        FatumCard(
            modifier = Modifier.fillMaxWidth(),
            onClick  = { expanded = !expanded }
        ) {
            Text(
                text      = log.content,
                style     = MaterialTheme.typography.bodyMedium,
                color     = FatumColors.TextPrimary,
                maxLines  = if (expanded) Int.MAX_VALUE else 4,
                overflow  = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    formatTime(log.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = FatumColors.TextMuted
                )
                tags.forEach { tag ->
                    Text(
                        "#$tag",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = FatumColors.Green,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(FatumColors.GreenSurface)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .clickable { onTagClick(tag) }
                    )
                }
                Spacer(Modifier.weight(1f))
                AnimatedVisibility(visible = expanded) {
                    Icon(
                        Icons.Outlined.Edit, null,
                        tint     = FatumColors.TextMuted,
                        modifier = Modifier.size(16.dp).clickable { onEdit() }
                    )
                }
            }
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────
@Composable
fun fatumFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = FatumColors.Green,
    unfocusedBorderColor    = FatumColors.Border,
    focusedTextColor        = FatumColors.TextPrimary,
    unfocusedTextColor      = FatumColors.TextPrimary,
    cursorColor             = FatumColors.Green,
    focusedLabelColor       = FatumColors.Green,
    unfocusedLabelColor     = FatumColors.TextMuted,
    focusedContainerColor   = Color.Transparent,
    unfocusedContainerColor = Color.Transparent
)

private fun formatDateHeader(ds: String): String {
    val today     = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val cal       = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    return when (ds) {
        today     -> "Hoy"
        yesterday -> "Ayer"
        else -> try {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(ds)!!
            SimpleDateFormat("d 'de' MMMM", Locale("es")).format(d)
        } catch (_: Exception) { ds }
    }
}

private fun formatTime(ms: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))

private fun formatCapsuleDate(ds: String): String = try {
    val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(ds)!!
    SimpleDateFormat("d MMM yyyy", Locale("es")).format(d)
} catch (_: Exception) { ds }
