package com.fatum.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector
) {
    object Home      : Screen("home",      "Diario",   Icons.Filled.Home,         Icons.Outlined.Home)
    object Habits    : Screen("habits",    "Hábitos",  Icons.Filled.CheckCircle,  Icons.Outlined.CheckCircle)
    object Planner   : Screen("planner",   "Agenda",   Icons.Filled.CalendarMonth,Icons.Outlined.CalendarMonth)
    object Goals     : Screen("goals",     "Metas",    Icons.Filled.Flag,         Icons.Outlined.Flag)
    object Focus     : Screen("focus",     "Enfoque",  Icons.Filled.Timer,        Icons.Outlined.Timer)
    object Analytics : Screen("analytics", "Análisis", Icons.Filled.BarChart,     Icons.Outlined.BarChart)
    object Settings  : Screen("settings",  "Ajustes",  Icons.Filled.Settings,     Icons.Outlined.Settings)
}

val BottomNavItems = listOf(
    Screen.Home,
    Screen.Habits,
    Screen.Planner,
    Screen.Goals,
    Screen.Focus,
    Screen.Analytics
)
