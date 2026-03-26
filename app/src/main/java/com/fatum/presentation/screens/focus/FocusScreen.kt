package com.fatum.presentation.screens.focus

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatum.presentation.components.FatumCard
import com.fatum.presentation.components.SectionHeader
import com.fatum.presentation.theme.FatumColors
import com.fatum.presentation.viewmodels.FocusViewModel
import com.fatum.services.AppBlockerOverlayService
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(vm: FocusViewModel = hiltViewModel()) {
    val state          by vm.state.collectAsStateWithLifecycle()
    val remainingMs    by vm.remainingMs.collectAsStateWithLifecycle()
    val durationMin    by vm.durationMinutes.collectAsStateWithLifecycle()
    val sessions       by vm.recentSessions.collectAsStateWithLifecycle()
    val context        = LocalContext.current

    // ── Tick engine: fires every second while RUNNING ──────────────────────
    LaunchedEffect(state) {
        while (state == FocusViewModel.TimerState.RUNNING) {
            delay(1_000)
            vm.tick(1_000)
        }
    }

    // ── Start/stop the overlay blocker service (RF-5.2) ───────────────────
    LaunchedEffect(state) {
        when (state) {
            FocusViewModel.TimerState.RUNNING  -> startBlockerService(context)
            FocusViewModel.TimerState.IDLE,
            FocusViewModel.TimerState.FINISHED -> stopBlockerService(context)
            else                               -> Unit
        }
    }

    val totalMs = durationMin * 60 * 1000L
    val sweepAngle = if (totalMs > 0) (remainingMs.toFloat() / totalMs) * 360f else 0f

    Scaffold(
        containerColor = FatumColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Deep Work") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FatumColors.Background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Circular timer (RF-5.1) ─────────────────────────────────
            Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Track
                    drawArc(
                        color = FatumColors.SurfaceVariant,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                        size = Size(size.width - 20.dp.toPx(), size.height - 20.dp.toPx()),
                        topLeft = androidx.compose.ui.geometry.Offset(10.dp.toPx(), 10.dp.toPx())
                    )
                    // Progress arc
                    drawArc(
                        color = FatumColors.Accent,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                        size = Size(size.width - 20.dp.toPx(), size.height - 20.dp.toPx()),
                        topLeft = androidx.compose.ui.geometry.Offset(10.dp.toPx(), 10.dp.toPx())
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatTime(remainingMs),
                        style = MaterialTheme.typography.displayLarge,
                        color = FatumColors.Primary
                    )
                    Text(
                        text = when (state) {
                            FocusViewModel.TimerState.RUNNING  -> "EN SESIÓN"
                            FocusViewModel.TimerState.FINISHED -> "COMPLETADO ✓"
                            else                               -> "LISTO"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = FatumColors.PrimaryVariant
                    )
                }
            }

            // ── Duration picker (when idle) ─────────────────────────────
            if (state == FocusViewModel.TimerState.IDLE) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(15, 25, 45, 60, 90).forEach { min ->
                        FilterChip(
                            selected = durationMin == min,
                            onClick = { vm.setDuration(min) },
                            label = { Text("${min}m") }
                        )
                    }
                }
            }

            // ── Controls ─────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                when (state) {
                    FocusViewModel.TimerState.IDLE, FocusViewModel.TimerState.FINISHED -> {
                        Button(
                            onClick = { vm.startSession() },
                            colors = ButtonDefaults.buttonColors(containerColor = FatumColors.Accent)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = FatumColors.Background)
                            Spacer(Modifier.width(4.dp))
                            Text("Iniciar sesión", color = FatumColors.Background)
                        }
                    }
                    FocusViewModel.TimerState.RUNNING -> {
                        OutlinedButton(
                            onClick = { vm.interruptSession() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FatumColors.Error)
                        ) {
                            Icon(Icons.Default.Stop, null, tint = FatumColors.Error)
                            Spacer(Modifier.width(4.dp))
                            Text("Interrumpir sesión")
                        }
                    }
                    else -> Unit
                }
            }

            // ── Session history ───────────────────────────────────────────
            SectionHeader("Sesiones recientes")
            sessions.take(5).forEach { s ->
                FatumCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${s.durationMinutes} min", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (s.status == "COMPLETED") "✓ Completada" else "✗ Interrumpida",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (s.status == "COMPLETED") FatumColors.AccentSecondary else FatumColors.Error
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}

private fun startBlockerService(context: Context) {
    val intent = Intent(context, AppBlockerOverlayService::class.java).apply {
        action = AppBlockerOverlayService.ACTION_START
    }
    context.startForegroundService(intent)
}

private fun stopBlockerService(context: Context) {
    val intent = Intent(context, AppBlockerOverlayService::class.java).apply {
        action = AppBlockerOverlayService.ACTION_STOP
    }
    context.startService(intent)
}
