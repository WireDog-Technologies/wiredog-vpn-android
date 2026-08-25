package com.wiredog.vpn.ui.screens.servers

import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.wiredog.vpn.R
import com.wiredog.vpn.domain.model.ConnectionState
import com.wiredog.vpn.domain.model.Server
import com.wiredog.vpn.ui.theme.VpnGreen
import com.wiredog.vpn.ui.theme.VpnPrimary
import com.wiredog.vpn.ui.theme.VpnRed
import com.wiredog.vpn.ui.theme.VpnYellow
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun ServerMapView(
    servers: List<Server>,
    selectedServer: Server?,
    connectionState: ConnectionState,
    isSwitchingServer: Boolean,
    onServerSelected: (Server) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Pulse animation for markers
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Map servers to their positions using hardcoded city positions
    val serverPositions = remember(servers) {
        servers.mapNotNull { server ->
            ServerMapPosition.getPosition(server.city)?.let { pos ->
                server to pos
            }
        }
    }

    val zoomLevel = 2.5f
    val animDuration = 400
    val easeOutCubic = androidx.compose.animation.core.CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

    val selectedPosition = selectedServer?.let { server ->
        ServerMapPosition.getPosition(server.city)
    }

    val targetScale = if (selectedPosition != null) zoomLevel else 1f
    val targetSvgX = selectedPosition?.x ?: (ServerMapPosition.SVG_WIDTH / 2f)
    val targetSvgY = selectedPosition?.y ?: (ServerMapPosition.SVG_HEIGHT / 2f)

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = animDuration, easing = easeOutCubic),
        label = "mapScale"
    )
    val animatedSvgX by animateFloatAsState(
        targetValue = targetSvgX,
        animationSpec = tween(durationMillis = animDuration, easing = easeOutCubic),
        label = "mapSvgX"
    )
    val animatedSvgY by animateFloatAsState(
        targetValue = targetSvgY,
        animationSpec = tween(durationMillis = animDuration, easing = easeOutCubic),
        label = "mapSvgY"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ServerMapPosition.SVG_WIDTH / ServerMapPosition.SVG_HEIGHT)
            .graphicsLayer {
                val s = animatedScale
                scaleX = s
                scaleY = s
                clip = true

                // Convert SVG target point to pixel coordinates
                val px = animatedSvgX / ServerMapPosition.SVG_WIDTH * size.width
                val py = animatedSvgY / ServerMapPosition.SVG_HEIGHT * size.height
                val cx = size.width / 2f
                val cy = size.height / 2f

                // Translate so the target point is at center, clamped to map bounds
                val maxTx = cx * (s - 1f)
                val maxTy = cy * (s - 1f)
                translationX = (s * (cx - px)).coerceIn(-maxTx, maxTx)
                translationY = (s * (cy - py)).coerceIn(-maxTy, maxTy)
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(R.raw.us_map)
                .decoderFactory(SvgDecoder.Factory())
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        renderEffect =
                            android.graphics.RenderEffect
                                .createBlurEffect(
                                    20f,
                                    20f,
                                    android.graphics.Shader.TileMode.CLAMP
                                )
                                .asComposeRenderEffect()
                    }
                    translationY = 8.dp.toPx()
                    alpha = 0.25f
                },
            colorFilter = ColorFilter.tint(Color(0xFFB3212C))
        )

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(R.raw.us_map)
                .decoderFactory(SvgDecoder.Factory())
                .build(),
            contentDescription = "US Map",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(serverPositions) {
                    detectTapGestures { tapOffset ->
                        val scaleX = size.width / ServerMapPosition.SVG_WIDTH
                        val scaleY = size.height / ServerMapPosition.SVG_HEIGHT

                        val tapRadius = 60f * scaleX // Generous tap target
                        var closestServer: Server? = null
                        var closestDistance = Float.MAX_VALUE

                        serverPositions.forEach { (server, position) ->
                            val markerX = position.x * scaleX
                            val markerY = position.y * scaleY
                            val distance = sqrt(
                                (tapOffset.x - markerX).pow(2) +
                                (tapOffset.y - markerY).pow(2)
                            )
                            if (distance < tapRadius && distance < closestDistance) {
                                closestDistance = distance
                                closestServer = server
                            }
                        }

                        closestServer?.let { onServerSelected(it) }
                    }
                }
        ) {
            val scaleX = size.width / ServerMapPosition.SVG_WIDTH
            val scaleY = size.height / ServerMapPosition.SVG_HEIGHT

            serverPositions.forEach { (server, position) ->
                val isSelected = selectedServer?.city == server.city
                // Marker color reflects live connection status: green only while actually
                // connected, gold while a connect/disconnect/switch is in flight, red when this
                // server is selected but there's no live tunnel at all. Non-selected markers
                // keep the default blue regardless of state.
                val markerColor = when {
                    !isSelected -> VpnPrimary
                    isSwitchingServer -> VpnYellow
                    connectionState == ConnectionState.CONNECTED -> VpnGreen
                    connectionState == ConnectionState.CONNECTING ||
                        connectionState == ConnectionState.DISCONNECTING -> VpnYellow
                    else -> VpnRed
                }
                drawServerMarker(
                    center = Offset(position.x * scaleX, position.y * scaleY),
                    color = markerColor,
                    pulseScale = pulseScale,
                    pulseAlpha = pulseAlpha,
                    scale = scaleX
                )
            }
        }
    }
}

private fun DrawScope.drawServerMarker(
    center: Offset,
    color: Color,
    pulseScale: Float,
    pulseAlpha: Float,
    scale: Float
) {
    val baseColor = color
    val outerRadius = 20f * scale
    val innerRadius = 8f * scale

    // Pulsing outer ring
    drawCircle(
        color = baseColor.copy(alpha = pulseAlpha),
        radius = outerRadius * pulseScale,
        center = center
    )

    // Static outer ring
    drawCircle(
        color = baseColor.copy(alpha = 0.3f),
        radius = outerRadius,
        center = center,
        style = Stroke(width = 2f * scale)
    )

    // Inner filled dot
    drawCircle(
        color = baseColor,
        radius = innerRadius,
        center = center
    )
}

@Composable
fun StarPatternOverlay(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val starSize = 6.dp.toPx()
        val spacing = 30.dp.toPx()

        val cols = (size.width / spacing).toInt() + 1
        val rows = (size.height / spacing).toInt() + 1

        for (row in 0..rows) {
            for (col in 0..cols) {
                val x = col * spacing
                val y = row * spacing
                drawStar(
                    center = Offset(x, y),
                    size = starSize,
                    color = Color.White.copy(alpha = 0.025f)
                )
            }
        }
    }
}

private fun DrawScope.drawStar(
    center: Offset,
    size: Float,
    color: Color
) {
    val path = Path()
    val outerRadius = size
    val innerRadius = size / 2.4f
    val points = 5

    for (i in 0 until points * 2) {
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        val angle = Math.PI / 2 + (i * Math.PI / points)
        val x = center.x + (radius * cos(angle)).toFloat()
        val y = center.y - (radius * sin(angle)).toFloat()

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()

    drawPath(path = path, color = color)
}
