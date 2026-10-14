package com.wiredog.vpn.data.repository

import com.wiredog.vpn.data.config.Config
import com.wiredog.vpn.data.local.keystore.SecureStorage
import com.wiredog.vpn.data.logging.LogLevel
import com.wiredog.vpn.data.logging.LogService
import com.wiredog.vpn.data.remote.api.WireDogApi
import com.wiredog.vpn.data.remote.api.toApiError
import com.wiredog.vpn.data.remote.api.dto.AnonymousAccountRequest
import com.wiredog.vpn.data.remote.api.dto.AnonymousLoginRequest
import com.wiredog.vpn.data.remote.api.dto.ChangePasswordRequest
import com.wiredog.vpn.data.remote.api.dto.LoginResponse
import com.wiredog.vpn.data.remote.api.dto.SsoExchangeRequest
import com.wiredog.vpn.data.remote.api.dto.TwoFactorVerifyRequest
import com.wiredog.vpn.data.remote.api.dto.ForgotPasswordRequest
import com.wiredog.vpn.data.remote.api.dto.ResetPasswordRequest
import com.wiredog.vpn.data.remote.api.dto.StandardAccountRequest
import com.wiredog.vpn.data.remote.api.dto.StandardLoginRequest
import com.wiredog.vpn.data.remote.api.dto.UserProfileDto
import com.wiredog.vpn.data.remote.api.dto.VerifyResetCodeRequest
import com.wiredog.vpn.domain.model.OrganizationAccess
import com.wiredog.vpn.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** What a password/account-number login produced: a session, or a pending 2FA challenge. */
sealed class LoginOutcome {
    data class SignedIn(val user: User) : LoginOutcome()
    data class TwoFactorRequired(val challengeToken: String) : LoginOutcome()
}

/** An auth failure with a message that is safe to show directly. */
class AuthException(val kind: Kind, message: String) : Exception(message) {
    enum class Kind { INVALID_TWO_FACTOR_CODE, TWO_FACTOR_CHALLENGE_EXPIRED, SSO_NOT_AVAILABLE, SSO_FAILED, SSO_NO_ACCOUNT, SSO_CANCELLED, PASSWORD_CHANGE_FAILED }

    companion object {
        fun invalidTwoFactorCode() = AuthException(Kind.INVALID_TWO_FACTOR_CODE, "That code didn't work. Check your authenticator app and try again, or use a recovery code.")
        fun challengeExpired() = AuthException(Kind.TWO_FACTOR_CHALLENGE_EXPIRED, "Your sign-in timed out. Please enter your password again.")
        fun ssoNotAvailable() = AuthException(Kind.SSO_NOT_AVAILABLE, "No single sign-on is set up for this email. Sign in with your password instead.")
        fun ssoFailed() = AuthException(Kind.SSO_FAILED, "Single sign-on didn't complete. Please try again.")
        fun ssoNoAccount() = AuthException(Kind.SSO_NO_ACCOUNT, "Your organization hasn't given this email access. Contact your administrator.")
        fun ssoCancelled() = AuthException(Kind.SSO_CANCELLED, "Single sign-on was cancelled.")
    }
}

