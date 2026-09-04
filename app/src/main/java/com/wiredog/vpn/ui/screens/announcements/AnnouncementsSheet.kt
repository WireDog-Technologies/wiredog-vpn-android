package com.wiredog.vpn.ui.screens.announcements

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.HapticFeedbackConstants
import com.wiredog.vpn.domain.model.Announcement
import com.wiredog.vpn.domain.model.AnnouncementSeverity
import com.wiredog.vpn.ui.theme.VpnBackground
import com.wiredog.vpn.ui.theme.VpnCardBackground
import com.wiredog.vpn.ui.theme.VpnPrimary
import com.wiredog.vpn.ui.theme.VpnRed
import com.wiredog.vpn.ui.theme.VpnTextPrimary
import com.wiredog.vpn.ui.theme.VpnTextSecondary
import com.wiredog.vpn.ui.theme.VpnTextTertiary
import com.wiredog.vpn.ui.theme.VpnYellow
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsSheet(
    announcements: List<Announcement>,
    readIds: Set<String>,
    onMarkRead: (String) -> Unit,
    onMarkUnread: (String) -> Unit,
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
            Text(
                text = "Announcements",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VpnTextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (announcements.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MailOutline,
                        contentDescription = null,
                        tint = VpnTextSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "No announcements right now",
                        fontSize = 15.sp,
                        color = VpnTextSecondary
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    announcements.forEach { message ->
                        AnnouncementRow(
                            message = message,
                            isRead = readIds.contains(message.id),
                            onTap = { onMarkRead(message.id) },
                            onLongPress = { onMarkUnread(message.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AnnouncementRow(
    message: Announcement,
    isRead: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val view = LocalView.current
    val severityColor = message.severity.color()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isRead) VpnCardBackground else severityColor.copy(alpha = 0.12f))
            .border(
                width = 1.dp,
                color = if (isRead) Color.Transparent else severityColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            )
            .combinedClickable(
                onClick = onTap,
                onLongClick = {
                    if (isRead) {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onLongPress()
                    }
                }
            )
            .padding(12.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = message.severity.icon(),
            contentDescription = null,
            tint = if (isRead) VpnTextTertiary else severityColor,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = message.title,
                fontSize = 15.sp,
                fontWeight = if (isRead) FontWeight.Normal else FontWeight.Bold,
                color = if (isRead) VpnTextSecondary else VpnTextPrimary
            )
            Text(
                text = message.body,
                fontSize = 13.sp,
                color = VpnTextSecondary
            )
            message.startAt?.let {
                Text(
                    text = dateFormatter.format(it.atZone(ZoneId.systemDefault())),
                    fontSize = 11.sp,
                    color = VpnTextTertiary
                )
            }
        }

        if (!isRead) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(VpnRed)
            )
        }
    }
}

/**
 * Envelope icon + unread badge shown on the Connect screen (parity with the iOS
 * `ConnectView` announcements button). Pulses briefly when the unread count rises.
 */
@Composable
fun AnnouncementBell(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pulsing by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (pulsing) 1.2f else 1f, label = "bellPulse")

    LaunchedEffect(unreadCount) {
        if (unreadCount > 0) {
            pulsing = true
            kotlinx.coroutines.delay(1800)
            pulsing = false
        }
    }

    // Outer box is NOT clipped — the badge sits partly outside the icon bounds and a clip
    // here would carve it into a crescent. Extra end/top padding leaves room for the badge.
    Box(
        modifier = modifier
            .clickable(onClick = onClick, indication = null, interactionSource = remember { MutableInteractionSource() })
            .padding(start = 8.dp, end = 10.dp, top = 8.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Email,
            contentDescription = "Announcements",
            tint = VpnTextPrimary,
            modifier = Modifier
                .size(24.dp)
                .scale(scale)
        )
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-6).dp)
                    .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                    .background(VpnRed, CircleShape)
                    .border(1.5.dp, VpnBackground, CircleShape)
                    .padding(horizontal = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                    fontSize = 9.sp,
                    lineHeight = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun AnnouncementSeverity.color(): Color = when (this) {
    AnnouncementSeverity.INFO -> VpnPrimary
    AnnouncementSeverity.MAINTENANCE -> VpnYellow
    AnnouncementSeverity.INCIDENT -> VpnRed
}

private fun AnnouncementSeverity.icon(): ImageVector = when (this) {
    AnnouncementSeverity.INFO -> Icons.Filled.Info
    AnnouncementSeverity.MAINTENANCE -> Icons.Filled.Build
    AnnouncementSeverity.INCIDENT -> Icons.Filled.Warning
}
