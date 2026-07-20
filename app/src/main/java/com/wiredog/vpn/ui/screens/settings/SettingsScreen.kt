package com.wiredog.vpn.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wiredog.vpn.data.config.Config
import com.wiredog.vpn.ui.theme.VpnBackground
import com.wiredog.vpn.ui.theme.VpnCardBackground
import com.wiredog.vpn.ui.theme.VpnDividerColor
import com.wiredog.vpn.ui.theme.VpnGreen
import com.wiredog.vpn.ui.theme.VpnPrimary
import com.wiredog.vpn.ui.theme.VpnRed
import com.wiredog.vpn.ui.theme.VpnSecondaryBackground
import com.wiredog.vpn.ui.theme.VpnTextPrimary
import com.wiredog.vpn.ui.theme.VpnTextSecondary
import com.wiredog.vpn.ui.theme.VpnYellow

@Composable
fun SettingsScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToChangeLog: () -> Unit,
    onNavigateToReportIssue: () -> Unit,
    onNavigateToSplitTunneling: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current
    var showReconnectDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.showReconnectWarning.collect {
            showReconnectDialog = true
        }
    }

    if (showReconnectDialog) {
        AlertDialog(
            onDismissRequest = { showReconnectDialog = false },
            title = { Text("Reconnect Required", color = VpnTextPrimary) },
            text = { Text("Reconnect for changes to take effect.", color = VpnTextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReconnectDialog = false
                        viewModel.reconnectNow()
                    }
                ) {
                    Text("Reconnect Now", color = VpnPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReconnectDialog = false }) {
                    Text("Later", color = VpnTextSecondary)
                }
            },
            containerColor = VpnCardBackground
        )
    }

    Scaffold(
        containerColor = VpnBackground
    ) { _ ->
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 36.dp, bottom = 16.dp)
        ) {
            Text(
                text = "Settings",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = VpnTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Configure your VPN preferences",
                fontSize = 14.sp,
                color = VpnTextSecondary
            )
        }

        SectionHeader(title = "ACCOUNT")

        SettingsCard {
            ProfileRow(
                displayName = if (!currentUser?.username.isNullOrBlank()) {
                    currentUser?.username ?: "User"
                } else {
                    currentUser?.accountNumber ?: "N/A"
                },
                onClick = onNavigateToProfile
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Privacy Policy",
                fontSize = 13.sp,
                color = VpnPrimary,
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Config.privacyPolicyURL)))
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "•", fontSize = 13.sp, color = VpnTextSecondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Terms of Service",
                fontSize = 13.sp,
                color = VpnPrimary,
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Config.termsOfServiceURL)))
                }
            )
        }

        SectionHeader(title = "CONNECTION")

        SettingsCard {
            SettingsRow(
                icon = Icons.Default.Public,
                iconTint = VpnPrimary,
                title = "Protocol",
                description = "Only AmneziaWG is currently supported",
                trailing = {
                    Text(
                        text = uiState.protocol,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VpnPrimary
                    )
                }
            )

            SettingsDivider()

            SettingsRow(
                icon = Icons.Default.Shield,
                iconTint = VpnRed,
                title = "Kill Switch",
                description = "Configure Always-on VPN in system settings",
                trailing = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = VpnTextSecondary
                    )
                },
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }
            )

            SettingsDivider()

            SettingsRow(
                icon = Icons.Default.Share,
                iconTint = VpnPrimary,
                title = "Split Tunneling",
                description = uiState.splitTunnelingSummary,
                trailing = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = VpnTextSecondary
                    )
                },
                modifier = Modifier.clickable { onNavigateToSplitTunneling() }
            )

            SettingsDivider()

            SettingsToggleRow(
                icon = Icons.Default.Bolt,
                iconTint = VpnYellow,
                title = "Auto-Connect",
                description = "Automatically connect to the last used server on device startup",
                checked = uiState.autoConnectEnabled,
                onCheckedChange = { viewModel.setAutoConnectEnabled(it) }
            )

            SettingsDivider()

            SettingsToggleRow(
                icon = Icons.Default.Language,
                iconTint = VpnPrimary,
                title = "IPv6 Connections",
                description = "Configure IPv6 and leak protection",
                checked = uiState.ipv6Enabled,
                onCheckedChange = { viewModel.setIpv6Enabled(it) }
            )

            SettingsDivider()

            SettingsToggleRow(
                icon = Icons.Default.Block,
                iconTint = VpnPrimary,
                title = "Block Ads",
                description = "Block ad and tracker domains",
                checked = uiState.blockAdsEnabled,
                onCheckedChange = { viewModel.setBlockAdsEnabled(it) }
            )

            SettingsDivider()

            SettingsToggleRow(
                icon = Icons.Default.Security,
                iconTint = VpnPrimary,
                title = "Block Malware",
                description = "Block known malware and phishing domains",
                checked = uiState.blockMalwareEnabled,
                onCheckedChange = { viewModel.setBlockMalwareEnabled(it) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SectionHeader(title = "SUPPORT")

        SettingsCard {
            SettingsRow(
                icon = Icons.Default.Description,
                iconTint = VpnTextSecondary,
                title = "View Logs",
                description = "Application and service diagnostic logs",
                trailing = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = VpnTextSecondary
                    )
                },
                modifier = Modifier.clickable { onNavigateToLogs() }
            )

            SettingsDivider()

            SettingsRow(
                icon = Icons.Default.Description,
                iconTint = VpnPrimary,
                title = "Change Log",
                description = "View recent updates and release notes",
                trailing = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = VpnTextSecondary
                    )
                },
                modifier = Modifier.clickable { onNavigateToChangeLog() }
            )

            SettingsDivider()

            SettingsRow(
                icon = Icons.Default.Shield,
                iconTint = VpnRed,
                title = "Report an Issue",
                description = "Contact support or report a bug",
                trailing = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = VpnTextSecondary
                    )
                },
                modifier = Modifier.clickable { onNavigateToReportIssue() }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "v${viewModel.appVersionName} (build ${viewModel.appVersionCode})",
                fontSize = 11.sp,
                color = VpnTextSecondary.copy(alpha = 0.5f)
            )
        }
    }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(VpnSecondaryBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = VpnTextSecondary,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(VpnCardBackground)
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun ProfileRow(
    displayName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = VpnPrimary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "View Profile",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = VpnTextPrimary,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = displayName,
                fontSize = 12.sp,
                color = VpnTextSecondary,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Navigate",
            tint = VpnTextSecondary
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    description: String,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = VpnTextPrimary,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
            Text(
                text = description,
                fontSize = 13.sp,
                color = VpnTextSecondary,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }

        trailing()
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = VpnTextPrimary,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
            Text(
                text = description,
                fontSize = 13.sp,
                color = VpnTextSecondary,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = VpnTextPrimary,
                checkedTrackColor = VpnGreen,
                uncheckedThumbColor = VpnTextPrimary,
                uncheckedTrackColor = VpnTextSecondary.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = VpnDividerColor,
        thickness = 1.dp,
        modifier = Modifier.padding(start = 52.dp)
    )
}
