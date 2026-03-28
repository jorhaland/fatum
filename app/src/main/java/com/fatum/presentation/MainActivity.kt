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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.fatum.presentation.navigation.*
import com.fatum.presentation.screens.analytics.AnalyticsScreen
import com.fatum.presentation.screens.focus.FocusScreen
import com.fatum.presentation.screens.goals.GoalsScreen
import com.fatum.presentation.screens.habits.HabitsScreen
import com.fatum.presentation.screens.home.HomeScreen
import com.fatum.presentation.screens.planner.PlannerScreen
import com.fatum.presentation.screens.settings.SettingsScreen
import com.fatum.presentation.theme.FatumColors
import com.fatum.presentation.theme.FatumTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host.
 * Settings is accessible via the gear icon in the top app bar of the Home screen
 * so it doesn't consume a precious bottom-nav slot.
 */
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
    val navController = rememberNavController()
    val backStack     by navController.currentBackStackEntryAsState()
    val currentRoute  = backStack?.destination?.route

    Scaffold(
        containerColor = FatumColors.Background,
        bottomBar = {
            // Hide bottom nav on Settings screen
            if (currentRoute != Screen.Settings.route) {
                NavigationBar(
                    containerColor = FatumColors.Surface,
                    tonalElevation = 0.dp
                ) {
                    BottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.iconFilled else screen.iconOutlined,
                                    contentDescription = screen.label
                                )
                            },
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
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Home.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(onSettingsClick = { navController.navigate(Screen.Settings.route) })
            }
            composable(Screen.Habits.route)    { HabitsScreen() }
            composable(Screen.Planner.route)   { PlannerScreen() }
            composable(Screen.Goals.route)     { GoalsScreen() }
            composable(Screen.Focus.route)     { FocusScreen() }
            composable(Screen.Analytics.route) { AnalyticsScreen() }
            composable(Screen.Settings.route)  { SettingsScreen() }
        }
    }
}
