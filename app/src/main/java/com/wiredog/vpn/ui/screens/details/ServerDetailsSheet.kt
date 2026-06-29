package com.wiredog.vpn.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiredog.vpn.domain.model.Server
import com.wiredog.vpn.ui.theme.VpnBackground
import com.wiredog.vpn.ui.theme.VpnCardBackground
import com.wiredog.vpn.ui.theme.VpnDividerColor
import com.wiredog.vpn.ui.theme.VpnGreen
import com.wiredog.vpn.ui.theme.VpnPrimary
import com.wiredog.vpn.ui.theme.VpnRed
import com.wiredog.vpn.ui.theme.VpnTextPrimary
import com.wiredog.vpn.ui.theme.VpnTextSecondary
import com.wiredog.vpn.ui.theme.VpnYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerDetailsSheet(
    server: Server,
    myIp: String?,
    vpnIp: String?,
    connectionDuration: String,
    isConnected: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isIpVisible by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VpnBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(VpnTextSecondary.copy(alpha = 0.5f))
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            ServerHeaderCard(server = server)

            Spacer(modifier = Modifier.height(16.dp))

            ConnectionDetailsCard(
                server = server,
                myIp = myIp,
                vpnIp = vpnIp,
                connectionDuration = connectionDuration,
                isConnected = isConnected,
                isIpVisible = isIpVisible,
                onToggleIpVisibility = { isIpVisible = !isIpVisible }
            )
        }
    }
}

@Composable
private fun ServerHeaderCard(
    server: Server,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VpnCardBackground)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\uD83C\uDDFA\uD83C\uDDF8",
                fontSize = 40.sp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = server.state,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VpnTextPrimary
                )
                Text(
                    text = server.id,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    color = VpnPrimary
                )
            }
        }
    }
}

@Composable
private fun ConnectionDetailsCard(
    server: Server,
    myIp: String?,
    vpnIp: String?,
    connectionDuration: String,
    isConnected: Boolean,
    isIpVisible: Boolean,
    onToggleIpVisibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VpnCardBackground)
    ) {
        Column {
            Text(
                text = "Connection details",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = VpnTextSecondary,
                modifier = Modifier.padding(16.dp)
            )

            DetailRowWithAction(
                label = "My IP",
                value = if (isIpVisible) myIp ?: "Unknown" else "••••••••••••",
                actionIcon = if (isIpVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                onActionClick = onToggleIpVisibility
            )

            DetailDivider()

            DetailRow(
                label = "VPN IP",
                value = if (isConnected) vpnIp ?: "Not connected" else "Not connected"
            )

            DetailDivider()

            DetailRow(
                label = "Connected for",
                value = connectionDuration
            )

            DetailDivider()

            DetailRow(
                label = "State",
                value = server.state
            )

            DetailDivider()

            DetailRow(
                label = "City",
                value = server.city
            )

            DetailDivider()

            DetailRow(
                label = "Server",
                value = server.id
            )

            DetailDivider()

            DetailRowWithLoadIndicator(
                label = "Server load",
                load = server.load
            )

            DetailDivider()

            DetailRow(
                label = "Protocol",
                value = "AmneziaWG"
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = VpnTextSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = VpnTextPrimary
        )
    }
}

@Composable
private fun DetailRowWithAction(
    label: String,
    value: String,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = VpnTextSecondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = actionIcon,
                contentDescription = "Toggle visibility",
                tint = VpnTextSecondary,
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onActionClick)
            )
        }
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = VpnTextPrimary
        )
    }
}

@Composable
private fun DetailRowWithLoadIndicator(
    label: String,
    load: Int,
    modifier: Modifier = Modifier
) {
    val loadColor = when {
        load < 50 -> VpnGreen
        load < 80 -> VpnYellow
        else -> VpnRed
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = VpnTextSecondary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(24.dp)
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(24.dp),
                    color = VpnTextTertiary,
                    strokeWidth = 3.dp,
                    strokeCap = StrokeCap.Round
                )
                CircularProgressIndicator(
                    progress = { load / 100f },
                    modifier = Modifier.size(24.dp),
                    color = loadColor,
                    strokeWidth = 3.dp,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "${load}%",
                    fontSize = 7.sp,
                    color = VpnTextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$load",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = VpnTextPrimary
            )
        }
    }
}

@Composable
private fun DetailDivider() {
    Divider(
        color = VpnDividerColor,
        thickness = 1.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

private val VpnTextTertiary = Color(0xFF59616F)
