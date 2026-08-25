package com.wiredog.vpn.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wiredog.vpn.ui.screens.auth.AnonymousAccountScreen
import com.wiredog.vpn.ui.screens.auth.CreateAccountScreen
import com.wiredog.vpn.ui.screens.auth.ForgotPasswordScreen
import com.wiredog.vpn.ui.screens.auth.LoginScreen
import com.wiredog.vpn.ui.screens.auth.StandardAccountScreen
import com.wiredog.vpn.ui.screens.connect.ConnectScreen
import com.wiredog.vpn.ui.screens.servers.ServersScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.wiredog.vpn.data.remote.api.WireDogApi
import com.wiredog.vpn.data.remote.api.dto.ReportBugRequest
import com.wiredog.vpn.ui.screens.settings.ChangeLogScreen
import com.wiredog.vpn.ui.screens.settings.LogsScreen
import com.wiredog.vpn.ui.screens.settings.LogViewerScreen
import com.wiredog.vpn.ui.screens.settings.ProfileScreen
import com.wiredog.vpn.ui.screens.settings.ReportBugScreen
import com.wiredog.vpn.ui.screens.settings.ReportIssueScreen
import com.wiredog.vpn.ui.screens.settings.SettingsScreen
import com.wiredog.vpn.ui.screens.settings.SplitTunnelingScreen
import com.wiredog.vpn.ui.screens.connect.SubscriptionScreen

@Composable
fun WireDogNavHost(
    navController: NavHostController,
    isLoggedIn: Boolean,
    onLoginSuccess: () -> Unit,
    onLogout: () -> Unit,
    wireDogApi: WireDogApi,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            navController.navigate(Screen.Connect.route) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) Screen.Connect.route else Screen.Login.route,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = onLoginSuccess,
                onNavigateToCreateAccount = {
                    navController.navigate(Screen.CreateAccount.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                }
            )
        }

        composable(Screen.CreateAccount.route) {
            CreateAccountScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToStandard = { navController.navigate(Screen.StandardAccount.route) },
                onNavigateToAnonymous = { navController.navigate(Screen.AnonymousAccount.route) }
            )
        }

        composable(Screen.StandardAccount.route) {
            StandardAccountScreen(
                onNavigateBack = { navController.popBackStack() },
                onSuccess = onLoginSuccess
            )
        }

        composable(Screen.AnonymousAccount.route) {
            AnonymousAccountScreen(
                onNavigateBack = { navController.popBackStack() },
                onLoginSuccess = onLoginSuccess
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Servers.route) {
            ServersScreen(navController = navController)
        }

        composable(Screen.Connect.route) {
            ConnectScreen(navController = navController)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToLogs = {
                    navController.navigate(Screen.Logs.route)
                },
                onNavigateToChangeLog = {
                    navController.navigate(Screen.ChangeLog.route)
                },
                onNavigateToReportIssue = {
                    navController.navigate(Screen.ReportIssue.route)
                },
                onNavigateToSplitTunneling = {
                    navController.navigate(Screen.SplitTunneling.route)
                }
            )
        }

        composable(Screen.SplitTunneling.route) {
            SplitTunnelingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSignOut = onLogout
            )
        }

        composable(Screen.Logs.route) {
            LogsScreen(
                onNavigateToLogViewer = { logType ->
                    navController.navigate(Screen.LogViewer.createRoute(logType))
                }
            )
        }

        composable(
            route = Screen.LogViewer.route,
            arguments = listOf(navArgument("logType") { type = NavType.StringType })
        ) {
            LogViewerScreen()
        }

        composable(Screen.ChangeLog.route) {
            ChangeLogScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ReportIssue.route) {
            ReportIssueScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToReportBug = {
                    navController.navigate(Screen.ReportBug.route)
                }
            )
        }

        composable(Screen.ReportBug.route) {
            ReportBugScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSubmitBugReport = { request ->
                    try {
                        wireDogApi.reportBug(request)
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
            )
        }

        composable(Screen.Subscription.route) {
            SubscriptionScreen(
                onDismiss = {
                    navController.popBackStack()
                },
                onRetryAfterSubscription = {
                    navController.popBackStack()
                }
            )
        }
    }
}
