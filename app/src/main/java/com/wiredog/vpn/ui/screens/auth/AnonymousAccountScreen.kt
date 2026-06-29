package com.wiredog.vpn.ui.screens.auth

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wiredog.vpn.ui.components.GradientActionButton
import com.wiredog.vpn.ui.theme.VpnBackground
import com.wiredog.vpn.ui.theme.VpnBorderColor
import com.wiredog.vpn.ui.theme.VpnCardBackground
import com.wiredog.vpn.ui.theme.VpnGreen
import com.wiredog.vpn.ui.theme.VpnPrimary
import com.wiredog.vpn.ui.theme.VpnRedMedium
import com.wiredog.vpn.ui.theme.VpnTextPrimary
import com.wiredog.vpn.ui.theme.VpnTextSecondary
import com.wiredog.vpn.ui.theme.VpnYellow

@Composable
fun AnonymousAccountScreen(
    onNavigateBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AnonymousAccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    if (uiState.error != null && uiState.accountNumber == null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Error") },
            text = { Text(uiState.error!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.createAccount() }) { Text("Try Again") }
            },
            dismissButton = {
                TextButton(onClick = onNavigateBack) { Text("Go Back") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VpnBackground)
    ) {
        if (uiState.accountNumber == null) {
            Row(
                modifier = Modifier.padding(start = 4.dp, top = 36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = VpnPrimary
                    )
                }
                Text(
                    text = "Back",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VpnPrimary,
                    modifier = Modifier.clickable { onNavigateBack() }
                )
            }
        } else {
            Spacer(modifier = Modifier.height(52.dp))
        }

        if (uiState.isGenerating) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = VpnPrimary,
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = "Creating Your Account",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VpnTextPrimary
                    )
                    Text(
                        text = "This will only take a moment...",
                        fontSize = 14.sp,
                        color = VpnTextSecondary
                    )
                }
            }
        } else if (uiState.accountNumber != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = VpnGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Account Created!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = VpnTextPrimary
                    )
                    Text(
                        text = "Your anonymous account is ready",
                        fontSize = 14.sp,
                        color = VpnTextSecondary
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(VpnCardBackground)
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = VpnYellow,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Keep This Number Safe",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VpnTextPrimary
                        )
                        Text(
                            text = "This account number is your only way to log in. Save it somewhere secure.",
                            fontSize = 12.sp,
                            color = VpnTextSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Your Account Number",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VpnTextSecondary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(VpnCardBackground)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatAccountNumber(uiState.accountNumber),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            color = VpnTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                copyToClipboard(context, uiState.accountNumber ?: "")
                                viewModel.onCopy()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy",
                                tint = VpnPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = uiState.showCopyMessage,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = "Copied to clipboard!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VpnGreen
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GradientActionButton(
                        title = "Got It",
                        isLoading = false,
                        onClick = viewModel::onGotIt
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, VpnRedMedium, RoundedCornerShape(8.dp))
                            .clickable { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Discard Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = VpnRedMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private fun formatAccountNumber(raw: String?): String {
    if (raw == null) return ""
    val digits = raw.filter { it.isDigit() }
    return if (digits.length == 16) digits.chunked(4).joinToString("-") else raw
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Account Number", text))

    // On pre-API 33 devices, clear the clipboard after 10 minutes.
    // Android 13+ (API 33) handles clipboard sensitivity natively.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Handler(Looper.getMainLooper()).postDelayed({
            clipboard.clearPrimaryClip()
        }, 10 * 60 * 1000L)
    }
}
