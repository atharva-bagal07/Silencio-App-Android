package com.example.silencio.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.silencio.ui.home.HomeScreen
import com.example.silencio.ui.onboarding.OnboardingScreen
import com.example.silencio.ui.onboarding.VipContactScreen
import com.example.silencio.ui.settings.SettingsScreen
import com.example.silencio.ui.home.HomeViewModel

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object VipContact : Screen("vip_contact")
    object Home : Screen("home")
    object Settings : Screen("settings")
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val isOnboarded by homeViewModel.isOnboarded.collectAsState(initial = false)

    NavHost(
        navController = navController,
        startDestination = if (isOnboarded) {
            Screen.Home.route
        } else {
            Screen.Onboarding.route
        }
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onCalendarConnected = {
                    navController.navigate(Screen.VipContact.route) {
                        popUpTo(Screen.Onboarding.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.VipContact.route) {
            VipContactScreen(
                onDone = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.VipContact.route) {
                            inclusive = true
                        }
                    }
                },
                onSkip = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.VipContact.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}