package com.wiredog.vpn.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiredog.vpn.BuildConfig
import com.wiredog.vpn.data.repository.AppConfigRepository
import com.wiredog.vpn.data.repository.AuthRepository
import com.wiredog.vpn.domain.model.ConnectionState
import com.wiredog.vpn.domain.model.User
import com.wiredog.vpn.service.vpn.VpnConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isSigningOut: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val error: String? = null,
    val debugMinVersion: Int? = null,
    val debugLatestVersion: Int? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val appConfigRepository: AppConfigRepository,
    private val vpnConnectionManager: VpnConnectionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val connectionState: StateFlow<ConnectionState> = vpnConnectionManager.connectionState

    val appVersionCode: Int = BuildConfig.VERSION_CODE
    val appVersionName: String = BuildConfig.VERSION_NAME

    init {
        fetchProfile()
        loadDebugConfig()
    }

    private fun loadDebugConfig() {
        viewModelScope.launch {
            val config = appConfigRepository.getCachedConfig()
            if (config != null) {
                _uiState.value = _uiState.value.copy(
                    debugMinVersion = config.platforms.android.minSupportedVersion,
                    debugLatestVersion = config.platforms.android.latestVersion
                )
            }
        }
    }

    private fun fetchProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            authRepository.fetchProfile()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
        }
    }

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSigningOut = true)
            authRepository.logout()
            _uiState.value = _uiState.value.copy(isSigningOut = false)
            onComplete()
        }
    }

    fun deleteAccount(onComplete: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingAccount = true)
            authRepository.deleteAccount()
                .onSuccess { onComplete() }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isDeletingAccount = false)
                    onError(e.message ?: "Failed to delete account. Please try again.")
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
