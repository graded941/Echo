package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.activity.ActivityScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dial.DialStudioScreen
import com.example.ui.earbuds.EarbudsScreen
import com.example.ui.health.HealthScreen
import com.example.ui.integrations.IntegrationsScreen
import com.example.ui.notifications.NotificationsScreen
import com.example.ui.theme.EchoOrange
import com.example.ui.theme.MyApplicationTheme

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    object Health : Screen("health", "Health", Icons.Default.Favorite)
    object Activity : Screen("activity", "Activity", Icons.Default.DirectionsRun)
    object Dials : Screen("dials", "AI Dials", Icons.Default.Watch)
    object More : Screen("more", "Ecosystem", Icons.Default.Hub)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
                var moreTabSelection by remember { mutableStateOf(0) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            val items = listOf(Screen.Dashboard, Screen.Health, Screen.Activity, Screen.Dials, Screen.More)
                            items.forEach { screen ->
                                NavigationBarItem(
                                    icon = { Icon(screen.icon, contentDescription = screen.title) },
                                    label = { Text(screen.title) },
                                    selected = currentScreen == screen,
                                    onClick = { currentScreen = screen },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = EchoOrange,
                                        selectedTextColor = EchoOrange,
                                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            Screen.Dashboard -> DashboardScreen(
                                onNavigateToAiDial = { currentScreen = Screen.Dials },
                                onNavigateToEarbuds = { currentScreen = Screen.More; moreTabSelection = 0 },
                                onNavigateToActivity = { currentScreen = Screen.Activity }
                            )
                            Screen.Health -> HealthScreen()
                            Screen.Activity -> ActivityScreen()
                            Screen.Dials -> DialStudioScreen()
                            Screen.More -> MoreEcosystemScreen(initialTab = moreTabSelection)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoreEcosystemScreen(initialTab: Int = 0) {
    var selectedTab by remember { mutableStateOf(initialTab) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = EchoOrange
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Buds Neo 3") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Notifications") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Media & Weather") })
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> EarbudsScreen()
                1 -> NotificationsScreen()
                2 -> IntegrationsScreen()
            }
        }
    }
}
