package com.wiredog.vpn.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiredog.vpn.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnonymousAccountUiState(
    val isGenerating: Boolean = true,
    val error: String? = null,
    val accountNumber: String? = null,
    val showCopyMessage: Boolean = false,
    val isLoggedIn: Boolean = false
)

@HiltViewModel
class AnonymousAccountViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnonymousAccountUiState())
    val uiState: StateFlow<AnonymousAccountUiState> = _uiState.asStateFlow()

    // Guards against a duplicate account being minted: `init` and the "Try Again" button both
    // call createAccount(), and the backend's /auth/register/anonymous has no idempotency key —
    // a second concurrent call creates a second, unrelated account.
    private var creationJob: Job? = null

    init {
        createAccount()
    }

    fun createAccount() {
        if (_uiState.value.accountNumber != null) return
        if (creationJob?.isActive == true) return

        _uiState.update { AnonymousAccountUiState(isGenerating = true, error = null) }
        creationJob = viewModelScope.launch {
            authRepository.registerAnonymous()
                .onSuccess { accountNumber ->
                    _uiState.update { it.copy(isGenerating = false, accountNumber = accountNumber) }
                }
                .onFailure {
                    _uiState.update { it.copy(isGenerating = false, error = "Failed to create account. Please try again.") }
                }
        }
    }

    fun onCopy() {
        viewModelScope.launch {
            _uiState.update { it.copy(showCopyMessage = true) }
            delay(2000)
            _uiState.update { it.copy(showCopyMessage = false) }
        }
    }

    fun onGotIt() {
        val accountNumber = _uiState.value.accountNumber ?: return
        viewModelScope.launch {
            authRepository.loginAnonymous(accountNumber)
                .onSuccess { _uiState.update { it.copy(isLoggedIn = true) } }
                .onFailure { _uiState.update { it.copy(error = "Login failed. Please try signing in manually.") } }
        }
    }
}
