package com.wiredog.vpn.ui.screens.auth

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiredog.vpn.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
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
    val selectedTabIndex: Int = 0
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

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

            authRepository.loginStandard(state.email, state.password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) }
                }
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
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) }
                }
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun isValidEmail(email: String): Boolean {
        return Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email)
    }

    private fun getErrorMessage(exception: Throwable, isAnonymous: Boolean): String {
        return when {
            exception is HttpException -> {
                when (exception.code()) {
                    400 -> "Bad Request"
                    401 -> if (isAnonymous) "Account number not found" else "Invalid email or password"
                    403 -> "Forbidden"
                    else -> "An error occurred. Please try again later."
                }
            }
            else -> "An error occurred. Please try again later."
        }
    }
}
