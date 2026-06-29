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
import retrofit2.HttpException
import javax.inject.Inject

data class StandardAccountUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val referralCode: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class StandardAccountViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StandardAccountUiState())
    val uiState: StateFlow<StandardAccountUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        val sanitized = value
            .filter { it.code in 0x20..0x7E }
            .filter { it.isLetterOrDigit() || it in "@.+-_" }
            .take(128)
        _uiState.update { it.copy(email = sanitized, error = null) }
    }

    fun onPasswordChange(value: String) {
        val sanitized = value.filter { it.code in 0x20..0x7E }.take(64)
        _uiState.update { it.copy(password = sanitized, error = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        val sanitized = value.filter { it.code in 0x20..0x7E }.take(64)
        _uiState.update { it.copy(confirmPassword = sanitized, error = null) }
    }

    fun onReferralCodeChange(value: String) {
        val sanitized = value.filter { it.isLetterOrDigit() }.uppercase().take(8)
        _uiState.update { it.copy(referralCode = sanitized, error = null) }
    }

    fun register() {
        val state = _uiState.value

        if (state.email.isBlank()) {
            _uiState.update { it.copy(error = "Email is required") }
            return
        }
        if (!Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(state.email)) {
            _uiState.update { it.copy(error = "Enter a valid email address") }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(error = "Password is required") }
            return
        }
        if (state.password.length < 8) {
            _uiState.update { it.copy(error = "Password must be at least 8 characters") }
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(error = "Passwords do not match") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val referral = state.referralCode.ifBlank { null }
            authRepository.registerStandard(state.email, state.password, referral)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = mapError(e)) }
                }
        }
    }

    private fun mapError(e: Throwable): String = when {
        e is HttpException && e.code() == 409 -> "An account with this email already exists"
        e is HttpException && e.code() == 400 -> "Invalid registration details"
        else -> "Registration failed. Please try again."
    }
}
