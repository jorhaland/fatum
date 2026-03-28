package com.fatum.presentation.screens.focus

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatum.data.db.entities.FocusSessionEntity
import com.fatum.presentation.components.*
import com.fatum.services.AppBlockerOverlayService
import com.fatum.presentation.theme.FatumColors
import com.fatum.presentation.viewmodels.FocusViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(vm: FocusViewModel = hiltViewModel()) {
    val state       by vm.state.collectAsStateWithLifecycle()
    val remainingMs by vm.remainingMs.collectAsStateWithLifecycle()
    val durationMin by vm.durationMinutes.collectAsStateWithLifecycle()
    val sessions    by vm.recentSessions.collectAsStateWithLifecycle()
    val context     = LocalContext.current

    // Timer tick
    LaunchedEffect(state) {
        while (state == FocusViewModel.TimerState.RUNNING) {
            delay(1_000)
            vm.tick(1_000)
        }
    }

    // Service lifecycle
    LaunchedEffect(state) {
        when (state) {
            FocusViewModel.TimerState.RUNNING  -> startBlockerService(context)
            FocusViewModel.TimerState.IDLE,
            FocusViewModel.TimerState.FINISHED -> stopBlockerService(context)
            else -> Unit
        }
    }

    val totalMs = durationMin * 60 * 1000L
    val sweepAngle = if (totalMs > 0) (remainingMs.toFloat() / totalMs) * 360f else 0f
    val isRunning = state == FocusViewModel.TimerState.RUNNING

    Scaffold(
        containerColor = FatumColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Enfoque", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FatumColors.Background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Circular timer ──────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 14.dp.toPx()
                        val inset  = stroke / 2
                        val rect   = Size(size.width - stroke, size.height - stroke)
                        val tl     = Offset(inset, inset)
                        // Track
                        drawArc(color = FatumColors.SurfaceVariant, startAngle = -90f, sweepAngle = 360f,
                            useCenter = false, style = Stroke(stroke, cap = StrokeCap.Round), size = rect, topLeft = tl)
                        // Progress arc
                        if (sweepAngle > 0f) {
                            drawArc(
                                brush = Brush.sweepGradient(listOf(FatumColors.GreenDim, FatumColors.Green)),
                                startAngle = -90f, sweepAngle = sweepAngle, useCenter = false,
                                style = Stroke(stroke, cap = StrokeCap.Round), size = rect, topLeft = tl
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatMs(remainingMs),
                            style = MaterialTheme.typography.displayLarge,
                            color = FatumColors.TextPrimary
                        )
                        Text(
                            text = when (state) {
                                FocusViewModel.TimerState.RUNNING  -> "EN SESIÓN"
                                FocusViewModel.TimerState.FINISHED -> "¡COMPLETADO!"
                                else                               -> "${durationMin} MIN"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isRunning) FatumColors.Green else FatumColors.TextMuted
                        )
                    }
                }
            }

            // ── Duration chips (only when idle) ─────────────────────────────
            item {
                AnimatedVisibility(visible = !isRunning) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 25, 45, 60, 90).forEach { min ->
                            FatumChip(
                                label = "${min}m",
                                selected = durationMin == min,
                                onClick  = { vm.setDuration(min) }
                            )
                        }
                    }
                }
            }

            // ── Controls ─────────────────────────────────────────────────────
            item {
                when (state) {
                    FocusViewModel.TimerState.IDLE, FocusViewModel.TimerState.FINISHED -> {
                        FatumButton(
                            text = if (state == FocusViewModel.TimerState.FINISHED) "Nueva sesión" else "Iniciar sesión",
                            onClick = { vm.startSession() },
                            modifier = Modifier.fillMaxWidth(0.6f),
                            icon = { Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF0F1117), modifier = Modifier.size(20.dp)) }
                        )
                    }
                    FocusViewModel.TimerState.RUNNING -> {
                        OutlinedButton(
                            onClick = { vm.interruptSession() },
                            modifier = Modifier.fillMaxWidth(0.7f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, FatumColors.Error),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FatumColors.Error)
                        ) {
                            Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Interrumpir sesión", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    else -> Unit
                }
            }

            // ── App block notice ──────────────────────────────────────────────
            item {
                AnimatedVisibility(visible = isRunning) {
                    FatumCard(modifier = Modifier.fillMaxWidth(), highlight = true) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("🔒", style = MaterialTheme.typography.titleLarge)
                            Column {
                                Text("Redes sociales bloqueadas", style = MaterialTheme.typography.titleSmall, color = FatumColors.Green)
                                Text("Instagram, X y TikTok están restringidos durante esta sesión.", style = MaterialTheme.typography.bodySmall, color = FatumColors.TextMuted)
                            }
                        }
                    }
                }
            }

            // ── Stats ─────────────────────────────────────────────────────────
            if (sessions.isNotEmpty()) {
                item {
                    val totalMin    = sessions.filter { it.status == "COMPLETED" }.sumOf { it.durationMinutes }
                    val completed   = sessions.count { it.status == "COMPLETED" }
                    val interrupted = sessions.count { it.status == "FAILED_INTERRUPTED" }
                    SectionHeader("Esta semana")
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile("minutos", "$totalMin", Modifier.weight(1f))
                        StatTile("completadas", "$completed", Modifier.weight(1f))
                        StatTile("interrumpidas", "$interrupted", Modifier.weight(1f))
                    }
                }
                item { SectionHeader("Historial") }
                items(sessions.take(7)) { session ->
                    SessionRow(session)
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun SessionRow(session: FocusSessionEntity) {
    val completed = session.status == "COMPLETED"
    FatumCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (completed) "✅" else "⚠️", style = MaterialTheme.typography.bodyLarge)
                Column {
                    Text("${session.durationMinutes} minutos", style = MaterialTheme.typography.titleSmall, color = FatumColors.TextPrimary)
                    Text(
                        SimpleDateFormat("d MMM, HH:mm", Locale("es")).format(Date(session.startTimestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = FatumColors.TextMuted
                    )
                }
            }
            Text(
                text = if (completed) "Completada" else "Interrumpida",
                style = MaterialTheme.typography.labelMedium,
                color = if (completed) FatumColors.Green else FatumColors.Error
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val s = ms / 1000
    return "%02d:%02d".format(s / 60, s % 60)
}

private fun startBlockerService(context: Context) {
    runCatching {
        context.startForegroundService(
            Intent(context, AppBlockerOverlayService::class.java).apply { action = AppBlockerOverlayService.ACTION_START }
        )
    }
}

private fun stopBlockerService(context: Context) {
    runCatching {
        context.startService(
            Intent(context, AppBlockerOverlayService::class.java).apply { action = AppBlockerOverlayService.ACTION_STOP }
        )
    }
}
