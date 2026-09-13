package com.finlite.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.finlite.app.ui.HistoryScreen
import com.finlite.app.ui.HomeScreen
import com.finlite.app.ui.StatsScreen
import com.finlite.app.ui.theme.FinLiteTheme
import com.finlite.app.vm.FinanceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FinLiteTheme { App() } }
    }
}

@Composable
fun App() {
    val nav = rememberNavController()
    val vm: FinanceViewModel = viewModel()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    Scaffold(bottomBar = {
        NavigationBar {
            NavigationBarItem(
                selected = route == "home",
                onClick = { nav.navigate("home") { popUpTo("home"); launchSingleTop = true } },
                icon = { Icon(Icons.Filled.Home, null) },
                label = { Text("Главная") },
            )
            NavigationBarItem(
                selected = route == "stats",
                onClick = { nav.navigate("stats") { launchSingleTop = true } },
                icon = { Icon(Icons.Filled.Info, null) },
                label = { Text("Статистика") },
            )
            NavigationBarItem(
                selected = route == "history",
                onClick = { nav.navigate("history") { launchSingleTop = true } },
                icon = { Icon(Icons.Filled.List, null) },
                label = { Text("История") },
            )
        }
    }) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { HomeScreen(vm) }
            composable("stats") { StatsScreen(vm) }
            composable("history") { HistoryScreen(vm) }
        }
    }
}
