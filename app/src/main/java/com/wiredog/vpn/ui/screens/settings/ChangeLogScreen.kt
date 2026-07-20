package com.wiredog.vpn.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiredog.vpn.ui.theme.VpnBackground
import com.wiredog.vpn.ui.theme.VpnCardBackground
import com.wiredog.vpn.ui.theme.VpnPrimary
import com.wiredog.vpn.ui.theme.VpnSecondaryBackground
import com.wiredog.vpn.ui.theme.VpnTextPrimary
import com.wiredog.vpn.ui.theme.VpnTextSecondary

data class ChangeLogEntry(
    val version: String,
    val date: String,
    val changes: List<String>
)

private val changeLog = listOf(
    ChangeLogEntry(
        version = "1.1.0",
        date = "July 2026",
        changes = listOf(
            "Togglable DNS filters — Block Ads and Block Malware can now be switched on or off independently in Settings.",
            "Clearer error handling and messaging — Connection issues now show specific, actionable messages instead of generic failures."
        )
    ),
    ChangeLogEntry(
        version = "1.0.5",
        date = "June 2026",
        changes = listOf(
            "Real latency measurement — Live TCP probes with triple sampling replace server-reported latency values, improving recommendation accuracy.",
            "Protocol upgraded from WireGuard to AmneziaWG — Operates on port 443 with packet obfuscation for improved compatibility on restricted and censored networks."
        )
    ),
    ChangeLogEntry(
        version = "1.0.4",
        date = "May 2026",
        changes = listOf(
            "Initial release",
            "WireGuard protocol support only",
            "Kill Switch protection",
            "Auto-Connect on startup",
            "Interactive world map for server selection",
            "Server stats — load, speed, and latency per server",
            "Server Selector sheet with Favorites, Recommended, and All Servers sections",
            "Active connection stats sheet",
            "DNS leak protection",
            "IPv6 leak protection",
            "Split Tunneling"
        )
    )
)

@Composable
fun ChangeLogScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VpnBackground)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateBack() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Back",
                tint = VpnPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.padding(4.dp))
            Text(
                text = "Back",
                color = VpnPrimary,
                fontSize = 16.sp
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Change Log",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = VpnTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Release history and updates",
                fontSize = 14.sp,
                color = VpnTextSecondary
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            changeLog.forEach { entry ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(VpnCardBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(VpnSecondaryBackground)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "v${entry.version}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = VpnTextPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = entry.date,
                            fontSize = 13.sp,
                            color = VpnTextSecondary
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        entry.changes.forEach { change ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                androidx.compose.foundation.Canvas(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .padding(top = 8.dp)
                                ) {
                                    drawCircle(color = VpnPrimary)
                                }
                                Spacer(modifier = Modifier.padding(5.dp))
                                Text(
                                    text = change,
                                    fontSize = 14.sp,
                                    color = VpnTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
