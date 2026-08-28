package com.spoookify.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.spoookify.ui.home.HomeScreen
import com.spoookify.ui.library.LibraryScreen
import com.spoookify.ui.player.PlayerScreen
import com.spoookify.ui.search.SearchScreen
import com.spoookify.ui.settings.EqualizerScreen
import com.spoookify.ui.settings.SettingsScreen
import com.spoookify.ui.settings.StorageManagerScreen
import com.spoookify.ui.car.CarModeScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Library : Screen("library")
    object Player : Screen("player")
    object Settings : Screen("settings")
    object Equalizer : Screen("equalizer")
    object CarMode : Screen("car_mode")
    object StorageManager : Screen("storage_manager")
    object Statistics : Screen("statistics")
    object Diagnostics : Screen("diagnostics")
    object HomeCustomization : Screen("home_customization")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        composable(
            route = Screen.Home.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() }
        ) {
            HomeScreen(
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onStatsClick = { navController.navigate(Screen.Statistics.route) },
                onCustomizeClick = { navController.navigate(Screen.HomeCustomization.route) }
            )
        }
        composable(
            route = Screen.Search.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) {
            SearchScreen()
        }
        composable(Screen.Library.route) {
            LibraryScreen(
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onAddClick = { /* Could open a dialog to create playlist */ }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onEqualizerClick = { navController.navigate(Screen.Equalizer.route) },
                onStorageClick = { navController.navigate(Screen.StorageManager.route) },
                onDiagnosticsClick = { navController.navigate(Screen.Diagnostics.route) },
                onCarModeClick = { navController.navigate(Screen.CarMode.route) },
                onHomeCustomizeClick = { navController.navigate(Screen.HomeCustomization.route) }
            )
        }
        composable(
            route = Screen.Player.route,
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(500)
                )
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(500)
                )
            },
            popEnterTransition = {
                EnterTransition.None
            },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(500)
                )
            }
        ) {
            PlayerScreen(
                onBackClick = { navController.popBackStack() },
                onEqualizerClick = { navController.navigate(Screen.Equalizer.route) }
            )
        }

        composable(Screen.Equalizer.route) {
            EqualizerScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.StorageManager.route) {
            StorageManagerScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.Statistics.route) {
            com.spoookify.ui.stats.StatisticsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.Diagnostics.route) {
            com.spoookify.ui.settings.DiagnosticsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.HomeCustomization.route) {
            com.spoookify.ui.home.HomeCustomizationScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
