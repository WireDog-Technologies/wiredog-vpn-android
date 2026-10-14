package com.wiredog.vpn.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wiredog.vpn.ui.components.GradientActionButton
import com.wiredog.vpn.ui.components.SecureToggleField
import com.wiredog.vpn.ui.theme.VpnBackground
import com.wiredog.vpn.ui.theme.VpnRed
import com.wiredog.vpn.ui.theme.VpnTextPrimary
import com.wiredog.vpn.ui.theme.VpnTextSecondary

@Composable
fun ForcedPasswordChangeScreen(
    onSignOut: () -> Unit,
    viewModel: ForcedPasswordChangeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VpnBackground)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(72.dp))

        Text(
            text = "Choose a New Password",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = VpnTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your administrator gave you a temporary password. Replace it to continue. " +
                "Use at least 8 characters with a letter and a number.",
            fontSize = 14.sp,
            color = VpnTextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        SecureToggleField(
            label = "Temporary Password",
            placeholder = "Enter your temporary password",
            value = uiState.currentPassword,
            onValueChange = viewModel::onCurrentPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            imeAction = ImeAction.Next
        )
        Spacer(modifier = Modifier.height(16.dp))
        SecureToggleField(
            label = "New Password",
            placeholder = "Enter a new password",
            value = uiState.newPassword,
            onValueChange = viewModel::onNewPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            imeAction = ImeAction.Next
        )
        Spacer(modifier = Modifier.height(16.dp))
        SecureToggleField(
            label = "Confirm New Password",
            placeholder = "Re-enter the new password",
            value = uiState.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            imeAction = ImeAction.Done
        )

        uiState.error?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = it, color = VpnRed, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        GradientActionButton(
            title = "Change Password",
            isLoading = uiState.isLoading,
            isDisabled = uiState.currentPassword.isBlank() || uiState.newPassword.isBlank() || uiState.confirmPassword.isBlank(),
            onClick = viewModel::submit
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Sign Out",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = VpnTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !uiState.isLoading) { onSignOut() }
        )
    }
}
