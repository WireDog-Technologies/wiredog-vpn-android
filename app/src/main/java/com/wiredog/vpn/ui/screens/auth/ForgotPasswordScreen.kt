package com.wiredog.vpn.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wiredog.vpn.ui.components.GradientActionButton
import com.wiredog.vpn.ui.components.SecureToggleField
import com.wiredog.vpn.ui.theme.VpnBackground
import com.wiredog.vpn.ui.theme.VpnBorderColor
import com.wiredog.vpn.ui.theme.VpnCardBackground
import com.wiredog.vpn.ui.theme.VpnGreen
import com.wiredog.vpn.ui.theme.VpnRed
import com.wiredog.vpn.ui.theme.VpnRedMedium
import com.wiredog.vpn.ui.theme.VpnTextPrimary
import com.wiredog.vpn.ui.theme.VpnTextSecondary

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VpnBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
        Row(
            modifier = Modifier.padding(start = 4.dp, top = 36.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = VpnTextPrimary
                )
            }
            Text(
                text = "Reset Password",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VpnTextPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            when (uiState.step) {
                ForgotPasswordStep.EMAIL -> EmailStep(uiState, viewModel)
                ForgotPasswordStep.VERIFY_CODE -> VerifyCodeStep(uiState, viewModel)
                ForgotPasswordStep.RESET_PASSWORD -> ResetPasswordStep(uiState, viewModel)
                ForgotPasswordStep.DONE -> DoneStep(onNavigateBack)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun EmailStep(uiState: ForgotPasswordUiState, viewModel: ForgotPasswordViewModel) {
    Text(
        text = "Enter the email associated with your account",
        fontSize = 14.sp,
        color = VpnTextSecondary,
        lineHeight = 20.sp
    )

    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(
        value = uiState.email,
        onValueChange = viewModel::onEmailChange,
        label = { Text("Email") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
        singleLine = true,
        colors = textFieldColors()
    )

    uiState.error?.let { ErrorText(it) }

    Spacer(modifier = Modifier.height(24.dp))

    GradientActionButton(
        title = "Send Reset Code",
        isLoading = uiState.isLoading,
        isDisabled = uiState.email.isBlank(),
        onClick = viewModel::submitEmail
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "If an account under this email exists, instructions to reset your password will be sent.",
        fontSize = 12.sp,
        color = VpnTextSecondary,
        lineHeight = 17.sp
    )
}

@Composable
private fun VerifyCodeStep(uiState: ForgotPasswordUiState, viewModel: ForgotPasswordViewModel) {
    Text(
        text = "Enter the 6-digit code sent to ${uiState.email}",
        fontSize = 14.sp,
        color = VpnTextSecondary,
        lineHeight = 20.sp
    )

    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(
        value = uiState.code,
        onValueChange = viewModel::onCodeChange,
        label = { Text("Verification Code") },
        placeholder = { Text("000000", color = VpnTextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        singleLine = true,
        colors = textFieldColors()
    )

    uiState.error?.let { ErrorText(it) }

    Spacer(modifier = Modifier.height(24.dp))

    GradientActionButton(
        title = "Verify Code",
        isLoading = uiState.isLoading,
        isDisabled = uiState.code.length != 6,
        onClick = viewModel::submitCode
    )
}

@Composable
private fun ResetPasswordStep(uiState: ForgotPasswordUiState, viewModel: ForgotPasswordViewModel) {
    Text(
        text = "Enter your new password.",
        fontSize = 14.sp,
        color = VpnTextSecondary
    )

    Spacer(modifier = Modifier.height(24.dp))

    SecureToggleField(
        label = "New Password",
        placeholder = "Enter new password",
        value = uiState.newPassword,
        onValueChange = viewModel::onNewPasswordChange,
        modifier = Modifier.fillMaxWidth(),
        imeAction = ImeAction.Next
    )

    Spacer(modifier = Modifier.height(16.dp))

    SecureToggleField(
        label = "Confirm Password",
        placeholder = "Re-enter new password",
        value = uiState.confirmPassword,
        onValueChange = viewModel::onConfirmPasswordChange,
        modifier = Modifier.fillMaxWidth(),
        imeAction = ImeAction.Done
    )

    uiState.error?.let { ErrorText(it) }

    Spacer(modifier = Modifier.height(24.dp))

    GradientActionButton(
        title = "Reset Password",
        isLoading = uiState.isLoading,
        isDisabled = uiState.newPassword.isBlank() || uiState.confirmPassword.isBlank(),
        onClick = viewModel::submitNewPassword
    )
}

@Composable
private fun DoneStep(onNavigateBack: () -> Unit) {
    Text(
        text = "Password reset successfully!",
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = VpnGreen
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "You can now sign in with your new password.",
        fontSize = 14.sp,
        color = VpnTextSecondary
    )

    Spacer(modifier = Modifier.height(32.dp))

    GradientActionButton(
        title = "Back to Sign In",
        isLoading = false,
        onClick = onNavigateBack
    )
}

@Composable
private fun ErrorText(error: String) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = error, color = VpnRed, fontSize = 14.sp)
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = VpnTextPrimary,
    unfocusedTextColor = VpnTextPrimary,
    focusedBorderColor = VpnRedMedium.copy(alpha = 0.6f),
    unfocusedBorderColor = VpnBorderColor,
    focusedLabelColor = VpnRedMedium.copy(alpha = 0.6f),
    unfocusedLabelColor = VpnTextSecondary,
    cursorColor = VpnRedMedium,
    focusedContainerColor = VpnCardBackground,
    unfocusedContainerColor = VpnCardBackground
)
