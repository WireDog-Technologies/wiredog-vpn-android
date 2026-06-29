package com.wiredog.vpn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.RemoveModerator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiredog.vpn.domain.model.ConnectionState
import com.wiredog.vpn.ui.theme.VpnBackground
import com.wiredog.vpn.ui.theme.VpnCardBackground
import com.wiredog.vpn.ui.theme.VpnGreen
import com.wiredog.vpn.ui.theme.VpnRed
import com.wiredog.vpn.ui.theme.VpnTextSecondary
import com.wiredog.vpn.ui.theme.VpnYellow

@Composable
fun PowerButton(
    connectionState: ConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp
) {
    val isConnecting = connectionState == ConnectionState.CONNECTING ||
            connectionState == ConnectionState.DISCONNECTING

    val statusColor by animateColorAsState(
        targetValue = when (connectionState) {
            ConnectionState.CONNECTED -> VpnGreen
            ConnectionState.CONNECTING -> VpnRed
            ConnectionState.DISCONNECTING -> VpnYellow
            ConnectionState.DISCONNECTED -> VpnRed
        },
        animationSpec = tween(300),
        label = "statusColor"
    )

    Box(
        modifier = modifier
            .size(size)
            .border(3.dp, statusColor, CircleShape)
            .padding(3.dp)
            .clip(CircleShape)
            .background(VpnBackground)
            .padding(20.dp)
            .clip(CircleShape)
            .background(statusColor)
            .clickable(
                enabled = !isConnecting,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = when (connectionState) {
                ConnectionState.CONNECTED -> Icons.Default.Shield
                ConnectionState.DISCONNECTING -> Icons.Default.Shield
                ConnectionState.CONNECTING -> Icons.Outlined.RemoveModerator
                ConnectionState.DISCONNECTED -> Icons.Outlined.RemoveModerator
            },
            contentDescription = when (connectionState) {
                ConnectionState.CONNECTED -> "Connected - tap to disconnect"
                ConnectionState.CONNECTING -> "Connecting..."
                ConnectionState.DISCONNECTING -> "Disconnecting..."
                ConnectionState.DISCONNECTED -> "Disconnected - tap to connect"
            },
            modifier = Modifier.size(size * 0.4f),
            tint = VpnBackground
        )
    }
}

@Composable
fun ConnectionStatusText(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when (connectionState) {
            ConnectionState.CONNECTED -> VpnGreen
            ConnectionState.CONNECTING, ConnectionState.DISCONNECTING -> VpnYellow
            ConnectionState.DISCONNECTED -> VpnRed
        },
        animationSpec = tween(300),
        label = "statusTextColor"
    )

    val statusText = when (connectionState) {
        ConnectionState.CONNECTED -> "PROTECTED"
        ConnectionState.CONNECTING -> "CONNECTING..."
        ConnectionState.DISCONNECTING -> "DISCONNECTING..."
        ConnectionState.DISCONNECTED -> "UNPROTECTED"
    }

    androidx.compose.material3.Text(
        text = statusText,
        modifier = modifier,
        color = statusColor,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp
    )
}
