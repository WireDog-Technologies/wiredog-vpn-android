package com.wiredog.vpn.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiredog.vpn.ui.theme.VpnBackground
import com.wiredog.vpn.ui.theme.VpnBorderColor
import com.wiredog.vpn.ui.theme.VpnCardBackground
import com.wiredog.vpn.ui.theme.VpnRedMedium
import com.wiredog.vpn.ui.theme.VpnTextPrimary
import com.wiredog.vpn.ui.theme.VpnTextSecondary

@Composable
fun CreateAccountScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStandard: () -> Unit,
    onNavigateToAnonymous: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VpnBackground)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Logo section
        Image(
            painter = painterResource(id = com.wiredog.vpn.R.drawable.wiredog_minimal_navy_1024x1024),
            contentDescription = "WireDog VPN",
            modifier = Modifier.height(100.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Image(
            painter = painterResource(id = com.wiredog.vpn.R.drawable.wiredog_text_logo_1024),
            contentDescription = "WireDog VPN",
            modifier = Modifier.height(60.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Header
        Text(
            text = "Create an Account",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = VpnTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose your account type",
            fontSize = 14.sp,
            color = VpnTextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Standard Account card
        AccountOptionCard(
            title = "Standard Account",
            subtitle = "Email and password required",
            icon = {
                Icon(
                    imageVector = Icons.Filled.Email,
                    contentDescription = null,
                    tint = VpnRedMedium,
                    modifier = Modifier.size(24.dp)
                )
            },
            onClick = onNavigateToStandard
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Anonymous Account card
        AccountOptionCard(
            title = "Anonymous Account",
            subtitle = "16-digit account number only",
            icon = {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = VpnRedMedium,
                    modifier = Modifier.size(24.dp)
                )
            },
            onClick = onNavigateToAnonymous
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Info section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(VpnCardBackground)
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = VpnRedMedium,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "What's the difference?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VpnTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            BulletPoint("Standard: Use email and password to log in, ability to recover and manage account")
            Spacer(modifier = Modifier.height(6.dp))
            BulletPoint("Anonymous: Log in with only a 16-digit account number, maximum privacy")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Already have an account
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Already have an account?", fontSize = 13.sp, color = VpnTextSecondary)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Sign In",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = VpnRedMedium,
                modifier = Modifier.clickable { onNavigateBack() }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun AccountOptionCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(VpnCardBackground)
            .border(1.dp, VpnBorderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon box
        Column(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(VpnCardBackground),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            icon()
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = VpnTextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, fontSize = 13.sp, color = VpnTextSecondary)
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = VpnTextSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(text = "•", fontSize = 12.sp, color = VpnTextSecondary, modifier = Modifier.padding(end = 8.dp))
        Text(text = text, fontSize = 12.sp, color = VpnTextSecondary, lineHeight = 17.sp)
    }
}
