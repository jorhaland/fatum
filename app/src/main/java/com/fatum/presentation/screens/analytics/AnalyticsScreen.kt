package com.fatum.presentation.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    val moods        by vm.allMoods.collectAsStateWithLifecycle()
    val habits       by vm.activeHabits.collectAsStateWithLifecycle()
    val goals        by vm.allGoals.collectAsStateWithLifecycle()
    val sessions     by vm.recentSessions.collectAsStateWithLifecycle()
    val selectedHabit by vm.correlationHabitId.collectAsStateWithLifecycle()
    val weeklySummary by vm.weeklySummary.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.generateWeeklySummary() }

    Scaffold(
        containerColor = FatumColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Análisis") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FatumColors.Background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Weekly summary (RF-6.3) ─────────────────────────────────
            weeklySummary?.let { summary ->
                item {
                    SectionHeader("Resumen semanal")
                    FatumCard(modifier = Modifier.fillMaxWidth()) {
                        Text(summary.weekRange, style = MaterialTheme.typography.labelSmall, color = FatumColors.PrimaryVariant)
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatBlock(label = "Humor medio", value = "%.1f".format(summary.avgMood))
                            StatBlock(label = "Deep Work", value = "${summary.deepWorkMinutes}m")
                            StatBlock(label = "Mejor racha", value = "${summary.bestStreak.second}d")
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Avance de metas", style = MaterialTheme.typography.labelSmall, color = FatumColors.PrimaryVariant)
                        summary.goalProgress.forEach { (title, pct) ->
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                LinearProgressIndicator(
                                    progress = { pct / 100f },
                                    modifier = Modifier.weight(1f),
                                    color = FatumColors.AccentSecondary,
                                    trackColor = FatumColors.SurfaceVariant
                                )
                                Text("${pct.toInt()}%", style = MaterialTheme.typography.labelSmall, color = FatumColors.AccentSecondary)
                            }
                        }
                    }
                }
            }

            // ── Mood timeline graph ─────────────────────────────────────
            if (moods.isNotEmpty()) {
                item {
                    SectionHeader("Evolución del humor")
                    FatumCard(modifier = Modifier.fillMaxWidth()) {
                        MoodLineChart(moods = moods.takeLast(30))
                    }
                }
            }

            // ── Correlation chart (RF-6.2) ───────────────────────────────
            item {
                SectionHeader("Correlación hábito–humor")
                FatumCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Selecciona un hábito para ver su correlación con tu humor:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    habits.forEach { habit ->
                        FilterChip(
                            selected = selectedHabit == habit.id,
                            onClick = { vm.selectCorrelationHabit(habit.id) },
                            label = { Text(habit.name) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                    if (selectedHabit != null) {
                        Spacer(Modifier.height(12.dp))
                        // Display correlation insight
                        val habitName = habits.find { it.id == selectedHabit }?.name ?: ""
                        Text(
                            text = "Mostrando correlación para: $habitName",
                            style = MaterialTheme.typography.labelSmall,
                            color = FatumColors.Accent
                        )
                        // Visual placeholder for the scatter plot
                        CorrelationPlaceholder()
                    }
                }
            }

            // ── Habit streaks leaderboard ───────────────────────────────
            if (habits.isNotEmpty()) {
                item { SectionHeader("Rachas activas") }
                items(habits.sortedByDescending { it.currentStreak }) { habit ->
                    HabitStreakRow(habit)
                }
            }

            // ── Focus session stats ─────────────────────────────────────
            if (sessions.isNotEmpty()) {
                item {
                    SectionHeader("Resumen Deep Work")
                    val completed = sessions.count { it.status == "COMPLETED" }
                    val failed    = sessions.count { it.status == "FAILED_INTERRUPTED" }
                    val totalMin  = sessions.filter { it.status == "COMPLETED" }.sumOf { it.durationMinutes }
                    FatumCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatBlock(label = "Completadas", value = "$completed")
                            StatBlock(label = "Interrumpidas", value = "$failed")
                            StatBlock(label = "Total (min)", value = "$totalMin")
                        }
                    }
                }
            }
        }
    }
}

// ── Mood line chart (last 30 days) ────────────────────────────────────────
@Composable
private fun MoodLineChart(moods: List<DailyMoodEntity>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        if (moods.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val step = w / (moods.size - 1)
        val path = Path()

        moods.forEachIndexed { i, mood ->
            val x = i * step
            val y = h - (mood.score / 10f) * h
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path = path, color = FatumColors.Accent, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

        // Draw dots
        moods.forEachIndexed { i, mood ->
            val x = i * step
            val y = h - (mood.score / 10f) * h
            drawCircle(color = FatumColors.AccentSecondary, radius = 4.dp.toPx(), center = Offset(x, y))
        }
    }
    // X labels
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        moods.firstOrNull()?.let { Text(it.dateString.takeLast(5), style = MaterialTheme.typography.labelSmall, color = FatumColors.PrimaryVariant) }
        moods.lastOrNull()?.let { Text(it.dateString.takeLast(5), style = MaterialTheme.typography.labelSmall, color = FatumColors.PrimaryVariant) }
    }
}

// ── Correlation placeholder (RF-6.2 visual) ───────────────────────────────
@Composable
private fun CorrelationPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(FatumColors.SurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Los datos de correlación aparecerán aquí conforme registres más días.",
            style = MaterialTheme.typography.bodyMedium,
            color = FatumColors.PrimaryVariant,
            modifier = Modifier.padding(12.dp)
        )
    }
}

// ── Habit streak row ───────────────────────────────────────────────────────
@Composable
private fun HabitStreakRow(habit: HabitEntity) {
    FatumCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium)
                Text("Mejor racha: ${habit.maxStreak} días", style = MaterialTheme.typography.labelSmall, color = FatumColors.PrimaryVariant)
            }
            StreakBadge(streak = habit.currentStreak)
        }
    }
}

// ── Generic stat block ─────────────────────────────────────────────────────
@Composable
private fun StatBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = FatumColors.Accent)
        Text(label, style = MaterialTheme.typography.labelSmall, color = FatumColors.PrimaryVariant)
    }
}
