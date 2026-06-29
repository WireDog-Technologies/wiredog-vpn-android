package com.wiredog.vpn.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiredog.vpn.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ForgotPasswordStep { EMAIL, VERIFY_CODE, RESET_PASSWORD, DONE }

data class ForgotPasswordUiState(
    val step: ForgotPasswordStep = ForgotPasswordStep.EMAIL,
    val email: String = "",
    val code: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        val sanitized = value
            .filter { it.code in 0x20..0x7E }
            .filter { it.isLetterOrDigit() || it in "@.+-_" }
            .take(128)
        _uiState.update { it.copy(email = sanitized, error = null) }
    }

    fun onCodeChange(value: String) {
        val sanitized = value.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(code = sanitized, error = null) }
    }

    fun onNewPasswordChange(value: String) {
        val sanitized = value.filter { it.code in 0x20..0x7E }.take(64)
        _uiState.update { it.copy(newPassword = sanitized, error = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        val sanitized = value.filter { it.code in 0x20..0x7E }.take(64)
        _uiState.update { it.copy(confirmPassword = sanitized, error = null) }
    }

    fun submitEmail() {
        val email = _uiState.value.email
        if (email.isBlank() || !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email)) {
            _uiState.update { it.copy(error = "Enter a valid email address") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.forgotPassword(email)
                .onSuccess { _uiState.update { it.copy(isLoading = false, step = ForgotPasswordStep.VERIFY_CODE) } }
                .onFailure { _uiState.update { it.copy(isLoading = false, error = "Failed to send reset email. Please try again.") } }
        }
    }

    fun submitCode() {
        val state = _uiState.value
        if (state.code.length != 6) {
            _uiState.update { it.copy(error = "Enter the 6-digit code") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.verifyResetCode(state.email, state.code)
                .onSuccess { _uiState.update { it.copy(isLoading = false, step = ForgotPasswordStep.RESET_PASSWORD) } }
                .onFailure { _uiState.update { it.copy(isLoading = false, error = "Invalid or expired code") } }
        }
    }

    fun submitNewPassword() {
        val state = _uiState.value
        if (state.newPassword.length < 8) {
            _uiState.update { it.copy(error = "Password must be at least 8 characters") }
            return
        }
        if (state.newPassword != state.confirmPassword) {
            _uiState.update { it.copy(error = "Passwords do not match") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.resetPassword(state.email, state.code, state.newPassword)
                .onSuccess { _uiState.update { it.copy(isLoading = false, step = ForgotPasswordStep.DONE) } }
                .onFailure { _uiState.update { it.copy(isLoading = false, error = "Password reset failed. Please try again.") } }
        }
    }
}
