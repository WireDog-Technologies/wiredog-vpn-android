package com.wiredog.vpn.ui.screens.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wiredog.vpn.ui.components.GradientActionButton
import com.wiredog.vpn.ui.theme.VpnBackground
import com.wiredog.vpn.ui.theme.VpnRed
import com.wiredog.vpn.ui.theme.VpnTextPrimary
import com.wiredog.vpn.ui.theme.VpnTextSecondary

@Composable
fun TwoFactorSetupRequiredScreen(
    onSignOut: () -> Unit,
    viewModel: TwoFactorSetupRequiredViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VpnBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(72.dp))

        Text(
            text = "Two-Factor Authentication Required",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = VpnTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your organization requires two-factor authentication. Set it up on your account " +
                "dashboard, then come back and continue.",
            fontSize = 14.sp,
            color = VpnTextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        GradientActionButton(
            title = "Set Up on Dashboard",
            isLoading = uiState.isOpeningDashboard,
            onClick = {
                viewModel.openDashboard { url ->
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) {
                        android.util.Log.e("TwoFactorSetup", "Failed to open dashboard", e)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        GradientActionButton(
            title = "I Have Set It Up",
            isLoading = uiState.isChecking,
            onClick = viewModel::checkAgain
        )

        uiState.error?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = it, color = VpnRed, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Sign Out",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = VpnTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSignOut() }
        )
    }
}
