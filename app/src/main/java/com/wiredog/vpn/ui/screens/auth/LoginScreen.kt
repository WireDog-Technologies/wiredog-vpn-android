package com.wiredog.vpn.ui.screens.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wiredog.vpn.data.config.Config
import com.wiredog.vpn.ui.components.GradientActionButton
import com.wiredog.vpn.ui.components.SecureToggleField
import com.wiredog.vpn.ui.theme.VpnBackground
import com.wiredog.vpn.ui.theme.VpnBorderColor
import com.wiredog.vpn.ui.theme.VpnCardBackground
import com.wiredog.vpn.ui.theme.VpnRed
import com.wiredog.vpn.ui.theme.VpnRedBright
import com.wiredog.vpn.ui.theme.VpnRedDark
import com.wiredog.vpn.ui.theme.VpnRedMedium
import com.wiredog.vpn.ui.theme.VpnTextPrimary
import com.wiredog.vpn.ui.theme.VpnTextSecondary

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToCreateAccount: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isLoginSuccess) {
        if (uiState.isLoginSuccess) onLoginSuccess()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VpnBackground)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Image(
            painter = painterResource(id = com.wiredog.vpn.R.drawable.wiredog_minimal_navy_1024x1024),
            contentDescription = "WireDog VPN",
            modifier = Modifier.height(125.dp)
        )

        Image(
            painter = painterResource(id = com.wiredog.vpn.R.drawable.wiredog_text_logo_1024),
            contentDescription = "WireDog VPN",
            modifier = Modifier.height(75.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        TabRow(
            selectedTabIndex = uiState.selectedTabIndex,
            containerColor = VpnCardBackground,
            contentColor = VpnTextPrimary,
            indicator = { tabPositions ->
                Box(
                    Modifier
                        .tabIndicatorOffset(tabPositions[uiState.selectedTabIndex])
                        .height(3.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(VpnRedBright, VpnRedMedium, VpnRedDark)
                            )
                        )
                )
            }
        ) {
            Tab(
                selected = uiState.selectedTabIndex == 0,
                onClick = { viewModel.onTabSelected(0) },
                text = {
                    Text(
                        text = "Standard Login",
                        fontWeight = if (uiState.selectedTabIndex == 0) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            )
            Tab(
                selected = uiState.selectedTabIndex == 1,
                onClick = { viewModel.onTabSelected(1) },
                text = {
                    Text(
                        text = "Anonymous Login",
                        fontWeight = if (uiState.selectedTabIndex == 1) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        when (uiState.selectedTabIndex) {
            0 -> StandardLoginForm(
                email = uiState.email,
                password = uiState.password,
                error = uiState.error,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onLogin = viewModel::loginStandard,
                onCreateAccount = onNavigateToCreateAccount,
                onForgotPassword = onNavigateToForgotPassword,
                isLoading = uiState.isLoading
            )
            1 -> AnonymousLoginForm(
                accountNumber = uiState.accountNumber,
                error = uiState.error,
                onAccountNumberChange = viewModel::onAccountNumberChange,
                onLogin = viewModel::loginAnonymous,
                onCreateAccount = onNavigateToCreateAccount,
                isLoading = uiState.isLoading
            )
        }
    }
}

@Composable
private fun StandardLoginForm(
    email: String,
    password: String,
    error: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onCreateAccount: () -> Unit,
    onForgotPassword: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            colors = textFieldColors()
        )

        SecureToggleField(
            label = "Password",
            placeholder = "Enter your password",
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            imeAction = ImeAction.Done
        )

        error?.let {
            Text(text = it, color = VpnRed, fontSize = 14.sp)
        }

        GradientActionButton(
            title = "Sign In",
            isLoading = isLoading,
            isDisabled = email.isBlank() || password.isBlank(),
            onClick = onLogin,
            modifier = Modifier.padding(top = 8.dp)
        )

        GradientTextLink(
            text = "Create an Account",
            onClick = onCreateAccount
        )

        GradientTextLink(
            text = "Forgot Password?",
            onClick = onForgotPassword
        )

        Spacer(modifier = Modifier.height(32.dp))

        LegalText()

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun AnonymousLoginForm(
    accountNumber: TextFieldValue,
    error: String?,
    onAccountNumberChange: (TextFieldValue) -> Unit,
    onLogin: () -> Unit,
    onCreateAccount: () -> Unit,
    isLoading: Boolean
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = accountNumber,
            onValueChange = onAccountNumberChange,
            label = { Text("Account Number") },
            placeholder = { Text("XXXX-XXXX-XXXX-XXXX", color = VpnTextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            singleLine = true,
            colors = textFieldColors()
        )

        error?.let {
            Text(text = it, color = VpnRed, fontSize = 14.sp)
        }

        GradientActionButton(
            title = "Sign In",
            isLoading = isLoading,
            isDisabled = accountNumber.text.isBlank(),
            onClick = onLogin,
            modifier = Modifier.padding(top = 8.dp)
        )

        GradientTextLink(
            text = "Create an Account",
            onClick = onCreateAccount
        )

        Spacer(modifier = Modifier.height(32.dp))

        LegalText()

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun GradientTextLink(text: String, onClick: () -> Unit) {
    val gradient = Brush.verticalGradient(
        colors = listOf(VpnRedBright, VpnRedBright, VpnRedMedium, VpnRedDark)
    )
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        style = TextStyle(brush = gradient),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

@Composable
private fun LegalText() {
    val context = LocalContext.current
    val redStyle = SpanStyle(color = VpnRedMedium, fontWeight = FontWeight.SemiBold)

    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = VpnTextSecondary)) {
            append("By using this application you accept our\n")
        }
        pushStringAnnotation("terms", Config.termsOfServiceURL)
        withStyle(redStyle) { append("Terms of Service") }
        pop()
        withStyle(SpanStyle(color = VpnTextSecondary)) {
            append(" and acknowledge our ")
        }
        pushStringAnnotation("privacy", Config.privacyPolicyURL)
        withStyle(redStyle) { append("Privacy Policy") }
        pop()
    }

    androidx.compose.foundation.text.ClickableText(
        text = text,
        style = TextStyle(
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        ),
        modifier = Modifier.fillMaxWidth(),
        onClick = { offset ->
            text.getStringAnnotations("terms", offset, offset).firstOrNull()?.let {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item)))
            }
            text.getStringAnnotations("privacy", offset, offset).firstOrNull()?.let {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item)))
            }
        }
    )
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
