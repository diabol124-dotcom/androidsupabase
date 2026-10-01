package com.example.std

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

data class MainTab(val route: String, val label: String, val icon: ImageVector)

val mainTabs = listOf(
    MainTab("list", "Студенты", Icons.Default.Home),
    MainTab("dev", "Скоро", Icons.Default.Lock),
    MainTab("groups", "Группы", Icons.AutoMirrored.Filled.List),
    MainTab("profile", "Профиль", Icons.Default.AccountCircle)
)

fun isMainTab(route: String) = mainTabs.any { it.route == route }

/** Нижняя панель (телефон) */
@Composable
fun AppNavigationBar(current: String, onSelect: (String) -> Unit) {
    NavigationBar {
        mainTabs.forEach { tab ->
            NavigationBarItem(
                selected = current == tab.route,
                onClick = { onSelect(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}

/** Боковая панель (планшет / широкий экран) */
@Composable
fun AppNavigationRail(current: String, onSelect: (String) -> Unit) {
    NavigationRail {
        Spacer(modifier = Modifier.weight(1f))
        mainTabs.forEach { tab ->
            NavigationRailItem(
                selected = current == tab.route,
                onClick = { onSelect(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}
