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

data class TwoFactorSetupUiState(
    val isOpeningDashboard: Boolean = false,
    val isChecking: Boolean = false,
    val error: String? = null
)

/**
 * The user's organization requires 2FA and they have not turned it on. Setup happens on the
 * website dashboard (opened already signed in via a handoff code); once done, refreshing the
 * profile clears twoFactorRequiredByOrg && !totpEnabled and MainActivity drops this gate.
 */
@HiltViewModel
class TwoFactorSetupRequiredViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TwoFactorSetupUiState())
    val uiState: StateFlow<TwoFactorSetupUiState> = _uiState.asStateFlow()

    fun openDashboard(onUrl: (String) -> Unit) {
        if (_uiState.value.isOpeningDashboard) return
        _uiState.update { it.copy(isOpeningDashboard = true, error = null) }
        viewModelScope.launch {
            val url = authRepository.dashboardHandoffUrl()
            _uiState.update { it.copy(isOpeningDashboard = false) }
            onUrl(url)
        }
    }

    fun checkAgain() {
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, error = null) }
            authRepository.fetchProfile()
                .onSuccess { user ->
                    val stillRequired = user.twoFactorRequiredByOrg && !user.totpEnabled
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            error = if (stillRequired) "Two-factor authentication is not turned on yet." else null
                        )
                    }
                }
                .onFailure { _uiState.update { it.copy(isChecking = false, error = "Could not check your account. Please try again.") } }
        }
    }
}
