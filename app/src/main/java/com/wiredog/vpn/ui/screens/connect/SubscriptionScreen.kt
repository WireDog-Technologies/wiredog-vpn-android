package com.wiredog.vpn.ui.screens.connect

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.wiredog.vpn.R
import com.wiredog.vpn.data.config.Config
import com.wiredog.vpn.ui.components.GradientActionButton
import com.wiredog.vpn.ui.theme.VpnBackground
import com.wiredog.vpn.ui.theme.VpnBorderColor
import com.wiredog.vpn.ui.theme.VpnCardBackground
import com.wiredog.vpn.ui.theme.VpnRedBright
import com.wiredog.vpn.ui.theme.VpnRedDark
import com.wiredog.vpn.ui.theme.VpnRedMedium
import com.wiredog.vpn.ui.theme.VpnTextPrimary
import com.wiredog.vpn.ui.theme.VpnTextSecondary

@Composable
fun SubscriptionScreen(
    onDismiss: () -> Unit,
    onRetryAfterSubscription: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VpnBackground)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
        ) {
            // Dismiss button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .background(VpnCardBackground, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = VpnTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Logo
            Image(
                painter = painterResource(id = R.drawable.wiredog_vectorized_1024px),
                contentDescription = "WireDog",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(top = 10.dp, bottom = 20.dp),
                contentScale = ContentScale.Fit
            )

            // Title: "Save 30% with"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Save ",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = VpnTextPrimary
                    )
                    GradientText(
                        text = "50%",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        brush = Brush.verticalGradient(
                            colors = listOf(VpnRedBright, VpnRedMedium, VpnRedDark)
                        )
                    )
                    Text(
                        text = " with",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = VpnTextPrimary
                    )
                }
                Text(
                    text = "our 2-year plan",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = VpnTextPrimary
                )
            }

            // Pricing card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(VpnCardBackground, shape = RoundedCornerShape(12.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Price with gradient
                GradientText(
                    text = "$4.99/month",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    brush = Brush.verticalGradient(
                        colors = listOf(VpnRedBright, VpnRedMedium, VpnRedDark)
                    )
                )

                // Price comparison
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$239.76",
                        fontSize = 13.sp,
                        color = VpnTextSecondary,
                        textDecoration = TextDecoration.LineThrough
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "$119.76 for 24 months",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = VpnTextPrimary 
                    )
                }

                // Start Subscription button
                GradientActionButton(
                    title = "Start Subscription",
                    isLoading = false,
                    isDisabled = false,
                    onClick = {
                        try {
                            if (Config.loginURL.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Config.loginURL))
                                context.startActivity(intent)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SubscriptionScreen", "Failed to open URL", e)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // See All Plans button
                Button(
                    onClick = {
                        try {
                            if (Config.pricingURL.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Config.pricingURL))
                                context.startActivity(intent)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SubscriptionScreen", "Failed to open pricing", e)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .border(1.dp, VpnBorderColor, RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = VpnCardBackground)
                ) {
                    Text(
                        text = "See All Plans",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VpnTextPrimary
                    )
                }
            }

            // Money back guarantee
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = VpnRedMedium,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "30-Day Money Back Guarantee",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VpnTextSecondary
                )
            }

            // Back to connecting link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Back to Connecting",
                    fontSize = 14.sp,
                    color = VpnTextSecondary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable(enabled = true, onClick = onDismiss)
                )
            }
        }
    }
}

@Composable
private fun GradientText(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    brush: Brush
) {
    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        style = androidx.compose.ui.text.TextStyle(brush = brush)
    )
}
