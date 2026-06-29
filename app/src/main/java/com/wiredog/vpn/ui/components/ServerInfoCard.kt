package com.wiredog.vpn.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiredog.vpn.domain.model.Server
import com.wiredog.vpn.ui.theme.VpnCardBackground
import com.wiredog.vpn.ui.theme.VpnGreen
import com.wiredog.vpn.ui.theme.VpnPrimary
import com.wiredog.vpn.ui.theme.VpnRed
import com.wiredog.vpn.ui.theme.VpnTextPrimary
import com.wiredog.vpn.ui.theme.VpnTextSecondary
import com.wiredog.vpn.ui.theme.VpnYellow

@Composable
fun ServerInfoCard(
    server: Server?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VpnCardBackground)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        if (server != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "\uD83C\uDDFA\uD83C\uDDF8",
                    fontSize = 32.sp
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = server.state,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VpnTextPrimary
                    )
                    if (server.city.isNotBlank()) {
                        Text(
                            text = server.city,
                            fontSize = 12.sp,
                            color = VpnTextSecondary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Server ID",
                        fontSize = 16.sp,
                        color = VpnTextSecondary
                    )
                    Text(
                        text = server.id,
                        fontSize = 14.sp,
                        fontFamily = IosevkaTermExtended,
                        color = VpnPrimary
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = VpnTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "No server selected",
                    fontSize = 16.sp,
                    color = VpnTextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Select server",
                    tint = VpnTextSecondary
                )
            }
        }
    }
}

@Composable
fun ServerLoadBadge(
    load: Int,
    modifier: Modifier = Modifier
) {
    val loadColor = when {
        load < 50 -> VpnGreen
        load < 80 -> VpnYellow
        else -> VpnRed
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(loadColor.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$load%",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = loadColor
        )
    }
}

@Composable
fun LocationInfoCard(
    city: String?,
    state: String?,
    ipAddress: String?,
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = VpnPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (city != null && state != null) "$city, $state" else "Unknown Location",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = VpnTextPrimary
                )
            }

            if (ipAddress != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ipAddress,
                    fontSize = 14.sp,
                    color = VpnTextSecondary
                )
            }
        }
    }
}
