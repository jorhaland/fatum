package com.fatum.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/** All top-level navigation destinations for the bottom nav bar. */
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home      : Screen("home",      "Diario",    Icons.Default.Home)
    object Habits    : Screen("habits",    "Hábitos",   Icons.Default.CheckCircle)
    object Planner   : Screen("planner",   "Agenda",    Icons.Default.CalendarMonth)
    object Goals     : Screen("goals",     "Metas",     Icons.Default.Flag)
    object Focus     : Screen("focus",     "Enfoque",   Icons.Default.Timer)
    object Analytics : Screen("analytics", "Análisis",  Icons.Default.BarChart)
}

val BottomNavItems = listOf(
    Screen.Home,
    Screen.Habits,
    Screen.Planner,
    Screen.Goals,
    Screen.Focus,
    Screen.Analytics
)
