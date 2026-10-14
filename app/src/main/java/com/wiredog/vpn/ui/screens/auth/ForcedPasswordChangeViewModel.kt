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

data class ForcedPasswordChangeUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Admin-provisioned employees sign in with a temporary password and must replace it. On success
 * the profile refresh clears mustChangePassword, which makes MainActivity drop this gate.
 */
@HiltViewModel
class ForcedPasswordChangeViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForcedPasswordChangeUiState())
    val uiState: StateFlow<ForcedPasswordChangeUiState> = _uiState.asStateFlow()

    private fun clean(value: String) = value.filter { it.code in 0x20..0x7E }.take(64)

    fun onCurrentPasswordChange(value: String) = _uiState.update { it.copy(currentPassword = clean(value), error = null) }
    fun onNewPasswordChange(value: String) = _uiState.update { it.copy(newPassword = clean(value), error = null) }
    fun onConfirmPasswordChange(value: String) = _uiState.update { it.copy(confirmPassword = clean(value), error = null) }

    fun submit() {
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
            authRepository.changePassword(state.currentPassword, state.newPassword)
                .onSuccess { _uiState.update { it.copy(isLoading = false) } }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Couldn't change your password. Please try again.")
                    }
                }
        }
    }
}
