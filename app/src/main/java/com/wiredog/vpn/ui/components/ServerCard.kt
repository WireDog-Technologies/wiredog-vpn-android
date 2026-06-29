package com.wiredog.vpn.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiredog.vpn.domain.model.Server
import com.wiredog.vpn.domain.model.ServerGroup
import com.wiredog.vpn.ui.theme.VpnBorderColor
import com.wiredog.vpn.ui.theme.VpnCardBackground
import com.wiredog.vpn.ui.theme.VpnGreen
import com.wiredog.vpn.ui.theme.VpnRed
import com.wiredog.vpn.ui.theme.VpnSecondaryBackground
import com.wiredog.vpn.ui.theme.VpnTextPrimary
import com.wiredog.vpn.ui.theme.VpnTextSecondary
import com.wiredog.vpn.ui.theme.VpnYellow

@Composable
fun ServerCard(
    server: Server,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) VpnSecondaryBackground else VpnCardBackground)
            .clickable(onClick = onSelect)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "\uD83C\uDDFA\uD83C\uDDF8",
            fontSize = 24.sp
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = server.state,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = VpnTextPrimary,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
            if (server.city.isNotBlank()) {
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = server.city,
                    fontSize = 13.sp,
                    color = VpnTextSecondary,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
            }
        }

        LoadIndicator(
            load = server.load,
            modifier = Modifier.size(30.dp)
        )

        val latencyColor = when {
            server.latency <= 0 -> VpnTextSecondary
            server.latency <= 80 -> VpnGreen
            server.latency <= 150 -> VpnYellow
            else -> VpnRed
        }

        Column(
            modifier = Modifier.width(38.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "${server.latency}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = latencyColor,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
            Text(
                text = "ms",
                fontSize = 10.sp,
                color = VpnTextSecondary,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }

        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                imageVector = if (server.isFavorite) {
                    Icons.Filled.Star
                } else {
                    Icons.Outlined.StarBorder
                },
                contentDescription = if (server.isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (server.isFavorite) VpnYellow else VpnTextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun StateCard(
    group: ServerGroup,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        label = "chevronRotation"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(VpnCardBackground)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "\uD83C\uDDFA\uD83C\uDDF8",
            fontSize = 24.sp
        )

        Text(
            text = group.state,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = VpnTextPrimary,
            modifier = Modifier.weight(1f),
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = VpnTextSecondary,
            modifier = Modifier
                .size(20.dp)
                .rotate(chevronRotation)
        )
    }
}

@Composable
fun ServerSubItem(
    server: Server,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) VpnSecondaryBackground else VpnCardBackground)
            .clickable(onClick = onSelect)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = server.city,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = VpnTextPrimary,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
            Text(
                text = server.id,
                fontSize = 11.sp,
                color = VpnTextSecondary,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }

        LoadIndicator(
            load = server.load,
            modifier = Modifier.size(26.dp)
        )

        val latencyColor = when {
            server.latency <= 0 -> VpnTextSecondary
            server.latency <= 80 -> VpnGreen
            server.latency <= 150 -> VpnYellow
            else -> VpnRed
        }

        Column(
            modifier = Modifier.width(34.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "${server.latency}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = latencyColor,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
            Text(
                text = "ms",
                fontSize = 9.sp,
                color = VpnTextSecondary,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }

        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(18.dp)
        ) {
            Icon(
                imageVector = if (server.isFavorite) {
                    Icons.Filled.Star
                } else {
                    Icons.Outlined.StarBorder
                },
                contentDescription = if (server.isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (server.isFavorite) VpnYellow else VpnTextSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun LoadIndicator(
    load: Int,
    modifier: Modifier = Modifier
) {
    val loadColor = when {
        load < 50 -> VpnGreen
        load < 80 -> VpnYellow
        else -> VpnRed
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidth = 2.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = VpnBorderColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )

            drawArc(
                color = loadColor,
                startAngle = -90f,
                sweepAngle = 360f * (load / 100f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Text(
            text = "$load%",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = VpnTextPrimary
        )
    }
}
