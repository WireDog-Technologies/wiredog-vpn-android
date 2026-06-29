package com.wiredog.vpn.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiredog.vpn.BuildConfig
import com.wiredog.vpn.data.local.preferences.SettingsPreferences
import com.wiredog.vpn.data.repository.AuthRepository
import com.wiredog.vpn.domain.model.ConnectionState
import com.wiredog.vpn.domain.model.User
import com.wiredog.vpn.service.vpn.VpnConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val protocol: String = "AmneziaWG",
    val autoConnectEnabled: Boolean = false,
    val ipv6Enabled: Boolean = false,
    val splitTunnelingSummary: String = "Off"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsPreferences: SettingsPreferences,
    private val authRepository: AuthRepository,
    private val vpnConnectionManager: VpnConnectionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _showReconnectWarning = MutableSharedFlow<Unit>()
    val showReconnectWarning: SharedFlow<Unit> = _showReconnectWarning.asSharedFlow()

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val appVersionCode: Int = BuildConfig.VERSION_CODE
    val appVersionName: String = BuildConfig.VERSION_NAME

    init {
        loadSettings()
        fetchProfile()
    }

    private fun fetchProfile() {
        viewModelScope.launch {
            authRepository.fetchProfile()
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsPreferences.protocol.collect { protocol ->
                _uiState.value = _uiState.value.copy(protocol = protocol)
            }
        }
        viewModelScope.launch {
            settingsPreferences.autoConnectEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(autoConnectEnabled = enabled)
            }
        }
        viewModelScope.launch {
            settingsPreferences.ipv6Enabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(ipv6Enabled = enabled)
            }
        }
        viewModelScope.launch {
            combine(
                settingsPreferences.splitTunnelingEnabled,
                settingsPreferences.splitTunnelingMode,
                settingsPreferences.splitTunnelingApps,
                settingsPreferences.splitTunnelingIps
            ) { enabled, mode, apps, ips ->
                if (!enabled) "Off"
                else {
                    val modeLabel = mode.replaceFirstChar { it.uppercase() }
                    val parts = mutableListOf<String>()
                    if (apps.isNotEmpty()) parts.add("${apps.size} app${if (apps.size != 1) "s" else ""}")
                    if (ips.isNotEmpty()) parts.add("${ips.size} IP${if (ips.size != 1) "s" else ""}")
                    if (parts.isEmpty()) "$modeLabel mode"
                    else "$modeLabel: ${parts.joinToString(", ")}"
                }
            }.collect { summary ->
                _uiState.value = _uiState.value.copy(splitTunnelingSummary = summary)
            }
        }
    }

    fun setAutoConnectEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setAutoConnectEnabled(enabled)
        }
    }

    fun setIpv6Enabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setIpv6Enabled(enabled)
            if (vpnConnectionManager.connectionState.value == ConnectionState.CONNECTED) {
                _showReconnectWarning.emit(Unit)
            }
        }
    }
}
