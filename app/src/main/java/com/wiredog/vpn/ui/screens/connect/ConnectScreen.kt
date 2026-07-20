package com.wiredog.vpn.ui.screens.connect

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.view.HapticFeedbackConstants
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.wiredog.vpn.ui.navigation.Screen
import com.wiredog.vpn.domain.model.ConnectionState
import com.wiredog.vpn.ui.components.ConnectionStatusText
import com.wiredog.vpn.ui.components.ConnectionTimer
import com.wiredog.vpn.ui.components.IosevkaTermExtended
import com.wiredog.vpn.ui.components.PowerButton
import androidx.compose.foundation.clickable
import com.wiredog.vpn.ui.components.ServerInfoCard
import com.wiredog.vpn.ui.screens.details.ServerDetailsSheet
import com.wiredog.vpn.ui.theme.VpnCardBackground
import com.wiredog.vpn.ui.theme.VpnGreen
import com.wiredog.vpn.ui.theme.VpnPrimary
import com.wiredog.vpn.ui.theme.VpnRed
import com.wiredog.vpn.ui.theme.VpnTextPrimary
import com.wiredog.vpn.ui.theme.VpnTextSecondary
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.delay

@Composable
fun ConnectScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: ConnectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()
    val servers by viewModel.servers.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val view = LocalView.current
    var showServerDetails by remember { mutableStateOf(false) }
    var showServerSelector by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    // VPN permission launcher
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onVpnPermissionGranted()
        } else {
            viewModel.onVpnPermissionDenied()
        }
    }

    // Launch VPN permission dialog when needed
    LaunchedEffect(uiState.vpnPermissionIntent) {
        uiState.vpnPermissionIntent?.let { intent ->
            vpnPermissionLauncher.launch(intent)
        }
    }

    // Track connection duration for the details sheet
    LaunchedEffect(uiState.connectionStartTime) {
        if (uiState.connectionStartTime != null) {
            while (true) {
                elapsedSeconds = (System.currentTimeMillis() - uiState.connectionStartTime!!) / 1000
                delay(1000)
            }
        } else {
            elapsedSeconds = 0
        }
    }

    // Pause/resume stats polling based on app lifecycle
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.onPause()
                Lifecycle.Event.ON_START -> viewModel.onResume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Show error in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            WireDogLogo()

            Spacer(modifier = Modifier.height(24.dp))

            LocationIpCard(
                location = uiState.location,
                ipAddress = uiState.publicIp,
                isLoading = uiState.isLoadingIp,
                isConnected = uiState.connectionState == ConnectionState.CONNECTED,
                onClick = {
                    if (uiState.connectionState == ConnectionState.CONNECTED && selectedServer != null) {
                        showServerDetails = true
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            PowerButton(
                connectionState = uiState.connectionState,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    viewModel.toggleConnection()
                },
                size = 180.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            ConnectionStatusText(connectionState = uiState.connectionState)

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                if (uiState.splitTunnelingEnabled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallSplit,
                            contentDescription = null,
                            tint = VpnTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Split Tunneling: Enabled",
                            fontSize = 13.sp,
                            color = VpnTextSecondary
                        )
                    }
                }
            }

            ConnectionTimeCard(
                connectionStartTime = uiState.connectionStartTime,
                isConnected = uiState.connectionState == ConnectionState.CONNECTED
            )

            Spacer(modifier = Modifier.height(16.dp))

            ServerInfoCard(
                server = selectedServer,
                onClick = {
                    showServerSelector = true
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (showServerDetails && selectedServer != null) {
            val hours = elapsedSeconds / 3600
            val minutes = (elapsedSeconds % 3600) / 60
            val seconds = elapsedSeconds % 60
            val durationString = String.format("%02d:%02d:%02d", hours, minutes, seconds)

            ServerDetailsSheet(
                server = selectedServer!!,
                myIp = uiState.realPublicIp,
                vpnIp = if (uiState.connectionState == ConnectionState.CONNECTED)
                    uiState.publicIp else null,
                connectionDuration = durationString,
                isConnected = uiState.connectionState == ConnectionState.CONNECTED,
                onDismiss = { showServerDetails = false }
            )
        }

        if (showServerSelector) {
            ServerSelectorSheet(
                servers = servers,
                selectedServer = selectedServer,
                onServerSelected = { server ->
                    viewModel.selectServer(server)
                    showServerSelector = false
                },
                onToggleFavorite = { server -> viewModel.toggleFavorite(server) },
                onDismiss = { showServerSelector = false }
            )
        }

        if (uiState.needsSubscription) {
            LaunchedEffect(Unit) {
                navController.navigate(Screen.Subscription.route)
                viewModel.closeSubscriptionSheet()
            }
        }

        if (uiState.showReviewPrompt) {
            val context = LocalContext.current
            ReviewPromptSheet(
                onPositive = {
                    viewModel.recordReviewPromptPositive()
                    viewModel.dismissReviewPrompt()
                    val activity = context as? Activity
                    if (activity != null) {
                        val reviewManager = ReviewManagerFactory.create(context)
                        reviewManager.requestReviewFlow().addOnCompleteListener { request ->
                            if (request.isSuccessful) {
                                reviewManager.launchReviewFlow(activity, request.result)
                            }
                        }
                    }
                },
                onNegative = {
                    viewModel.dismissReviewPrompt()
                    navController.navigate(Screen.ReportIssue.route)
                },
                onDismiss = { viewModel.dismissReviewPrompt() }
            )
        }
    }
}

@Composable
private fun WireDogLogo(modifier: Modifier = Modifier) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(
                    id = com.wiredog.vpn.R.drawable.wiredog_minimal_navy_1024x1024
                ),
                contentDescription = "WireDog Icon",
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Image(
                painter = painterResource(
                    id = com.wiredog.vpn.R.drawable.wiredog_text_logo_1024
                ),
                contentDescription = "WireDog VPN",
                modifier = Modifier.fillMaxWidth(0.6f)
            )
        }
}

@Composable
private fun LocationIpCard(
    location: String?,
    ipAddress: String?,
    isLoading: Boolean,
    isConnected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val valueColor = if (isConnected) VpnGreen else VpnRed
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VpnCardBackground)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your Location",
                    fontSize = 14.sp,
                    color = VpnTextSecondary
                )
                Text(
                    text = location ?: "\u2014",
                    fontSize = 16.sp,
                    fontFamily = IosevkaTermExtended,
                    color = valueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "IP Address",
                    fontSize = 14.sp,
                    color = VpnTextSecondary
                )
                Text(
                    text = if (isLoading && ipAddress == null) "Loading..."
                           else ipAddress ?: "Unknown",
                    fontSize = 16.sp,
                    fontFamily = IosevkaTermExtended,
                    color = valueColor
                )
            }
        }
    }
}

@Composable
private fun ConnectionTimeCard(
    connectionStartTime: Long?,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VpnCardBackground)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Connection Time",
                fontSize = 14.sp,
                color = VpnTextSecondary
            )
            ConnectionTimer(
                startTimeMillis = if (isConnected) connectionStartTime else null
            )
        }
    }
}
