package com.example.silencio.ui.navigation

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.silencio.ui.home.HomeScreen
import com.example.silencio.ui.home.HomeViewModel
import com.example.silencio.ui.meetings.MeetingsScreen
import com.example.silencio.ui.onboarding.OnboardingScreen
import com.example.silencio.ui.settings.SettingsScreen
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.silencio.ui.congrats.CongratsScreen
import com.example.silencio.ui.premium.NotificationAccessScreen
import com.example.silencio.ui.premium.PaywallScreen
import com.example.silencio.ui.premium.PremiumSettingsScreen
import com.example.silencio.ui.premium.PremiumViewModel
import com.example.silencio.ui.premium.ReplyContactPickerScreen
import com.example.silencio.ui.theme.AccentBlue
import com.example.silencio.ui.theme.Background
import com.example.silencio.ui.theme.Surface
import com.example.silencio.ui.theme.TextMuted

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object VipContact : Screen("vip_contact")
    object Home : Screen("home")
    object Meetings : Screen("meetings")
    object Settings : Screen("settings")
    object Paywall : Screen("paywall")
    object ReplyContactPicker : Screen("reply_contact_picker")
    object NotificationAccess : Screen("notification_access")
    object Congrats : Screen("congrats")
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val isOnboarded by homeViewModel.isOnboarded.collectAsState()

    if (isOnboarded == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF111111))
        )
        return
    }

    val startDestination = remember {
        if (isOnboarded == true) Screen.Home.route else Screen.Onboarding.route
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarRoutes = remember {
        setOf(Screen.Home.route, Screen.Meetings.route, Screen.Paywall.route, Screen.Settings.route)
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                SilencioBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onCalendarConnected = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(viewModel = homeViewModel)
            }

            composable(Screen.Meetings.route) {
                MeetingsScreen()
            }

            composable(Screen.Congrats.route) {
                CongratsScreen(
                    onContinue = {
                        navController.navigate(Screen.ReplyContactPicker.route) {
                            popUpTo(Screen.Congrats.route) { inclusive = true }
                        }
                    },
                    onSkip = {
                        navController.navigate(Screen.Paywall.route) {
                            popUpTo(Screen.Congrats.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Paywall.route) {
                val premiumViewModel: PremiumViewModel = hiltViewModel()
                val uiState by premiumViewModel.uiState.collectAsState()

                if (!uiState.isPremium) {
                    BackHandler {
                        navController.popBackStack()
                    }
                }

                if (uiState.isPremium) {
                    PremiumSettingsScreen(viewModel = premiumViewModel)
                } else {
                    PaywallScreen(
                        onPurchase = {
                            navController.navigate(Screen.Congrats.route) {
                                popUpTo(Screen.Paywall.route) { inclusive = true }
                            }
                        },
                        onSkip = {
                            navController.popBackStack()
                        }
                    )
                }
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onUpgrade = { navController.navigate(Screen.Paywall.route) }
                )
            }

            composable(Screen.NotificationAccess.route) {
                NotificationAccessScreen(
                    onDone = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.NotificationAccess.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.ReplyContactPicker.route) {
                ReplyContactPickerScreen(
                    onDone = {
                        navController.navigate(Screen.NotificationAccess.route) {
                            popUpTo(Screen.ReplyContactPicker.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SilencioBottomBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit
) {
    val tabs = listOf(
        Triple(Screen.Home.route, Icons.Default.Home, "Home"),
        Triple(Screen.Meetings.route, Icons.Default.CalendarMonth, "Meetings"),
        Triple(Screen.Paywall.route, Icons.Default.Star, "Premium"),
        Triple(Screen.Settings.route, Icons.Default.Settings, "Settings")
    )

    NavigationBar(
        containerColor = Surface,
        tonalElevation = 0.dp
    ) {
        tabs.forEach { (route, icon, label) ->
            val selected = currentRoute == route
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(route) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentBlue,
                    selectedTextColor = AccentBlue,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    indicatorColor = Background
                )
            )
        }
    }
}