@Singleton
class AuthRepository @Inject constructor(
    private val api: WireDogApi,
    private val secureStorage: SecureStorage,
    private val logService: LogService,
    private val okHttpClient: OkHttpClient
) {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // A 2FA challenge token between the password step and the code step. Memory only.
    @Volatile
    var pendingTwoFactorChallenge: String? = null

    init {
        _isLoggedIn.value = secureStorage.hasAuthToken()
    }

    /**
     * Shared tail of every successful sign-in: persist the token, then load the full profile.
     * Always fetches auth/me (not the login response's embedded user) so the organization and
     * 2FA gating fields are present.
     */
    private suspend fun completeLogin(response: LoginResponse): User {
        val token = response.token ?: throw IllegalStateException("Login response had no token")
        secureStorage.saveAuthToken(token)
        val user = try {
            api.getProfile().toDomain()
        } catch (e: Exception) {
            response.user?.toDomain() ?: throw e
        }
        pendingTwoFactorChallenge = null
        _currentUser.value = user
        _isLoggedIn.value = true
        return user
    }

    suspend fun loginStandard(email: String, password: String): Result<LoginOutcome> {
        logService.logApp("Standard login attempt: ${redactEmail(email)}")
        return try {
            val response = api.loginStandard(StandardLoginRequest(email, password))
            if (response.requiresTwoFactor && response.challengeToken != null) {
                pendingTwoFactorChallenge = response.challengeToken
                logService.logApp("Standard login: 2FA required")
                return Result.success(LoginOutcome.TwoFactorRequired(response.challengeToken))
            }
            secureStorage.saveLoginIdentifier(email)
            val user = completeLogin(response)
            logService.logApp("Standard login success: ${redactEmail(email)}")
            Result.success(LoginOutcome.SignedIn(user))
        } catch (e: Exception) {
            logService.logApp("Standard login failed (${e.javaClass.simpleName}): ${redactEmail(email)} — ${e.message}", LogLevel.ERROR)
            Result.failure(e)
        }
    }

    suspend fun loginAnonymous(accountNumber: String): Result<LoginOutcome> {
        logService.logApp("Anonymous login attempt: ${redactAccountNumber(accountNumber)}")
        return try {
            val response = api.loginAnonymous(AnonymousLoginRequest(accountNumber))
            if (response.requiresTwoFactor && response.challengeToken != null) {
                pendingTwoFactorChallenge = response.challengeToken
                logService.logApp("Anonymous login: 2FA required")
                return Result.success(LoginOutcome.TwoFactorRequired(response.challengeToken))
            }
            val user = completeLogin(response)
            logService.logApp("Anonymous login success: ${redactAccountNumber(accountNumber)}")
            Result.success(LoginOutcome.SignedIn(user))
        } catch (e: Exception) {
            logService.logApp("Anonymous login failed (${e.javaClass.simpleName}): ${redactAccountNumber(accountNumber)} — ${e.message}", LogLevel.ERROR)
            Result.failure(e)
        }
    }

    /**
     * Second step of a login that returned [LoginOutcome.TwoFactorRequired]. [code] is a 6-digit
     * authenticator code or a recovery code. The challenge survives a wrong code so the user can
     * retry; a timed-out challenge surfaces as [AuthException.Kind.TWO_FACTOR_CHALLENGE_EXPIRED].
     */
    suspend fun verifyTwoFactor(code: String): Result<User> {
        val challenge = pendingTwoFactorChallenge
            ?: return Result.failure(AuthException.challengeExpired())
        return try {
            val response = api.verifyTwoFactor(TwoFactorVerifyRequest(challenge, code))
            val user = completeLogin(response)
            logService.logApp("User logged in (2FA verified)")
            Result.success(user)
        } catch (e: Exception) {
            val apiError = e.toApiError()
            val mapped: Exception = when {
                apiError?.status == 401 && apiError.code == "two_factor_challenge_expired" -> {
                    pendingTwoFactorChallenge = null
                    AuthException.challengeExpired()
                }
                apiError?.status == 401 -> AuthException.invalidTwoFactorCode()
                else -> e
            }
            logService.logApp("2FA verify failed (${mapped.javaClass.simpleName}): ${apiError?.status ?: e.message}", LogLevel.ERROR)
            Result.failure(mapped)
        }
    }

    // SSO

    /**
     * Whether [email]'s domain has verified SSO. A failed lookup counts as "no SSO" so a network
     * blip never blocks a password login.
     */
    suspend fun ssoAvailable(email: String): Boolean {
        val trimmed = email.trim()
        if (!trimmed.contains("@")) return false
        return try {
            api.loginLookup(trimmed).sso
        } catch (e: Exception) {
            logService.logApp("SSO lookup failed: ${e.message}", LogLevel.WARNING)
            false
        }
    }

    /** Redeems the one-time code from the SSO callback (bound to the PKCE verifier). */
    suspend fun exchangeSsoCode(code: String, codeVerifier: String): Result<User> {
        return try {
            val response = api.ssoExchange(SsoExchangeRequest(code, codeVerifier))
            val user = completeLogin(response)
            logService.logApp("User logged in (SSO)")
            Result.success(user)
        } catch (e: Exception) {
            val status = e.toApiError()?.status
            logService.logApp("SSO exchange failed (${e.javaClass.simpleName}): ${status ?: e.message}", LogLevel.ERROR)
            Result.failure(if (status == 401) AuthException.ssoFailed() else e)
        }
    }

    // Forced password change

    /**
     * Admin-provisioned employees sign in with a temporary password and must replace it
     * ([User.mustChangePassword]). The backend clears the flag on success.
     */
    suspend fun changePassword(current: String, new: String): Result<Unit> {
        return try {
            api.changePassword(ChangePasswordRequest(current, new))
            fetchProfile()
            logService.logApp("Password changed")
            Result.success(Unit)
        } catch (e: Exception) {
            val status = e.toApiError()?.status
            logService.logApp("Password change failed: ${status ?: e.message}", LogLevel.ERROR)
            Result.failure(
                if (status == 400) AuthException(
                    AuthException.Kind.PASSWORD_CHANGE_FAILED,
                    "Couldn't change your password. Check your current password, and that the new one is at least 8 characters with a letter and a number."
                ) else e
            )
        }
    }

    suspend fun registerStandard(email: String, password: String, referralCode: String?): Result<User> {
        logService.logApp("Standard registration attempt: ${redactEmail(email)}")
        return try {
            // Register — server returns message + accountNumber (no token)
            api.registerStandard(StandardAccountRequest(email, password, referralCode))
            logService.logApp("Standard registration success: ${redactEmail(email)}")

            // Auto-login to obtain token and full profile
            loginStandard(email, password).mapCatching { outcome ->
                (outcome as? LoginOutcome.SignedIn)?.user
                    ?: throw IllegalStateException("Unexpected 2FA challenge after registration")
            }
        } catch (e: Exception) {
            logService.logApp("Standard registration failed: ${redactEmail(email)} — ${e.message}", LogLevel.ERROR)
            Result.failure(e)
        }
    }

    suspend fun registerAnonymous(): Result<String> {
        logService.logApp("Anonymous registration attempt")
        return try {
            val response = api.registerAnonymous(AnonymousAccountRequest())
            logService.logApp("Anonymous registration success: ${redactAccountNumber(response.accountNumber)}")

            // Auto-login if token provided
            if (response.token != null) {
                completeLogin(LoginResponse(token = response.token, user = response.user))
            }

            Result.success(response.accountNumber)
        } catch (e: Exception) {
            logService.logApp("Anonymous registration failed: ${e.message}", LogLevel.ERROR)
            Result.failure(e)
        }
    }

    suspend fun forgotPassword(email: String): Result<Unit> {
        logService.logApp("Forgot password request: ${redactEmail(email)}")
        return try {
            api.forgotPassword(ForgotPasswordRequest(email))
            logService.logApp("Forgot password email sent: ${redactEmail(email)}")
            Result.success(Unit)
        } catch (e: Exception) {
            logService.logApp("Forgot password failed: ${redactEmail(email)} — ${e.message}", LogLevel.ERROR)
            Result.failure(e)
        }
    }

    /** Returns true when the account has 2FA on, so the reset must also carry a 2FA code. */
    suspend fun verifyResetCode(email: String, code: String): Result<Boolean> {
        logService.logApp("Verify reset code: ${redactEmail(email)}")
        return try {
            val response = api.verifyResetCode(VerifyResetCodeRequest(email, code))
            Result.success(response.twoFactorRequired ?: false)
        } catch (e: Exception) {
            logService.logApp("Verify reset code failed: ${redactEmail(email)} — ${e.message}", LogLevel.ERROR)
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String, code: String, newPassword: String, twoFactorCode: String? = null): Result<Unit> {
        logService.logApp("Password reset: ${redactEmail(email)}")
        return try {
            api.resetPassword(ResetPasswordRequest(email, code, newPassword, twoFactorCode))
            logService.logApp("Password reset success: ${redactEmail(email)}")
            Result.success(Unit)
        } catch (e: Exception) {
            logService.logApp("Password reset failed: ${redactEmail(email)} — ${e.message}", LogLevel.ERROR)
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        logService.logApp("Logout requested")
        return try {
            api.logout()
            pendingTwoFactorChallenge = null
            secureStorage.deleteAuthToken()
            _currentUser.value = null
            _isLoggedIn.value = false
            logService.logApp("Logout success")
            Result.success(Unit)
        } catch (e: Exception) {
            // Even if API call fails, clear local state
            secureStorage.deleteAuthToken()
            _currentUser.value = null
            _isLoggedIn.value = false
            Result.success(Unit)
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        logService.logApp("Delete account requested")
        return try {
            api.deleteAccount()
            secureStorage.deleteAuthToken()
            _currentUser.value = null
            _isLoggedIn.value = false
            logService.logApp("Account deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            logService.logApp("Delete account failed: ${e.message}", LogLevel.ERROR)
            Result.failure(e)
        }
    }

    /**
     * Drops any pooled/keep-alive connections. Should be called before a request that must not
     * fail on a stale pooled socket left over from a recent network change (e.g. app relaunch
     * after switching Wi-Fi/cellular, or right after the VPN tunnel connects/disconnects).
     * Evicting the pool closes live sockets, which is itself blocking I/O — must run off the
     * main thread or it throws NetworkOnMainThreadException.
     */
    suspend fun resetConnections() = withContext(Dispatchers.IO) {
        okHttpClient.connectionPool.evictAll()
    }

    /**
     * Mints a one-time handoff code so the checkout website can recognize this
     * already-authenticated user without a second sign-in. The code travels as a URL
     * fragment (`#`) — never a query param — so it is never sent to the server or included
     * in a Referer header. Falls back to the public get-started funnel if the request fails
     * (e.g. offline), so a network hiccup never dead-ends the flow.
     */
    suspend fun checkoutUrl(): String = handoffUrl(Config.checkoutURL, Config.getStartedURL)

    /**
     * Same one-time-code handoff, landing on the account dashboard. Used for Manage Account and to
     * send an employee to turn on org-required 2FA without signing in a second time. Falls back to
     * the plain dashboard URL (which asks the user to sign in).
     */
    suspend fun dashboardHandoffUrl(): String = handoffUrl(Config.dashboardHandoffURL, Config.dashboardURL)

    private suspend fun handoffUrl(destination: String, fallback: String): String {
        return try {
            val response = api.handoffToken()
            "$destination#handoff=${response.token}"
        } catch (e: Exception) {
            logService.logApp("Handoff token request failed: ${e.message}", LogLevel.WARNING)
            fallback
        }
    }

    suspend fun fetchProfile(): Result<User> {
        return try {
            val profile = api.getProfile()
            val user = profile.toDomain()
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkSession(): Boolean {
        if (!secureStorage.hasAuthToken()) {
            _isLoggedIn.value = false
            return false
        }
        logService.logApp("Restoring session", LogLevel.DEBUG)
        // App launch/relaunch is a common place to land on a stale pooled connection from
        // before a network change — reset first so this fetch doesn't fail for that reason.
        resetConnections()
        return try {
            val profile = api.getProfile()
            _currentUser.value = profile.toDomain()
            _isLoggedIn.value = true
            logService.logApp("Session restored successfully", LogLevel.DEBUG)
            true
        } catch (e: Exception) {
            // AuthInterceptor already deletes the token on 401. If the token is still
            // present, this was a network error — keep the user logged in rather than
            // forcing them out on every connectivity blip.
            if (!secureStorage.hasAuthToken()) {
                logService.logApp("Session expired (401 from server)", LogLevel.WARNING)
                _isLoggedIn.value = false
                false
            } else {
                logService.logApp("Session check failed (network error): ${e.message} — keeping session", LogLevel.WARNING)
                _isLoggedIn.value = true
                true
            }
        }
    }

    // PII redaction helpers
    private fun redactEmail(email: String): String {
        val atIndex = email.indexOf('@')
        if (atIndex < 0) return "***"
        val local = email.substring(0, atIndex)
        val domain = email.substring(atIndex)
        return when {
            local.length <= 2 -> "${local.first()}***$domain"
            else -> "${local.first()}***${local.last()}$domain"
        }
    }

    private fun redactAccountNumber(accountNumber: String): String {
        val digits = accountNumber.filter { it.isDigit() }
        return if (digits.length >= 4) "****${digits.takeLast(4)}" else "****"
    }

    private fun UserProfileDto.toDomain(): User {
        val endDate = subscriptionExpiresAt?.let {
            try {
                LocalDate.parse(it.take(10), DateTimeFormatter.ISO_DATE)
            } catch (e: Exception) {
                null
            }
        }

        val displayUsername = if (accountType == "anonymous") {
            "Anonymous"
        } else {
            val name = username
                ?: secureStorage.getLoginIdentifier()
                ?: "User"
            if (name.contains("@")) name.substringBefore("@") else name
        }

        return User(
            id = id,
            username = displayUsername,
            accountNumber = accountNumber ?: "",
            accountType = accountType,
            subscriptionPlan = planTier?.replaceFirstChar { it.uppercase() } ?: "Free",
            subscriptionEndDate = endDate,
            isActive = isActive,
            totpEnabled = totpEnabled,
            twoFactorRequiredByOrg = twoFactorRequiredByOrg,
            mustChangePassword = mustChangePassword,
            subscriptionManagedByOrganization = subscriptionManagedByOrganization,
            organizationName = organizationName,
            organizationRole = organizationRole,
            organizationAccess = OrganizationAccess.fromApi(organizationAccess)
        )
    }
}
