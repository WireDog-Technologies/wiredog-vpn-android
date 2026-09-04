package com.wiredog.vpn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope
import com.wiredog.vpn.data.remote.api.WireDogApi
import com.wiredog.vpn.data.repository.AnnouncementRepository
import com.wiredog.vpn.data.repository.AuthRepository
import com.wiredog.vpn.data.repository.UpdateAction
import kotlinx.coroutines.launch
import com.wiredog.vpn.ui.navigation.BottomNavBar
import com.wiredog.vpn.ui.navigation.Screen
import com.wiredog.vpn.ui.navigation.WireDogNavHost
import com.wiredog.vpn.ui.screens.auth.PrivacyDisclosureView
import com.wiredog.vpn.ui.theme.VpnBackground
import com.wiredog.vpn.ui.theme.VpnPrimary
import com.wiredog.vpn.ui.theme.VpnRedBright
import com.wiredog.vpn.ui.theme.VpnTextPrimary
import com.wiredog.vpn.ui.theme.VpnTextSecondary
import com.wiredog.vpn.ui.theme.WireDogVPNAndroidTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var announcementRepository: AnnouncementRepository

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WireDogVPNAndroidTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                when {
                    uiState.isLoading -> LoadingScreen()

                    uiState.showPrivacyDisclosure -> {
                        PrivacyDisclosureView(
                            onAccept = viewModel::acceptPrivacyDisclosure
                        )
                    }

                    uiState.updateAction is UpdateAction.Maintenance -> MaintenanceScreen()

                    uiState.updateAction is UpdateAction.ForceUpdate -> ForceUpdateScreen()

                    else -> {
                        // Show main content with optional soft update dialog
                        Box {
                            val wireDogApi = viewModel.wireDogApi
                            MainContent(
                                isLoggedIn = uiState.isLoggedIn,
                                onLoginSuccess = viewModel::onLoginSuccess,
                                onLogout = viewModel::onLogout,
                                wireDogApi = wireDogApi
                            )

                            if (uiState.updateAction is UpdateAction.SoftUpdate && !uiState.softUpdateDismissed) {
                                SoftUpdateDialog(
                                    onDismiss = viewModel::dismissSoftUpdate
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Poll for in-app announcements only while the app is foregrounded (parity with the
        // iOS BroadcastService lifecycle). Single-Activity app, so this is effectively app-wide.
        announcementRepository.onAppForeground()
        // Refresh the account profile on every foreground — most notably this is what picks up
        // a subscription just purchased on the website checkout page (the user pays in a browser,
        // then switches back; nothing else would tell us their plan changed). Cheap no-op
        // otherwise. Mirrors the iOS MainTabView scene-phase refresh.
        lifecycleScope.launch {
            if (authRepository.isLoggedIn.value) {
                authRepository.fetchProfile()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        announcementRepository.onAppBackground()
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VpnBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.wiredog_minimal_navy_1024x1024),
                contentDescription = "WireDog Logo",
                modifier = Modifier.size(160.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(color = VpnRedBright)
        }
    }
}

@Composable
private fun ForceUpdateScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VpnBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.wiredog_minimal_navy_1024x1024),
                contentDescription = "WireDog Logo",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Update Required",
                color = VpnTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "A new version of WireDog VPN is available. Please update to continue using the app.",
                color = VpnTextSecondary,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MaintenanceScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VpnBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.wiredog_minimal_navy_1024x1024),
                contentDescription = "WireDog Logo",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Under Maintenance",
                color = VpnTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "WireDog VPN is currently undergoing maintenance. Please try again later.",
                color = VpnTextSecondary,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SoftUpdateDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VpnBackground,
        title = {
            Text(
                text = "Update Available",
                color = VpnTextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "A new version of WireDog VPN is available.",
                color = VpnTextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = VpnPrimary)
            }
        }
    )
}

@Composable
private fun MainContent(
    isLoggedIn: Boolean,
    onLoginSuccess: () -> Unit,
    onLogout: () -> Unit,
    wireDogApi: WireDogApi
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in Screen.bottomNavItems.map { it.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->
        WireDogNavHost(
            navController = navController,
            isLoggedIn = isLoggedIn,
            onLoginSuccess = onLoginSuccess,
            onLogout = onLogout,
            wireDogApi = wireDogApi,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
