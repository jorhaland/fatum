package com.fatum.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.fatum.presentation.navigation.*
import com.fatum.presentation.screens.analytics.AnalyticsScreen
import com.fatum.presentation.screens.focus.FocusScreen
import com.fatum.presentation.screens.goals.GoalsScreen
import com.fatum.presentation.screens.habits.HabitsScreen
import com.fatum.presentation.screens.home.HomeScreen
import com.fatum.presentation.screens.planner.PlannerScreen
import com.fatum.presentation.theme.FatumColors
import com.fatum.presentation.theme.FatumTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host for all Compose screens.
 * Navigation is handled by a [NavHost] with a bottom navigation bar.
 *
 * Time-to-first-log target: < 1.5 s (RNF-1).
 * Dark mode always on by default (RNF – mandatory dark support).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FatumTheme(darkTheme = true) {
                FatumNavHost()
            }
        }
    }
}

@Composable
private fun FatumNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = FatumColors.Background,
        bottomBar = {
            NavigationBar(containerColor = FatumColors.Surface) {
                BottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(screen.icon, contentDescription = screen.label)
                        },
                        label = { Text(screen.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = FatumColors.Accent,
                            selectedTextColor   = FatumColors.Accent,
                            unselectedIconColor = FatumColors.PrimaryVariant,
                            unselectedTextColor = FatumColors.PrimaryVariant,
                            indicatorColor      = FatumColors.SurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route)      { HomeScreen() }
            composable(Screen.Habits.route)    { HabitsScreen() }
            composable(Screen.Planner.route)   { PlannerScreen() }
            composable(Screen.Goals.route)     { GoalsScreen() }
            composable(Screen.Focus.route)     { FocusScreen() }
            composable(Screen.Analytics.route) { AnalyticsScreen() }
        }
    }
}
