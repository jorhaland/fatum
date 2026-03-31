package com.fatum.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.fatum.presentation.navigation.*
import com.fatum.presentation.screens.focus.FocusScreen
import com.fatum.presentation.screens.goals.GoalsScreen
import com.fatum.presentation.screens.habits.HabitsScreen
import com.fatum.presentation.screens.home.DashboardScreen
import com.fatum.presentation.screens.planner.PlannerScreen
import com.fatum.presentation.screens.profile.ProfileScreen
import com.fatum.presentation.screens.tasks.TasksScreen
import com.fatum.presentation.theme.FatumColors
import com.fatum.presentation.theme.FatumTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FatumTheme { FatumApp() } }
    }
}

@Composable
private fun FatumApp() {
    val nav          = rememberNavController()
    val backStack    by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    // Routes that hide the bottom bar
    val noBottomBar = setOf(Screen.Focus.route, Screen.Profile.route)

    Scaffold(
        containerColor = FatumColors.Background,
        bottomBar = {
            if (currentRoute !in noBottomBar) {
                NavigationBar(containerColor = FatumColors.Surface, tonalElevation = 0.dp) {
                    BottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick  = {
                                nav.navigate(screen.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon  = { Icon(if (selected) screen.iconFilled else screen.iconOutlined, screen.label) },
                            label = { Text(screen.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor   = FatumColors.Green,
                                selectedTextColor   = FatumColors.Green,
                                unselectedIconColor = FatumColors.TextMuted,
                                unselectedTextColor = FatumColors.TextMuted,
                                indicatorColor      = FatumColors.GreenSurface
                            )
                        )
                    }
                }
            }
        }
    ) { inner ->
        NavHost(
            navController    = nav,
            startDestination = Screen.Home.route,
            modifier         = Modifier.padding(inner)
        ) {
            composable(Screen.Home.route) {
                DashboardScreen(
                    onNavigateToHabits  = { nav.navigate(Screen.Habits.route) },
                    onNavigateToTasks   = { nav.navigate(Screen.Tasks.route) },
                    onNavigateToGoals   = { nav.navigate(Screen.Goals.route) },
                    onNavigateToAgenda  = { nav.navigate(Screen.Agenda.route) },
                    onNavigateToFocus   = { nav.navigate(Screen.Focus.route) },
                    onNavigateToProfile = { nav.navigate(Screen.Profile.route) }
                )
            }
            composable(Screen.Habits.route)  { HabitsScreen() }
            composable(Screen.Tasks.route)   { TasksScreen() }
            composable(Screen.Goals.route)   { GoalsScreen() }
            composable(Screen.Agenda.route)  { PlannerScreen() }
            composable(Screen.Focus.route)   { FocusScreen(onBack = { nav.popBackStack() }) }
            composable(Screen.Profile.route) { ProfileScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
