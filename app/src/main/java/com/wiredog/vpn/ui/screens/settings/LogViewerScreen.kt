package com.wiredog.vpn.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wiredog.vpn.ui.theme.VpnBackground
import com.wiredog.vpn.ui.theme.VpnPrimary
import com.wiredog.vpn.ui.theme.VpnRed
import com.wiredog.vpn.ui.theme.VpnTextPrimary
import com.wiredog.vpn.ui.theme.VpnTextSecondary
import com.wiredog.vpn.ui.theme.VpnYellow

@Composable
fun LogViewerScreen(
    modifier: Modifier = Modifier,
    viewModel: LogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VpnBackground)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = uiState.title,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = VpnTextPrimary,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy Logs",
                tint = VpnTextSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        clipboardManager.setText(AnnotatedString(uiState.logs))
                    }
                    .padding(4.dp)
            )

            Spacer(modifier = Modifier.size(8.dp))

            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh Logs",
                tint = VpnTextSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { viewModel.refreshLogs() }
                    .padding(4.dp)
            )

            Spacer(modifier = Modifier.size(8.dp))

            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share Logs",
                tint = VpnTextSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, uiState.logs)
                            putExtra(Intent.EXTRA_SUBJECT, uiState.title)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Logs"))
                    }
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.padding(top = 12.dp))

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VpnPrimary)
            }
        } else {
            val listState = rememberLazyListState()
            val logLines = uiState.logs.split("\n").filter { it.isNotBlank() }

            LaunchedEffect(uiState.logs) {
                if (logLines.isNotEmpty()) {
                    listState.animateScrollToItem(logLines.size - 1)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .background(Color(0xFF0A0E14), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(logLines) { line ->
                        Text(
                            text = line,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = colorForLogLevel(line),
                            lineHeight = 15.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

private fun colorForLogLevel(line: String): Color {
    return when {
        line.contains("[ERROR]") -> VpnRed
        line.contains("[WARNING]") -> VpnYellow
        line.contains("[DEBUG]") -> VpnTextSecondary
        else -> VpnTextPrimary
    }
}
