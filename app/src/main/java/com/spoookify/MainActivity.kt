package com.spoookify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spoookify.playback.CarModeManager
import com.spoookify.ui.navigation.NavGraph
import com.spoookify.ui.navigation.Screen
import com.spoookify.ui.player.PlayerBar
import com.spoookify.ui.theme.LocalAppTheme
import com.spoookify.ui.theme.SpoookifyTheme
import com.spoookify.ui.theme.SpotifyBlack
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.spoookify.ui.settings.ThemeSettingsViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var carModeManager: CarModeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeViewModel: ThemeSettingsViewModel = hiltViewModel()
            val isAmoledBlack by themeViewModel.isAmoledBlack.collectAsState()
            val appTheme by themeViewModel.appTheme.collectAsState()
            val useDynamicColors by themeViewModel.useDynamicColors.collectAsState()
            val scaleFactor by themeViewModel.currentScaleFactor.collectAsState()

            SpoookifyTheme(
                appTheme = appTheme,
                isAmoledBlack = isAmoledBlack,
                useDynamicColors = useDynamicColors,
                scaleFactor = scaleFactor
            ) {
                val navController = rememberNavController()

                MainScreen(navController)
            }
        }
    }
}

@Composable
fun MainScreen(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentTheme = LocalAppTheme.current
    
    val showBars = currentRoute != Screen.Player.route && currentRoute != Screen.CarMode.route

    Scaffold(
        bottomBar = {
            if (showBars) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent, 
                                    currentTheme.background.copy(alpha = 0.8f), 
                                    currentTheme.background
                                )
                            )
                        )
                ) {
                    Column {
                        PlayerBar(onClick = { navController.navigate(Screen.Player.route) })
                        BottomNavigationBar(navController)
                    }
                }
            }
        },
        containerColor = currentTheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = if (showBars) innerPadding.calculateBottomPadding() else 0.dp
                )
        ) {
            NavGraph(navController = navController)
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(Screen.Home, Screen.Search, Screen.Library)
    val currentTheme = LocalAppTheme.current
    NavigationBar(
        containerColor = Color.Transparent,
        contentColor = Color.White,
        tonalElevation = 0.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            val isSelected = currentRoute == screen.route
            val icon = when (screen) {
                Screen.Home -> Icons.Default.Home
                Screen.Search -> Icons.Default.Search
                Screen.Library -> Icons.Default.LibraryMusic
                else -> Icons.Default.Home
            }

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = screen.route,
                        tint = if (isSelected) currentTheme.primary else Color.Gray,
                        modifier = Modifier.size(26.dp)
                    )
                },
                label = { 
                    Text(
                        text = screen.route.replaceFirstChar { it.uppercase() },
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) currentTheme.primary else Color.Gray
                    )
                },
                selected = isSelected,
                onClick = {
                    if (screen == Screen.Home) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    } else if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = currentTheme.primary,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = currentTheme.primary,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = currentTheme.primary.copy(alpha = 0.12f)
                )
            )
        }
    }
}

