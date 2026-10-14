package com.wiredog.vpn.ui.screens.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiredog.vpn.data.config.Config
import com.wiredog.vpn.domain.model.LegalDocument
import com.wiredog.vpn.ui.components.GradientActionButton
import com.wiredog.vpn.ui.theme.VpnBackground
import com.wiredog.vpn.ui.theme.VpnRedMedium
import com.wiredog.vpn.ui.theme.VpnTextPrimary
import com.wiredog.vpn.ui.theme.VpnTextSecondary

/**
 * @param pending which documents the user must accept
 * @param isUpdate switches from the first-launch copy to "we have updated..." copy naming only
 * what changed
 */
@Composable
fun PrivacyDisclosureView(
    pending: Set<LegalDocument>,
    isUpdate: Boolean,
    summary: String?,
    onAccept: () -> Unit
) {
    val context = LocalContext.current
    val terms = LegalDocument.TERMS in pending
    val privacy = LegalDocument.PRIVACY in pending

    val title = when {
        !isUpdate -> "We value your privacy"
        terms && privacy -> "We've updated our Terms and Privacy Policy"
        terms -> "We've updated our Terms of Service"
        else -> "We've updated our Privacy Policy"
    }

    val redStyle = SpanStyle(color = VpnRedMedium, fontWeight = FontWeight.SemiBold)
    val agreement = remember(pending) {
        buildAnnotatedString {
            fun link(label: String, url: String) {
                pushStringAnnotation("url", url)
                withStyle(redStyle) { append(label) }
                pop()
            }
            append("By selecting “Accept,” ")
            when {
                terms && privacy -> {
                    append("you agree to our ")
                    link("Terms of Service", Config.termsOfServiceURL)
                    append(" and to how we handle data as explained in our ")
                    link("Privacy Policy", Config.privacyPolicyURL)
                }
                terms -> {
                    append("you agree to our updated ")
                    link("Terms of Service", Config.termsOfServiceURL)
                }
                else -> {
                    append("you agree to our updated ")
                    link("Privacy Policy", Config.privacyPolicyURL)
                }
            }
            append(".")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VpnBackground)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = VpnTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isUpdate) {
                if (!summary.isNullOrEmpty()) {
                    Text(
                        text = summary,
                        fontSize = 15.sp,
                        color = VpnTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                Text(
                    text = "We only collect the bare minimum of information required to offer a smooth and stable VPN experience.",
                    fontSize = 15.sp,
                    color = VpnTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Your browsing activity stays private. We never log the sites you visit or your DNS queries.",
                    fontSize = 15.sp,
                    color = VpnTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            ClickableText(
                text = agreement,
                style = TextStyle(
                    fontSize = 15.sp,
                    color = VpnTextSecondary,
                    textAlign = TextAlign.Center
                ),
                onClick = { offset ->
                    agreement.getStringAnnotations("url", offset, offset).firstOrNull()?.let {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item)))
                    }
                }
            )
        }

        GradientActionButton(
            title = "Accept",
            isLoading = false,
            isDisabled = false,
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
