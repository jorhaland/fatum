package com.fatum.presentation.screens.focus

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatum.presentation.components.*
import com.fatum.services.FocusTimerCore
import com.fatum.services.FocusTimerService
import com.fatum.presentation.theme.FatumColors
import com.fatum.presentation.viewmodels.FocusViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(onBack: () -> Unit, vm: FocusViewModel = hiltViewModel()) {
    val state    by vm.state.collectAsStateWithLifecycle()
    val rem      by vm.remainingMs.collectAsStateWithLifecycle()
    val dur      by vm.durationMinutes.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val context  = LocalContext.current

    val usageLauncher   = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    val notifLauncher   = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val totalMs = dur * 60_000L
    val sweep = if (totalMs > 0) ((rem.toFloat() / totalMs) * 360f).coerceIn(0f, 360f) else 0f
    val running = state == FocusTimerCore.TimerState.RUNNING

    Scaffold(
        containerColor = FatumColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Modo Enfoque", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = FatumColors.TextMuted) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FatumColors.Background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Box(Modifier.padding(vertical = 16.dp).size(240.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 14.dp.toPx()
                        val inset  = stroke / 2
                        val sz     = Size(size.width - stroke, size.height - stroke)
                        drawArc(color = FatumColors.SurfaceVariant, startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = Offset(inset, inset), size = sz, style = Stroke(stroke, cap = StrokeCap.Round))
                        if (sweep > 0f && !sweep.isNaN()) {
                            drawArc(brush = Brush.sweepGradient(listOf(FatumColors.GreenDim, FatumColors.Green)), startAngle = -90f, sweepAngle = sweep, useCenter = false, topLeft = Offset(inset, inset), size = sz, style = Stroke(stroke, cap = StrokeCap.Round))
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(fmtMs(rem), style = MaterialTheme.typography.displayLarge, color = FatumColors.TextPrimary)
                        Text(when(state) {
                            FocusTimerCore.TimerState.RUNNING  -> "EN SESIÓN"
                            FocusTimerCore.TimerState.FINISHED -> "¡COMPLETADO!"
                            else -> "${dur}m"
                        }, style = MaterialTheme.typography.labelLarge, color = if (running) FatumColors.Green else FatumColors.TextMuted)
                    }
                }
            }

            item {
                if (!running) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 25, 45, 60, 90).forEach { m -> FatumChip("${m}m", dur == m, { vm.setDuration(m) }) }
                }
            }

            item {
                when (state) {
                    FocusTimerCore.TimerState.IDLE ->
                        FatumButton("Iniciar", onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                            if (!hasAccessibilityPermission(context)) {
                                usageLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                android.widget.Toast.makeText(context, "Activa FATUM en Accesibilidad", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                vm.startSession()
                                context.startForegroundService(Intent(context, FocusTimerService::class.java))
                            }
                        }, modifier = Modifier.fillMaxWidth(.6f), icon = { Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF0F1117), modifier = Modifier.size(20.dp)) })

                    FocusTimerCore.TimerState.FINISHED ->
                        FatumButton("Guardar y Nueva sesión", onClick = { vm.finishSessionData() }, modifier = Modifier.fillMaxWidth(.6f))

                    FocusTimerCore.TimerState.RUNNING ->
                        OutlinedButton(onClick = {
                            vm.interrupt()
                            context.startService(Intent(context, FocusTimerService::class.java).apply { action = "STOP" })
                        }, modifier = Modifier.fillMaxWidth(.7f).height(48.dp), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, FatumColors.Error), colors = ButtonDefaults.outlinedButtonColors(contentColor = FatumColors.Error)) {
                            Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Interrumpir sesión")
                        }
                }
            }

            if (running) {
                item {
                    FatumCard(Modifier.fillMaxWidth(), highlight = true) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("🔒", style = MaterialTheme.typography.titleLarge)
                            Column {
                                Text("Redes sociales bloqueadas", style = MaterialTheme.typography.titleSmall, color = FatumColors.Green)
                                Text("Instagram, X y TikTok restringidos.", style = MaterialTheme.typography.bodySmall, color = FatumColors.TextMuted)
                            }
                        }
                    }
                }
            }

            if (sessions.isNotEmpty()) {
                item {
                    val total = sessions.filter { it.status == "COMPLETED" }.sumOf { it.durationMinutes }
                    SectionHeader("Esta semana")
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("minutos", "$total", Modifier.weight(1f))
                        StatTile("completadas", "${sessions.count { it.status == "COMPLETED" }}", Modifier.weight(1f))
                        StatTile("interrumpidas", "${sessions.count { it.status != "COMPLETED" }}", Modifier.weight(1f))
                    }
                }
                items(sessions.take(7)) { s ->
                    FatumCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(if (s.status == "COMPLETED") "✅" else "⚠️", style = MaterialTheme.typography.bodyLarge)
                                Column {
                                    Text("${s.durationMinutes} min", style = MaterialTheme.typography.titleSmall, color = FatumColors.TextPrimary)
                                    Text(SimpleDateFormat("d MMM, HH:mm", Locale("es")).format(Date(s.startTimestamp)), style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted)
                                }
                            }
                            Text(if (s.status == "COMPLETED") "Completada" else "Interrumpida", style = MaterialTheme.typography.labelMedium, color = if (s.status == "COMPLETED") FatumColors.Green else FatumColors.Error)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

private fun fmtMs(ms: Long) = "%02d:%02d".format(ms / 60_000, (ms % 60_000) / 1_000)

private fun hasAccessibilityPermission(context: Context): Boolean {
    val componentName = ComponentName(context, com.fatum.services.AppBlockerAccessibilityService::class.java)
    val enabledServices = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    return enabledServices?.contains(componentName.flattenToString()) == true
}