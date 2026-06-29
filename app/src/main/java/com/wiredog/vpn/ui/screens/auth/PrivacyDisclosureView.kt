package com.wiredog.vpn.ui.screens.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiredog.vpn.data.config.Config
import com.wiredog.vpn.ui.components.GradientActionButton
import com.wiredog.vpn.ui.theme.VpnBackground
import com.wiredog.vpn.ui.theme.VpnBorderColor
import com.wiredog.vpn.ui.theme.VpnCardBackground
import com.wiredog.vpn.ui.theme.VpnGreen
import com.wiredog.vpn.ui.theme.VpnRedMedium
import com.wiredog.vpn.ui.theme.VpnTextPrimary
import com.wiredog.vpn.ui.theme.VpnTextSecondary

@Composable
fun PrivacyDisclosureView(onAccept: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VpnBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Title and subtitle
        Column(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Your Privacy, Our Commitment",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VpnTextPrimary
            )
            Text(
                text = "Before you continue, here's what you should know about how WireDog handles your data.",
                fontSize = 12.sp,
                color = VpnTextSecondary
            )
        }

        // Data disclosure card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(VpnCardBackground, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // What we collect
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(title = "WHAT WE COLLECT")

                DisclosureRow(
                    symbol = Icons.Filled.Check,
                    symbolColor = VpnGreen,
                    text = "Your account credentials (email or account number) for authentication"
                )

                DisclosureRow(
                    symbol = Icons.Filled.Check,
                    symbolColor = VpnGreen,
                    text = "The VPN server you choose to connect to"
                )

                DisclosureRow(
                    symbol = Icons.Filled.Check,
                    symbolColor = VpnGreen,
                    text = "A session ID assigned by our server (used to manage your connection, not linked to you)"
                )
            }

            // Divider
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = VpnBorderColor
            )

            // What we do NOT collect
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(title = "WHAT WE DO NOT COLLECT")

                DisclosureRow(
                    symbol = Icons.Filled.Close,
                    symbolColor = VpnRedMedium,
                    text = "Your browsing history or DNS queries"
                )

                DisclosureRow(
                    symbol = Icons.Filled.Close,
                    symbolColor = VpnRedMedium,
                    text = "Your bandwidth usage (stored locally on your device only)"
                )

                DisclosureRow(
                    symbol = Icons.Filled.Close,
                    symbolColor = VpnRedMedium,
                    text = "Your device identifiers or location"
                )

                DisclosureRow(
                    symbol = Icons.Filled.Close,
                    symbolColor = VpnRedMedium,
                    text = "Your public IP address (displayed locally only — not stored or shared by WireDog)"
                )
            }

            // Divider
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = VpnBorderColor
            )

            // Logs
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionHeader(title = "YOUR LOGS")

                Text(
                    text = "App and connection logs are stored on your device only. They are only shared with us if you choose to submit a support report.",
                    fontSize = 12.sp,
                    color = VpnTextSecondary,
                    lineHeight = 17.sp
                )
            }
        }

        // Privacy policy link
        Text(
            text = "Read our full Privacy Policy",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = VpnRedMedium,
            modifier = Modifier
                .padding(top = 12.dp, bottom = 20.dp)
                .clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Config.privacyPolicyURL)))
                }
        )

        // CTA Button
        GradientActionButton(
            title = "Agree & Continue",
            isLoading = false,
            isDisabled = false,
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun DisclosureRow(
    symbol: ImageVector,
    symbolColor: androidx.compose.ui.graphics.Color,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = symbol,
            contentDescription = null,
            tint = symbolColor,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(14.dp)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = VpnTextSecondary,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = VpnTextSecondary,
        letterSpacing = 0.3.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

fun hasAcceptedPrivacyDisclosure(context: Context): Boolean {
    val prefs = context.getSharedPreferences("wiredog_prefs", Context.MODE_PRIVATE)
    return prefs.getBoolean("hasAcceptedPrivacyDisclosure", false)
}

fun setPrivacyDisclosureAccepted(context: Context) {
    val prefs = context.getSharedPreferences("wiredog_prefs", Context.MODE_PRIVATE)
    prefs.edit().putBoolean("hasAcceptedPrivacyDisclosure", true).apply()
}
