package com.wiredog.vpn.ui.screens.servers

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
import com.wiredog.vpn.domain.model.ServerGroup
import com.wiredog.vpn.ui.components.ServerSubItem
import com.wiredog.vpn.ui.theme.VpnBackground
import com.wiredog.vpn.ui.theme.VpnTextPrimary
import com.wiredog.vpn.ui.theme.VpnTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StateServersSheet(
    group: ServerGroup,
    selectedServer: Server?,
    onServerSelected: (Server) -> Unit,
    onToggleFavorite: (Server) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            // Header
            Text(
                text = group.state,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VpnTextPrimary
            )
            Text(
                text = "${group.servers.size} servers",
                fontSize = 14.sp,
                color = VpnTextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Server list
            group.servers.forEach { server ->
                ServerSubItem(
                    server = server,
                    isSelected = selectedServer?.id == server.id,
                    onSelect = {
                        onServerSelected(server)
                        onDismiss()
                    },
                    onToggleFavorite = { onToggleFavorite(server) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
