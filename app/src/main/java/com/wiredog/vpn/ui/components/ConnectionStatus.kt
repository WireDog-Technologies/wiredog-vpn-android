package com.wiredog.vpn.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.RemoveModerator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiredog.vpn.domain.model.ConnectionState
import com.wiredog.vpn.ui.theme.VpnBackground
import com.wiredog.vpn.ui.theme.VpnCardBackground
import com.wiredog.vpn.ui.theme.VpnGreen
import com.wiredog.vpn.ui.theme.VpnRed
import com.wiredog.vpn.ui.theme.VpnYellow

/**
 * PROTECTED / UNPROTECTED / CONNECTING… label + colour, shared by the Connect screen
 * status text and the Servers screen status banner so they never disagree.
 */
fun connectionStatusLabel(
    state: ConnectionState,
    isReconnecting: Boolean = false,
    isSwitchingServer: Boolean = false,
): String = when {
    isSwitchingServer -> "SWITCHING..."
    isReconnecting -> "RECONNECTING..."
    else -> when (state) {
        ConnectionState.CONNECTED -> "PROTECTED"
        ConnectionState.CONNECTING -> "CONNECTING..."
        ConnectionState.DISCONNECTING -> "DISCONNECTING..."
        ConnectionState.DISCONNECTED -> "UNPROTECTED"
    }
}

fun connectionStatusColor(
    state: ConnectionState,
    isReconnecting: Boolean = false,
    isSwitchingServer: Boolean = false,
): Color = when {
    isSwitchingServer || isReconnecting -> VpnYellow
    else -> when (state) {
        ConnectionState.CONNECTED -> VpnGreen
        ConnectionState.CONNECTING, ConnectionState.DISCONNECTING -> VpnYellow
        ConnectionState.DISCONNECTED -> VpnRed
    }
}

/**
 * Top-centre PROTECTED / UNPROTECTED / CONNECTING… pill for the Servers screen
 * (parity with the iOS `StatusBannerView`).
 */
@Composable
fun ConnectionStatusBanner(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
    isReconnecting: Boolean = false,
    isSwitchingServer: Boolean = false,
) {
    val color = connectionStatusColor(connectionState, isReconnecting, isSwitchingServer)
    val label = connectionStatusLabel(connectionState, isReconnecting, isSwitchingServer)
    val transitioning = isReconnecting || isSwitchingServer ||
        connectionState == ConnectionState.CONNECTING ||
        connectionState == ConnectionState.DISCONNECTING

    val ringScale by rememberInfiniteTransition(label = "bannerRing").animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Restart),
        label = "bannerRingScale"
    )

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(VpnCardBackground.copy(alpha = 0.85f))
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (transitioning) {
                Box(
                    modifier = Modifier
                        .size(25.dp)
                        .scale(ringScale)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.25f))
                )
            }
            Box(
                modifier = Modifier
                    .size(25.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (connectionState == ConnectionState.CONNECTED) {
                        Icons.Default.Shield
                    } else {
                        Icons.Outlined.RemoveModerator
                    },
                    contentDescription = null,
                    tint = VpnBackground,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(9.dp))

        Text(
            text = label,
            color = color,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}
