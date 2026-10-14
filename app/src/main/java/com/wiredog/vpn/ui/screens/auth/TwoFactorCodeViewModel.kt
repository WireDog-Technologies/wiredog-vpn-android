package com.wiredog.vpn.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiredog.vpn.data.repository.AuthException
import com.wiredog.vpn.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TwoFactorUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    // The challenge timed out: the screen sends the user back to the password step.
    val challengeExpired: Boolean = false
)

@HiltViewModel
class TwoFactorCodeViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TwoFactorUiState())
    val uiState: StateFlow<TwoFactorUiState> = _uiState.asStateFlow()

    fun onCodeChange(value: String) {
        // A 6-digit authenticator code or a recovery code (letters/digits/dashes).
        val sanitized = value.filter { it.isLetterOrDigit() || it == '-' }.take(15)
        _uiState.update { it.copy(code = sanitized, error = null) }
    }

    fun submit() {
        val code = _uiState.value.code.trim()
        if (code.isEmpty()) {
            _uiState.update { it.copy(error = "Enter your authenticator or recovery code") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.verifyTwoFactor(code)
                .onSuccess { _uiState.update { it.copy(isLoading = false, isSuccess = true) } }
                .onFailure { e ->
                    val auth = e as? AuthException
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = auth?.message ?: "An error occurred. Please try again later.",
                            challengeExpired = auth?.kind == AuthException.Kind.TWO_FACTOR_CHALLENGE_EXPIRED
                        )
                    }
                }
        }
    }

    /** Backing out of the code screen abandons the challenge. */
    fun abandonChallenge() {
        authRepository.pendingTwoFactorChallenge = null
    }
}
