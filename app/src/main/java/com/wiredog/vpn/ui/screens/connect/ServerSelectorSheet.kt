package com.wiredog.vpn.ui.screens.connect

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiredog.vpn.domain.model.Server
import com.wiredog.vpn.ui.components.ServerCard
import com.wiredog.vpn.ui.theme.VpnBackground
import com.wiredog.vpn.ui.theme.VpnTextPrimary
import com.wiredog.vpn.ui.theme.VpnTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSelectorSheet(
    servers: List<Server>,
    selectedServer: Server?,
    onServerSelected: (Server) -> Unit,
    onToggleFavorite: (Server) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val favorites = servers.filter { it.isFavorite }
    val measured = servers.filter { it.latency > 0 }
    val recommended = if (measured.isNotEmpty()) {
        measured
            .filter { it.load < 80 }
            .sortedBy { it.latency + (it.load * 2) }
            .take(3)
    } else {
        // Latency not yet measured — fall back to backend flag
        servers.filter { it.isRecommended }.take(3)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VpnBackground,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            if (favorites.isNotEmpty()) {
                SelectorSection(title = "FAVORITES") {
                    favorites.forEach { server ->
                        ServerCard(
                            server = server,
                            isSelected = selectedServer?.id == server.id,
                            onSelect = { onServerSelected(server) },
                            onToggleFavorite = { onToggleFavorite(server) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (recommended.isNotEmpty()) {
                SelectorSection(title = "RECOMMENDED") {
                    recommended.forEach { server ->
                        ServerCard(
                            server = server,
                            isSelected = selectedServer?.id == server.id,
                            onSelect = { onServerSelected(server) },
                            onToggleFavorite = { onToggleFavorite(server) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            SelectorSection(title = "ALL SERVERS") {
                servers.forEach { server ->
                    ServerCard(
                        server = server,
                        isSelected = selectedServer?.id == server.id,
                        onSelect = { onServerSelected(server) },
                        onToggleFavorite = { onToggleFavorite(server) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SelectorSection(
    title: String,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = VpnTextSecondary,
        letterSpacing = 0.5.sp
    )
    Spacer(modifier = Modifier.height(8.dp))
    content()
}
