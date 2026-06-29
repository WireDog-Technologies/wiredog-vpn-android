package com.wiredog.vpn.ui.screens.settings

import android.os.Build
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiredog.vpn.data.remote.api.dto.ReportBugRequest
import com.wiredog.vpn.ui.components.GradientActionButton
import com.wiredog.vpn.ui.theme.VpnBackground
import com.wiredog.vpn.ui.theme.VpnBorderColor
import com.wiredog.vpn.ui.theme.VpnCardBackground
import com.wiredog.vpn.ui.theme.VpnPrimary
import com.wiredog.vpn.ui.theme.VpnRedMedium
import com.wiredog.vpn.ui.theme.VpnTextPrimary
import com.wiredog.vpn.ui.theme.VpnTextSecondary
import kotlinx.coroutines.launch

@Composable
fun ReportBugScreen(
    onNavigateBack: () -> Unit,
    onSubmitBugReport: suspend (ReportBugRequest) -> Boolean,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showAlert by remember { mutableStateOf(false) }
    var alertTitle by remember { mutableStateOf("") }
    var alertMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val isFormValid = email.trim().isNotEmpty() &&
                      subject.trim().isNotEmpty() &&
                      description.trim().length >= 20

    val appVersion = "1.0.4"
    val osVersion = Build.VERSION.RELEASE

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VpnBackground)
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
                text = "Report a Bug",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = VpnTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Help us improve WireDog VPN",
                fontSize = 14.sp,
                color = VpnTextSecondary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Email Address",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VpnTextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Your email address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors()
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Subject",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VpnTextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    placeholder = { Text("Brief description of the bug") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors()
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Description",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VpnTextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Please be as descriptive as possible. Include steps to recreate the issue, what you expected to happen, and what actually happened.",
                    fontSize = 12.sp,
                    color = VpnTextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    colors = textFieldColors()
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                GradientActionButton(
                    title = if (isSubmitting) "Sending..." else "Submit Bug Report",
                    isLoading = isSubmitting,
                    isDisabled = !isFormValid || isSubmitting,
                    onClick = {
                        coroutineScope.launch {
                            isSubmitting = true
                            try {
                                val request = ReportBugRequest(
                                    email = email.trim(),
                                    subject = subject.trim(),
                                    message = description.trim(),
                                    osVersion = osVersion,
                                    vpnVersion = appVersion
                                )
                                val success = onSubmitBugReport(request)
                                if (success) {
                                    alertTitle = "Bug Report Submitted"
                                    alertMessage = "Thank you for helping us improve WireDog VPN. Our team will review your report shortly."
                                    email = ""
                                    subject = ""
                                    description = ""
                                } else {
                                    alertTitle = "Error"
                                    alertMessage = "Failed to submit bug report. Please try again."
                                }
                            } catch (e: Exception) {
                                alertTitle = "Error"
                                alertMessage = "Failed to submit bug report. Please try again."
                            } finally {
                                isSubmitting = false
                                showAlert = true
                            }
                        }
                    }
                )
            }
        }
    }

    if (showAlert) {
        AlertDialog(
            onDismissRequest = {
                showAlert = false
                if (alertTitle == "Bug Report Submitted") {
                    onNavigateBack()
                }
            },
            title = { Text(alertTitle) },
            text = { Text(alertMessage) },
            confirmButton = {
                Button(onClick = {
                    showAlert = false
                    if (alertTitle == "Bug Report Submitted") {
                        onNavigateBack()
                    }
                }) {
                    Text("OK")
                }
            }
        )
    }
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
