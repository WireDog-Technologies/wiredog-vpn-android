package com.wiredog.vpn.ui.screens.servers

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.wiredog.vpn.ui.components.StateCard
import com.wiredog.vpn.ui.components.SearchBar
import com.wiredog.vpn.ui.components.ServerCard
import com.wiredog.vpn.ui.components.ServerSubItem
import com.wiredog.vpn.ui.navigation.Screen
import com.wiredog.vpn.ui.theme.VpnBackground
import com.wiredog.vpn.ui.theme.VpnPrimary
import com.wiredog.vpn.ui.theme.VpnRed
import com.wiredog.vpn.ui.theme.VpnTextSecondary

@Composable
fun ServersScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: ServersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val serverGroups by viewModel.serverGroups.collectAsStateWithLifecycle()
    val selectedServer by viewModel.selectedServer.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val isSwitchingServer by viewModel.isSwitchingServer.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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

    // Show connect-attempt errors (subscription check / VPN permission) as a snackbar — distinct
    // from uiState.error below, which replaces the whole list with a retry screen.
    LaunchedEffect(uiState.connectError) {
        uiState.connectError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearConnectError()
        }
    }

    if (uiState.needsSubscription) {
        LaunchedEffect(Unit) {
            navController.navigate(Screen.Subscription.route)
            viewModel.closeSubscriptionSheet()
        }
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val mapHeight = screenHeight * 0.60f

    val mapServers = remember(serverGroups) {
        serverGroups
            .flatMap { it.servers }
            .distinctBy { it.city }
    }

    val totalServerCount = remember(serverGroups) {
        serverGroups.sumOf { it.servers.size }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VpnBackground)
    ) {
        StarPatternOverlay(
            modifier = Modifier.fillMaxSize()
        )

        val shadowColor = Color(0.7f, 0.13f, 0.17f, 0.25f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(mapHeight)
        ) {
            ServerMapView(
                servers = mapServers,
                selectedServer = selectedServer,
                connectionState = connectionState,
                isSwitchingServer = isSwitchingServer,
                onServerSelected = { viewModel.selectServer(it) },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, shadowColor)
                        )
                    )
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                val gradientHeight = 100.dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(mapHeight),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(gradientHeight)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        VpnBackground
                                    )
                                )
                            )
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VpnBackground),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 12.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(VpnTextSecondary.copy(alpha = 0.5f))
                    )

                    SearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LOCATIONS ($totalServerCount)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VpnTextSecondary,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            when {
                uiState.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(VpnBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = VpnPrimary)
                        }
                    }
                }

                uiState.error != null -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(VpnBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = uiState.error ?: "An error occurred",
                                    color = VpnRed,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Tap to retry",
                                    color = VpnPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { viewModel.loadServers() }
                                )
                            }
                        }
                    }
                }

                serverGroups.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(VpnBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (uiState.searchQuery.isNotBlank()) {
                                    "No servers match your search"
                                } else {
                                    "No servers available"
                                },
                                color = VpnTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                else -> {
                    items(
                        items = serverGroups,
                        key = { it.state }
                    ) { group ->
                        val isExpanded = uiState.expandedStateKey == group.state

                        Box(
                            modifier = Modifier
                                .background(VpnBackground)
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 8.dp)
                        ) {
                            if (group.servers.size == 1) {
                                val server = group.servers.first()
                                ServerCard(
                                    server = server,
                                    isSelected = selectedServer?.id == server.id,
                                    onSelect = { viewModel.selectServer(server) },
                                    onToggleFavorite = { viewModel.toggleFavorite(server) }
                                )
                            } else {
                                Column {
                                    StateCard(
                                        group = group,
                                        isExpanded = isExpanded,
                                        onClick = { viewModel.toggleStateExpansion(group) }
                                    )

                                    if (group.servers.size <= 3) {
                                        AnimatedVisibility(
                                            visible = isExpanded,
                                            enter = expandVertically(),
                                            exit = shrinkVertically()
                                        ) {
                                            Column {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                group.servers.forEach { server ->
                                                    ServerSubItem(
                                                        server = server,
                                                        isSelected = selectedServer?.id == server.id,
                                                        onSelect = { viewModel.selectServer(server) },
                                                        onToggleFavorite = { viewModel.toggleFavorite(server) }
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(
                            modifier = Modifier
                                .height(80.dp)
                                .fillMaxWidth()
                                .background(VpnBackground)
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    uiState.sheetGroup?.let { staleGroup ->
        val liveGroup = serverGroups.find { it.state == staleGroup.state } ?: staleGroup
        StateServersSheet(
            group = liveGroup,
            selectedServer = selectedServer,
            onServerSelected = { viewModel.selectServer(it) },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onDismiss = { viewModel.dismissSheet() }
        )
    }
}
