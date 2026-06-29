package com.wiredog.vpn.data.repository

import com.wiredog.vpn.data.local.keystore.SecureStorage
import com.wiredog.vpn.data.logging.LogLevel
import com.wiredog.vpn.data.logging.LogService
import com.wiredog.vpn.data.remote.api.WireDogApi
import com.wiredog.vpn.data.remote.api.dto.AnonymousAccountRequest
import com.wiredog.vpn.data.remote.api.dto.AnonymousLoginRequest
import com.wiredog.vpn.data.remote.api.dto.ForgotPasswordRequest
import com.wiredog.vpn.data.remote.api.dto.ResetPasswordRequest
import com.wiredog.vpn.data.remote.api.dto.StandardAccountRequest
import com.wiredog.vpn.data.remote.api.dto.StandardLoginRequest
import com.wiredog.vpn.data.remote.api.dto.UserProfileDto
import com.wiredog.vpn.data.remote.api.dto.VerifyResetCodeRequest
import com.wiredog.vpn.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: WireDogApi,
    private val secureStorage: SecureStorage,
    private val logService: LogService
) {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        _isLoggedIn.value = secureStorage.hasAuthToken()
    }

    suspend fun loginStandard(email: String, password: String): Result<User> {
        logService.logApp("Standard login attempt: ${redactEmail(email)}")
        return try {
            val response = api.loginStandard(StandardLoginRequest(email, password))
            secureStorage.saveAuthToken(response.token)
            secureStorage.saveLoginIdentifier(email)

            val user = if (response.user != null) {
                response.user.toDomain()
            } else {
                val profile = api.getProfile()
                profile.toDomain()
            }

            _currentUser.value = user
            _isLoggedIn.value = true
            logService.logApp("Standard login success: ${redactEmail(email)}")
            Result.success(user)
        } catch (e: Exception) {
            logService.logApp("Standard login failed: ${redactEmail(email)} — ${e.message}", LogLevel.ERROR)
            Result.failure(e)
        }
    }

    suspend fun loginAnonymous(accountNumber: String): Result<User> {
        logService.logApp("Anonymous login attempt: ${redactAccountNumber(accountNumber)}")
        return try {
            val response = api.loginAnonymous(AnonymousLoginRequest(accountNumber))
            secureStorage.saveAuthToken(response.token)

            val user = if (response.user != null) {
                response.user.toDomain()
            } else {
                val profile = api.getProfile()
                profile.toDomain()
            }

            _currentUser.value = user
            _isLoggedIn.value = true
            logService.logApp("Anonymous login success: ${redactAccountNumber(accountNumber)}")
            Result.success(user)
        } catch (e: Exception) {
            logService.logApp("Anonymous login failed: ${redactAccountNumber(accountNumber)} — ${e.message}", LogLevel.ERROR)
            Result.failure(e)
        }
    }

    suspend fun registerStandard(email: String, password: String, referralCode: String?): Result<User> {
        logService.logApp("Standard registration attempt: ${redactEmail(email)}")
        return try {
            // Register — server returns message + accountNumber (no token)
            api.registerStandard(StandardAccountRequest(email, password, referralCode))
            logService.logApp("Standard registration success: ${redactEmail(email)}")

            // Auto-login to obtain token and full profile
            loginStandard(email, password)
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
                secureStorage.saveAuthToken(response.token)
                val user = response.user?.toDomain() ?: run {
                    val profile = api.getProfile()
                    profile.toDomain()
                }
                _currentUser.value = user
                _isLoggedIn.value = true
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

    suspend fun verifyResetCode(email: String, code: String): Result<Unit> {
        logService.logApp("Verify reset code: ${redactEmail(email)}")
        return try {
            api.verifyResetCode(VerifyResetCodeRequest(email, code))
            Result.success(Unit)
        } catch (e: Exception) {
            logService.logApp("Verify reset code failed: ${redactEmail(email)} — ${e.message}", LogLevel.ERROR)
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit> {
        logService.logApp("Password reset: ${redactEmail(email)}")
        return try {
            api.resetPassword(ResetPasswordRequest(email, code, newPassword))
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
            subscriptionEndDate = endDate
        )
    }
}
