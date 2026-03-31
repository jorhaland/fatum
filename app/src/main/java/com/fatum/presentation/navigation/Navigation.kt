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
    object Home    : Screen("home",    "Inicio",  Icons.Filled.Home,         Icons.Outlined.Home)
    object Habits  : Screen("habits",  "Hábitos", Icons.Filled.CheckCircle,  Icons.Outlined.CheckCircle)
    object Tasks   : Screen("tasks",   "Tareas",  Icons.Filled.TaskAlt,      Icons.Outlined.TaskAlt)
    object Goals   : Screen("goals",   "Metas",   Icons.Filled.Flag,         Icons.Outlined.Flag)
    object Agenda  : Screen("agenda",  "Agenda",  Icons.Filled.CalendarMonth,Icons.Outlined.CalendarMonth)
    // Not in bottom nav:
    object Focus   : Screen("focus",   "Enfoque", Icons.Filled.Timer,        Icons.Outlined.Timer)
    object Profile : Screen("profile", "Perfil",  Icons.Filled.Person,       Icons.Outlined.Person)
}

/** The 5 bottom-nav destinations. Focus and Profile are accessed via icons. */
val BottomNavItems = listOf(
    Screen.Home,
    Screen.Habits,
    Screen.Tasks,
    Screen.Goals,
    Screen.Agenda
)
