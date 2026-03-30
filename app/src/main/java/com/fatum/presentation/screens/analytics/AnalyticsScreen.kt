package com.fatum.presentation.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatum.data.db.entities.DailyMoodEntity
import com.fatum.data.db.entities.HabitEntity
import com.fatum.presentation.components.*
import com.fatum.presentation.theme.FatumColors
import com.fatum.presentation.viewmodels.AnalyticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(vm: AnalyticsViewModel = hiltViewModel()) {
    val moods         by vm.allMoods.collectAsStateWithLifecycle()
    val habits        by vm.activeHabits.collectAsStateWithLifecycle()
    val goals         by vm.allGoals.collectAsStateWithLifecycle()
    val sessions      by vm.recentSessions.collectAsStateWithLifecycle()
    val weeklySummary by vm.weeklySummary.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.generateWeeklySummary() }

    Scaffold(
        containerColor = FatumColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Análisis", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FatumColors.Background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Weekly summary ──────────────────────────────────────────────
            weeklySummary?.let { s ->
                item {
                    SectionHeader("Resumen semanal")
                    Spacer(Modifier.height(6.dp))
                    FatumCard(modifier = Modifier.fillMaxWidth(), highlight = true) {
                        Text(s.weekRange, style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted)
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatTile("humor medio", "%.1f".format(s.avgMood), Modifier.weight(1f))
                            StatTile("deep work", "${s.deepWorkMinutes}m", Modifier.weight(1f))
                            StatTile("mejor racha", "${s.bestStreak.second}d", Modifier.weight(1f))
                        }
                        if (s.goalProgress.isNotEmpty()) {
                            Spacer(Modifier.height(14.dp))
                            Text("Progreso de metas", style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted)
                            Spacer(Modifier.height(8.dp))
                            s.goalProgress.forEach { (title, pct) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(title, style = MaterialTheme.typography.bodySmall, color = FatumColors.TextSecondary, modifier = Modifier.weight(1f), maxLines = 1)
                                    FatumProgressBar(pct / 100f, Modifier.weight(1.5f))
                                    Text("${pct.toInt()}%", style = MaterialTheme.typography.labelSmall, color = FatumColors.Green, modifier = Modifier.width(32.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ── Mood chart ─────────────────────────────────────────────────
            if (moods.size >= 3) {
                item {
                    SectionHeader("Humor — últimos 30 días")
                    Spacer(Modifier.height(6.dp))
                    FatumCard(modifier = Modifier.fillMaxWidth()) {
                        MoodLineChart(moods = moods.takeLast(30))
                        Spacer(Modifier.height(4.dp))
                        val avg = moods.takeLast(7).map { it.score }.average()
                        Text(
                            "Media 7 días: ${"%.1f".format(avg)} / 10",
                            style = MaterialTheme.typography.labelSmall,
                            color = FatumColors.TextMuted
                        )
                    }
                }
            }

            // ── Habit streaks ───────────────────────────────────────────────
            if (habits.isNotEmpty()) {
                item { SectionHeader("Rachas de hábitos") }
                items(habits.sortedByDescending { it.currentStreak }) { habit ->
                    HabitStreakRow(habit)
                }
            }

            // ── Focus total ─────────────────────────────────────────────────
            if (sessions.isNotEmpty()) {
                item {
                    val total = sessions.filter { it.status == "COMPLETED" }.sumOf { it.durationMinutes }
                    val rate  = if (sessions.isEmpty()) 0 else (sessions.count { it.status == "COMPLETED" } * 100 / sessions.size)
                    SectionHeader("Deep Work")
                    Spacer(Modifier.height(6.dp))
                    FatumCard(modifier = Modifier.fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatTile("minutos totales", "$total", Modifier.weight(1f))
                            StatTile("tasa de éxito", "$rate%", Modifier.weight(1f))
                            StatTile("sesiones", "${sessions.size}", Modifier.weight(1f))
                        }
                    }
                }
            }

            if (moods.isEmpty() && habits.isEmpty() && sessions.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "📊",
                        title = "Sin datos todavía",
                        subtitle = "Empieza a usar la app y aquí verás tus estadísticas"
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun MoodLineChart(moods: List<DailyMoodEntity>) {
    Canvas(
        modifier = Modifier.fillMaxWidth().height(100.dp)
    ) {
        if (moods.size < 2) return@Canvas
        val w    = size.width
        val h    = size.height
        val step = w / (moods.size - 1)

        // Fill path
        val fillPath = Path()
        moods.forEachIndexed { i, m ->
            val x = i * step
            val y = h - (m.score / 10f) * h * 0.9f - h * 0.05f
            if (i == 0) fillPath.moveTo(x, y) else fillPath.lineTo(x, y)
        }
        fillPath.lineTo((moods.size - 1) * step, h)
        fillPath.lineTo(0f, h)
        fillPath.close()

        drawPath(fillPath, color = FatumColors.Green.copy(alpha = 0.08f))

        // Line path
        val linePath = Path()
        moods.forEachIndexed { i, m ->
            val x = i * step
            val y = h - (m.score / 10f) * h * 0.9f - h * 0.05f
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }
        drawPath(linePath, color = FatumColors.Green, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

        // Dots
        moods.forEachIndexed { i, m ->
            val x = i * step
            val y = h - (m.score / 10f) * h * 0.9f - h * 0.05f
            drawCircle(FatumColors.GreenDim, radius = 3.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
private fun HabitStreakRow(habit: HabitEntity) {
    FatumCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.titleSmall, color = FatumColors.TextPrimary)
                Text(
                    "Récord: ${habit.maxStreak} días",
                    style = MaterialTheme.typography.labelSmall,
                    color = FatumColors.TextMuted
                )
            }
            StreakBadge(streak = habit.currentStreak)
        }
    }
}
