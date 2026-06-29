package com.wiredog.vpn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiredog.vpn.ui.theme.VpnRedBright
import com.wiredog.vpn.ui.theme.VpnRedDark
import com.wiredog.vpn.ui.theme.VpnRedMedium
import com.wiredog.vpn.ui.theme.VpnTextPrimary

@Composable
fun GradientActionButton(
    title: String,
    isLoading: Boolean,
    isDisabled: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = !isLoading && !isDisabled
    val gradient = Brush.verticalGradient(
        colors = if (enabled) {
            listOf(VpnRedBright, VpnRedMedium, VpnRedDark)
        } else {
            listOf(
                VpnRedBright.copy(alpha = 0.5f),
                VpnRedMedium.copy(alpha = 0.5f),
                VpnRedDark.copy(alpha = 0.5f)
            )
        }
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(brush = gradient)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = VpnTextPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = VpnTextPrimary
            )
        }
    }
}
