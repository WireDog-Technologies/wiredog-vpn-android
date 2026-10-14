package com.wiredog.vpn.data.repository

import android.net.Uri
import com.wiredog.vpn.data.config.Config
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Native SAML SSO for Business employees. The app opens the backend's auth/sso/login in a Custom
 * Tab; the backend bounces to the org's IdP, validates the assertion, and redirects to
 * wiredog://sso?code=... (or ?error=...). That code is bound to a PKCE challenge generated here,
 * so another app that registers the same URL scheme and sees the code cannot redeem it: only the
 * verifier, which never leaves this process, completes the exchange.
 * Backend counterpart: wiredog-backend/src/routes/auth.ts (/sso/login, /sso/acs, /sso/exchange).
 */
@Singleton
class SsoManager @Inject constructor() {

    sealed interface Callback {
        data class Code(val code: String, val codeVerifier: String) : Callback
        data object NoAccount : Callback
        data object Failed : Callback
    }

    // Buffered so a callback that lands a moment before the login screen is collecting isn't lost.
    private val results = Channel<Callback>(Channel.BUFFERED)
    val callbacks: Flow<Callback> = results.receiveAsFlow()

    // Held only while a sign-in is in progress; memory only.
    @Volatile
    private var pendingVerifier: String? = null

    /** Starts a sign-in for [domain] and returns the URL to open in the browser. */
    fun begin(domain: String): String {
        // Drop anything left over from an abandoned attempt.
        while (results.tryReceive().isSuccess) { /* drain */ }
        val verifier = makeCodeVerifier()
        pendingVerifier = verifier
        return Uri.parse("${Config.apiBaseURL}/auth/sso/login").buildUpon()
            .appendQueryParameter("domain", domain)
            .appendQueryParameter("challenge", codeChallenge(verifier))
            .build()
            .toString()
    }

    fun cancel() {
        pendingVerifier = null
    }

    /**
     * Handles a wiredog://sso deep link. Returns true if [uri] was an SSO callback (even one we
     * ignore). A callback with no sign-in in progress is dropped: nothing here asked for it.
     */
    fun handleCallback(uri: Uri): Boolean {
        if (uri.scheme != "wiredog" || uri.host != "sso") return false
        val verifier = pendingVerifier ?: return true
        pendingVerifier = null

        val error = uri.getQueryParameter("error")
        val code = uri.getQueryParameter("code")
        val result: Callback = when {
            error != null -> if (error == "sso_no_account") Callback.NoAccount else Callback.Failed
            code != null && code.length == CODE_LENGTH -> Callback.Code(code, verifier)
            else -> Callback.Failed
        }
        results.trySend(result)
        return true
    }

    companion object {
        private const val CODE_LENGTH = 64

        /** RFC 7636: 43 base64url characters from 32 random bytes. */
        fun makeCodeVerifier(): String {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            return base64Url(bytes)
        }

        /** RFC 7636 S256 challenge. */
        fun codeChallenge(verifier: String): String =
            base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

        private fun base64Url(bytes: ByteArray): String =
            Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
