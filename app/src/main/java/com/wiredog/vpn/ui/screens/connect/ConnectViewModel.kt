package com.wiredog.vpn.ui.screens.connect

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiredog.vpn.data.local.preferences.SettingsPreferences
import com.wiredog.vpn.data.remote.IpService
import com.wiredog.vpn.data.repository.AuthRepository
import com.wiredog.vpn.data.repository.ServerRepository
import com.wiredog.vpn.domain.model.ConnectionState
import com.wiredog.vpn.domain.model.ConnectionStats
import com.wiredog.vpn.domain.model.Server
import com.wiredog.vpn.service.vpn.VpnConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.random.Random
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val publicIp: String? = null,
    val realPublicIp: String? = null,
    val location: String? = null,
    val isLoadingIp: Boolean = false,
    val connectionStartTime: Long? = null,
    val error: String? = null,
    val vpnPermissionIntent: Intent? = null,
    val splitTunnelingEnabled: Boolean = false,
    val needsSubscription: Boolean = false,
    val pendingServer: Server? = null
)

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val ipService: IpService,
    private val vpnConnectionManager: VpnConnectionManager,
    private val authRepository: AuthRepository,
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState.asStateFlow()

    val selectedServer: StateFlow<Server?> = serverRepository.selectedServer
    val statistics: StateFlow<ConnectionStats> = vpnConnectionManager.statistics
    val servers: StateFlow<List<Server>> = serverRepository.servers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var latencyPollingJob: Job? = null

    init {
        val cachedIp = ipService.cachedPublicIp
        _uiState.update {
            it.copy(
                publicIp = cachedIp,
                realPublicIp = cachedIp,
                location = ipService.cachedLocation
            )
        }
        fetchIpThenGeoLocation()
        observeConnectionState()
        observeConnectionStartTime()
        observeSplitTunneling()
        restoreLastServer()
        startLatencyPolling()
    }

    private fun restoreLastServer() {
        viewModelScope.launch {
            serverRepository.fetchServers()
            serverRepository.restoreSelectedServer()
        }
    }

    private fun startLatencyPolling() {
        latencyPollingJob?.cancel()
        latencyPollingJob = viewModelScope.launch {
            while (true) {
                serverRepository.measureAndUpdateLatencies()
                val jitter = Random.nextLong(-10_000L, 10_000L)
                delay(60_000L + jitter)
            }
        }
    }

    private fun stopLatencyPolling() {
        latencyPollingJob?.cancel()
        latencyPollingJob = null
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            vpnConnectionManager.connectionState.collect { state ->
                val previousState = _uiState.value.connectionState
                _uiState.update { it.copy(connectionState = state) }

                val startTime = vpnConnectionManager.connectionStartTime.value
                _uiState.update { it.copy(connectionStartTime = startTime) }

                if (state == ConnectionState.CONNECTED && previousState != ConnectionState.CONNECTED) {
                    val vpnIp = vpnConnectionManager.vpnIp.value
                    val server = selectedServer.value
                    _uiState.update {
                        it.copy(
                            publicIp = vpnIp ?: it.publicIp,
                            location = if (server != null) "${server.city}, ${ipService.abbreviateState(server.state)}" else it.location,
                            isLoadingIp = false
                        )
                    }
                }

                if (state == ConnectionState.DISCONNECTED &&
                    (previousState == ConnectionState.CONNECTED || previousState == ConnectionState.DISCONNECTING)) {
                    _uiState.update {
                        it.copy(
                            publicIp = ipService.cachedPublicIp ?: it.publicIp,
                            location = ipService.cachedLocation
                        )
                    }
                    delay(500)
                    fetchIpWithRetryThenGeoLocation()
                }
            }
        }
    }

    private fun observeConnectionStartTime() {
        viewModelScope.launch {
            vpnConnectionManager.connectionStartTime.collect { startTime ->
                _uiState.update { it.copy(connectionStartTime = startTime) }
            }
        }
    }

    private fun observeSplitTunneling() {
        viewModelScope.launch {
            settingsPreferences.splitTunnelingEnabled.collect { enabled ->
                _uiState.update { it.copy(splitTunnelingEnabled = enabled) }
            }
        }
    }

    private fun fetchIpThenGeoLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingIp = true) }
            coroutineScope {
                val ipv4 = async { ipService.getPublicIp() }
                val ipv6 = async { ipService.getPublicIpv6() }
                ipv4.await()
                    .onSuccess { ip -> _uiState.update { it.copy(publicIp = ip, realPublicIp = ip, isLoadingIp = false) } }
                    .onFailure { _uiState.update { it.copy(isLoadingIp = false) } }
                ipv6.await()
            }
            val location = ipService.getGeoLocation(ipService.cachedPublicIpv6 ?: ipService.cachedPublicIp)
            if (location != null && _uiState.value.connectionState != ConnectionState.CONNECTED) {
                _uiState.update { it.copy(location = location) }
            }
        }
    }

    private fun fetchIpWithRetryThenGeoLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingIp = true) }
            coroutineScope {
                val ipv6 = async { ipService.getPublicIpv6() }
                var ipv4Success = false
                for (attempt in 0 until 3) {
                    val result = ipService.getPublicIp()
                    if (result.isSuccess) {
                        _uiState.update { it.copy(publicIp = result.getOrNull(), realPublicIp = result.getOrNull(), isLoadingIp = false) }
                        ipv4Success = true
                        break
                    }
                    if (attempt < 2) delay(2000)
                }
                if (!ipv4Success) _uiState.update { it.copy(isLoadingIp = false) }
                ipv6.await()
            }
            val location = ipService.getGeoLocation(ipService.cachedPublicIpv6 ?: ipService.cachedPublicIp)
            if (location != null && _uiState.value.connectionState != ConnectionState.CONNECTED) {
                _uiState.update { it.copy(location = location) }
            }
        }
    }

    fun selectServer(server: Server) {
        viewModelScope.launch { serverRepository.selectServer(server) }
    }

    fun toggleFavorite(server: Server) {
        viewModelScope.launch { serverRepository.toggleFavorite(server.id) }
    }

    fun toggleConnection() {
        val currentState = _uiState.value.connectionState
        val server = selectedServer.value

        when (currentState) {
            ConnectionState.DISCONNECTED -> {
                if (server == null) {
                    _uiState.update { it.copy(error = "Please select a server first") }
                    return
                }
                viewModelScope.launch {
                    try {
                        authRepository.fetchProfile()
                        val user = authRepository.currentUser.value
                        if (user != null && !user.isSubscriptionActive) {
                            _uiState.update { it.copy(needsSubscription = true, pendingServer = server) }
                        } else {
                            connect(server)
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(error = "Failed to verify subscription: ${e.message}") }
                    }
                }
            }
            ConnectionState.CONNECTED -> disconnect()
            else -> { /* Already connecting or disconnecting */ }
        }
    }

    private fun connect(server: Server) {
        val prepareIntent = vpnConnectionManager.prepareVpn()
        if (prepareIntent != null) {
            _uiState.update { it.copy(vpnPermissionIntent = prepareIntent) }
            return
        }

        vpnConnectionManager.connect(server)
    }

    /**
     * Called by the UI after VPN permission is granted.
     */
    fun onVpnPermissionGranted() {
        _uiState.update { it.copy(vpnPermissionIntent = null) }
        val server = selectedServer.value ?: return
        vpnConnectionManager.connect(server)
    }

    /**
     * Called by the UI if VPN permission is denied.
     */
    fun onVpnPermissionDenied() {
        _uiState.update {
            it.copy(
                vpnPermissionIntent = null,
                error = "VPN permission is required to connect"
            )
        }
    }

    private fun disconnect() {
        vpnConnectionManager.disconnect()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun closeSubscriptionSheet() {
        _uiState.update { it.copy(needsSubscription = false, pendingServer = null) }
    }

    fun retryConnectAfterSubscription() {
        _uiState.update { it.copy(needsSubscription = false) }
        val pendingServer = _uiState.value.pendingServer
        if (pendingServer != null) {
            _uiState.update { it.copy(pendingServer = null) }
            connect(pendingServer)
        }
    }

    fun onPause() {
        vpnConnectionManager.pauseStatsPolling()
        stopLatencyPolling()
    }

    fun onResume() {
        vpnConnectionManager.resumeStatsPolling()
        startLatencyPolling()
    }
}
