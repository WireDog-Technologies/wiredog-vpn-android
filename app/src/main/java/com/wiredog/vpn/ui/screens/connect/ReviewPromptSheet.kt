package com.wiredog.vpn.ui.screens.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiredog.vpn.ui.theme.VpnBackground
import com.wiredog.vpn.ui.theme.VpnCardBackground
import com.wiredog.vpn.ui.theme.VpnTextPrimary
import com.wiredog.vpn.ui.theme.VpnTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewPromptSheet(
    onPositive: () -> Unit,
    onNegative: () -> Unit,
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
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Enjoying WireDog VPN?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VpnTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "We'd love to know how it's going.",
                fontSize = 14.sp,
                color = VpnTextSecondary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ReviewChoiceButton(
                    emoji = "👎",
                    label = "Not really",
                    modifier = Modifier.weight(1f),
                    onClick = onNegative
                )
                ReviewChoiceButton(
                    emoji = "👍",
                    label = "Loving it",
                    modifier = Modifier.weight(1f),
                    onClick = onPositive
                )
            }
        }
    }
}

@Composable
private fun ReviewChoiceButton(
    emoji: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(VpnCardBackground)
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp)
    ) {
        Text(text = emoji, fontSize = 32.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = VpnTextPrimary
        )
    }
}
