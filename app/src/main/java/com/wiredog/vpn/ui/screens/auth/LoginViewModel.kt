package com.wiredog.vpn.ui.screens.auth

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiredog.vpn.data.remote.api.toApiError
import com.wiredog.vpn.data.repository.AuthException
import com.wiredog.vpn.data.repository.AuthRepository
import com.wiredog.vpn.data.repository.LoginOutcome
import com.wiredog.vpn.data.repository.SsoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoginSuccess: Boolean = false,
    // Standard login fields
    val email: String = "",
    val password: String = "",
    // Anonymous login field
    val accountNumber: TextFieldValue = TextFieldValue(""),
    // Tab selection
    val selectedTabIndex: Int = 0,
    // One-shot events consumed by the screen
    val ssoUrlToLaunch: String? = null,
    val navigateToTwoFactor: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val ssoManager: SsoManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // An SSO sign-in is waiting on the browser; leftApp flips once the browser actually took focus.
    private var ssoInFlight = false
    private var ssoLeftApp = false

    init {
        viewModelScope.launch {
            ssoManager.callbacks.collect { callback ->
                when (callback) {
                    is SsoManager.Callback.Code -> {
                        authRepository.exchangeSsoCode(callback.code, callback.codeVerifier)
                            .onSuccess { finishSso(success = true) }
                            .onFailure { e -> finishSso(error = getErrorMessage(e, isAnonymous = false)) }
                    }
                    SsoManager.Callback.NoAccount -> finishSso(error = AuthException.ssoNoAccount().message)
                    SsoManager.Callback.Failed -> finishSso(error = AuthException.ssoFailed().message)
                }
            }
        }
    }

    fun onEmailChange(email: String) {
        val sanitized = email
            .filter { it.code in 0x20..0x7E }
            .filter { it.isLetterOrDigit() || it in "@.+-_" }
            .take(128)
        _uiState.update { it.copy(email = sanitized, error = null) }
    }

    fun onPasswordChange(password: String) {
        val sanitized = password
            .filter { it.code in 0x20..0x7E }
            .take(64)
        _uiState.update { it.copy(password = sanitized, error = null) }
    }

    fun onAccountNumberChange(newValue: TextFieldValue) {
        val raw = newValue.text.filter { it.isDigit() }.take(16)
        val formatted = raw.chunked(4).joinToString("-")

        // Count how many digits were before the cursor in the typed text
        val rawCursorCount = newValue.text
            .take(newValue.selection.end)
            .count { it.isDigit() }
            .coerceAtMost(raw.length)

        // Find that same count in the formatted string to place the cursor correctly
        var digitSeen = 0
        var newCursor = formatted.length
        for (i in formatted.indices) {
            if (digitSeen == rawCursorCount) {
                newCursor = i
                break
            }
            if (formatted[i].isDigit()) digitSeen++
        }

        _uiState.update {
            it.copy(accountNumber = TextFieldValue(formatted, TextRange(newCursor)), error = null)
        }
    }

    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index, error = null) }
    }

    fun loginStandard() {
        val state = _uiState.value

        if (state.email.isBlank()) {
            _uiState.update { it.copy(error = "Email is required") }
            return
        }
        if (!isValidEmail(state.email)) {
            _uiState.update { it.copy(error = "Enter a valid email address") }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(error = "Password is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // SSO members have no password, so a password attempt on an SSO domain could only ever
            // fail with "Invalid credentials". Check first and route them to SSO instead. A failed
            // lookup counts as "no SSO", so a network blip never blocks password login.
            if (authRepository.ssoAvailable(state.email)) {
                beginSso(state.email)
                return@launch
            }

            authRepository.loginStandard(state.email, state.password)
                .onSuccess { handle(it) }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = getErrorMessage(e, isAnonymous = false)
                        )
                    }
                }
        }
    }

    fun loginAnonymous() {
        val state = _uiState.value

        val accountText = state.accountNumber.text
        if (accountText.isBlank()) {
            _uiState.update { it.copy(error = "Account number is required") }
            return
        }
        if (!accountText.matches(Regex("^[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{4}$"))) {
            _uiState.update { it.copy(error = "Account number must be 16 digits in format XXXX-XXXX-XXXX-XXXX") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val cleanAccountNumber = accountText.replace(Regex("[\\s-]"), "")
            authRepository.loginAnonymous(cleanAccountNumber)
                .onSuccess { handle(it) }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = getErrorMessage(e, isAnonymous = true)
                        )
                    }
                }
        }
    }

    /** A signed-in outcome just flips success; only the 2FA challenge needs another screen. */
    private fun handle(outcome: LoginOutcome) {
        when (outcome) {
            is LoginOutcome.SignedIn ->
                _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) }
            is LoginOutcome.TwoFactorRequired ->
                _uiState.update { it.copy(isLoading = false, navigateToTwoFactor = true) }
        }
    }

    fun onTwoFactorNavigated() {
        _uiState.update { it.copy(navigateToTwoFactor = false) }
    }

    // SSO

    /** The login screen goes straight to SSO only when the email field already holds an email. */
    fun hasValidEmail(): Boolean = isValidEmail(_uiState.value.email)

    /** Starts SSO for the typed email: it must be an email whose domain has SSO set up. */
    fun signInWithSso() {
        val email = _uiState.value.email
        if (!isValidEmail(email)) {
            _uiState.update { it.copy(error = "Enter a valid work email address") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            if (!authRepository.ssoAvailable(email)) {
                _uiState.update { it.copy(isLoading = false, error = AuthException.ssoNotAvailable().message) }
                return@launch
            }
            beginSso(email)
        }
    }

    private fun beginSso(email: String) {
        val domain = email.substringAfter('@').lowercase()
        val url = ssoManager.begin(domain)
        ssoInFlight = true
        ssoLeftApp = false
        _uiState.update { it.copy(ssoUrlToLaunch = url) }
    }

    fun onSsoLaunched() {
        _uiState.update { it.copy(ssoUrlToLaunch = null) }
    }

    /** No browser could open the sign-in page. */
    fun onSsoLaunchFailed() {
        ssoManager.cancel()
        finishSso(error = AuthException.ssoFailed().message)
    }

    private fun finishSso(success: Boolean = false, error: String? = null) {
        ssoInFlight = false
        ssoLeftApp = false
        _uiState.update { it.copy(isLoading = false, isLoginSuccess = success, error = error) }
    }

    /** Called from the screen's lifecycle: the browser (or anything else) took focus. */
    fun onScreenPaused() {
        if (ssoInFlight) ssoLeftApp = true
    }

    /**
     * Custom Tabs give no cancel callback, so coming back to the app with no callback is treated
     * as the user closing the tab. A real callback arrives (via onNewIntent) just before resume,
     * so wait a moment for it before declaring a cancel.
     */
    fun onScreenResumed() {
        if (!ssoInFlight || !ssoLeftApp) return
        ssoLeftApp = false
        viewModelScope.launch {
            delay(1500)
            if (ssoInFlight && !ssoLeftApp) {
                ssoManager.cancel()
                finishSso(error = AuthException.ssoCancelled().message)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun isValidEmail(email: String): Boolean {
        return Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email)
    }

    private fun getErrorMessage(exception: Throwable, isAnonymous: Boolean): String {
        if (exception is AuthException) return exception.message ?: GENERIC_ERROR
        val apiError = exception.toApiError()
        return when {
            apiError != null -> when (apiError.status) {
                400 -> apiError.message ?: "Bad Request"
                401 -> if (isAnonymous) "Account number not found" else "Invalid email or password"
                403 -> apiError.message ?: "Forbidden"
                else -> apiError.message ?: GENERIC_ERROR
            }
            else -> GENERIC_ERROR
        }
    }

    private companion object {
        const val GENERIC_ERROR = "An error occurred. Please try again later."
    }
}
