package com.fatum.presentation.screens.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatum.data.db.entities.*
import com.fatum.presentation.components.*
import com.fatum.presentation.theme.FatumColors
import com.fatum.presentation.viewmodels.DashboardViewModel
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToHabits: () -> Unit,
    onNavigateToTasks:  () -> Unit,
    onNavigateToGoals:  () -> Unit,
    onNavigateToAgenda: () -> Unit,
    onNavigateToFocus:  () -> Unit,
    onNavigateToProfile:() -> Unit,
    vm: DashboardViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.refresh() }

    Scaffold(
        containerColor = FatumColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("fatum", style = MaterialTheme.typography.headlineLarge, color = FatumColors.Green) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FatumColors.Background),
                actions = {
                    IconButton(onClick = onNavigateToFocus) {
                        Icon(Icons.Outlined.Timer, "Enfoque", tint = FatumColors.TextMuted)
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Outlined.Person, "Perfil", tint = FatumColors.TextMuted)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Today's schedule preview ──────────────────────────────────────
            item {
                DashSection(title = "Hoy", actionLabel = "Ver todo", onAction = onNavigateToAgenda) {
                    if (state.todayEvents.isEmpty()) {
                        EmptyMini("Sin eventos hoy")
                    } else {
                        state.todayEvents.take(4).forEach { ev ->
                            TodayEventRow(ev, onClick = onNavigateToAgenda)
                        }
                        if (state.todayEvents.size > 4) {
                            Text(
                                "+${state.todayEvents.size - 4} más",
                                style = MaterialTheme.typography.labelSmall,
                                color = FatumColors.TextMuted,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                        }
                    }
                }
            }

            // ── Habits ────────────────────────────────────────────────────────
            item {
                DashSection(title = "Hábitos", actionLabel = "Ver todo", onAction = onNavigateToHabits) {
                    if (state.habits.isEmpty()) {
                        EmptyMini("Crea tu primer hábito")
                    } else {
                        val done = state.habitDoneIds
                        state.habits.forEach { habit ->
                            HabitDashRow(
                                habit    = habit,
                                done     = habit.id in done,
                                onClick  = onNavigateToHabits
                            )
                        }
                    }
                }
            }

            // ── Tasks ─────────────────────────────────────────────────────────
            item {
                DashSection(title = "Tareas", actionLabel = "Ver todo", onAction = onNavigateToTasks) {
                    if (state.topTasks.isEmpty()) {
                        EmptyMini("Sin tareas pendientes ✓")
                    } else {
                        state.topTasks.forEach { task ->
                            TaskDashRow(task = task, onClick = onNavigateToTasks)
                        }
                    }
                }
            }

            // ── Goals ─────────────────────────────────────────────────────────
            item {
                DashSection(title = "Metas", actionLabel = "Ver todo", onAction = onNavigateToGoals) {
                    if (state.topGoals.isEmpty()) {
                        EmptyMini("Añade tu primera meta")
                    } else {
                        state.topGoals.forEach { goal ->
                            GoalDashRow(goal = goal, onClick = onNavigateToGoals)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ── Section wrapper ────────────────────────────────────────────────────────────
@Composable
private fun DashSection(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    FatumCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = FatumColors.TextPrimary)
            TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp)) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge, color = FatumColors.Green)
                Icon(Icons.Default.ChevronRight, null, tint = FatumColors.Green, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable private fun EmptyMini(msg: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(msg, style = MaterialTheme.typography.bodySmall, color = FatumColors.TextMuted)
    }
}

// ── Today event row ────────────────────────────────────────────────────────────
@Composable
private fun TodayEventRow(event: CalendarEventEntity, onClick: () -> Unit) {
    val fmt   = DateTimeFormatter.ofPattern("HH:mm")
    val start = Instant.ofEpochMilli(event.startTimestamp).atZone(ZoneId.systemDefault()).toLocalTime().format(fmt)
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(start, style = MaterialTheme.typography.labelLarge, color = FatumColors.Green, modifier = Modifier.width(40.dp))
        Box(Modifier.width(3.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(importanceColor(event.importance)))
        Text(event.title, style = MaterialTheme.typography.bodyMedium, color = FatumColors.TextPrimary, modifier = Modifier.weight(1f), maxLines = 1)
    }
}

// ── Habit row on dashboard ─────────────────────────────────────────────────────
@Composable
private fun HabitDashRow(habit: HabitEntity, done: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(20.dp).clip(CircleShape)
                .background(if (done) FatumColors.Green else FatumColors.SurfaceVariant)
                .border(1.dp, if (done) FatumColors.GreenDim else FatumColors.Border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (done) Icon(Icons.Default.Check, null, tint = Color(0xFF0F1117), modifier = Modifier.size(12.dp))
        }
        Text(habit.name, style = MaterialTheme.typography.bodyMedium, color = if (done) FatumColors.TextMuted else FatumColors.TextPrimary, modifier = Modifier.weight(1f))
        if (habit.currentStreak > 0) StreakBadge(habit.currentStreak)
    }
}

// ── Task row on dashboard ──────────────────────────────────────────────────────
@Composable
private fun TaskDashRow(task: TaskEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PriorityDot(task.priority)
        Text(task.title, style = MaterialTheme.typography.bodyMedium, color = FatumColors.TextPrimary, modifier = Modifier.weight(1f), maxLines = 1)
    }
}

// ── Goal row on dashboard ──────────────────────────────────────────────────────
@Composable
private fun GoalDashRow(goal: GoalEntity, onClick: () -> Unit) {
    val progress = when (goal.goalType) {
        "VALUE" -> if (goal.targetValue > 0) (goal.currentValue / goal.targetValue).coerceIn(0f, 1f) else 0f
        else    -> 0f // milestone progress shown textually
    }
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PriorityDot(goal.priority)
            Spacer(Modifier.width(10.dp))
            Text(goal.title, style = MaterialTheme.typography.bodyMedium, color = FatumColors.TextPrimary, modifier = Modifier.weight(1f), maxLines = 1)
        }
        if (goal.goalType == "VALUE" && goal.targetValue > 0) {
            Spacer(Modifier.height(4.dp))
            FatumProgressBar(progress = progress, modifier = Modifier.padding(start = 20.dp))
        }
    }
}
