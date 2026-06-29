package com.wiredog.vpn.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    data object Login : Screen("login", "Login")

    data object Servers : Screen(
        route = "servers",
        title = "Servers",
        selectedIcon = Icons.Filled.Public,
        unselectedIcon = Icons.Outlined.Public
    )

    data object Connect : Screen(
        route = "connect",
        title = "Connect",
        selectedIcon = Icons.Filled.PowerSettingsNew,
        unselectedIcon = Icons.Outlined.PowerSettingsNew
    )

    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    data object Profile : Screen("profile", "Profile")

    data object Logs : Screen("logs", "Logs")

    data object ChangeLog : Screen("change_log", "Change Log")

    data object ReportIssue : Screen("report_issue", "Report Issue")

    data object ReportBug : Screen("report_bug", "Report Bug")

    data object SplitTunneling : Screen("split_tunneling", "Split Tunneling")

    data object CreateAccount : Screen("create_account", "Create Account")
    data object StandardAccount : Screen("standard_account", "Standard Account")
    data object AnonymousAccount : Screen("anonymous_account", "Anonymous Account")
    data object ForgotPassword : Screen("forgot_password", "Forgot Password")

    data object LogViewer : Screen("log_viewer/{logType}", "Log Viewer") {
        fun createRoute(logType: String) = "log_viewer/$logType"
    }

    data object Subscription : Screen("subscription", "Subscription")

    companion object {
        val bottomNavItems = listOf(Servers, Connect, Settings)
    }
}
