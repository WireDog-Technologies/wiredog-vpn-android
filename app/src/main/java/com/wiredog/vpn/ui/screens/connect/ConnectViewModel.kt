package com.wiredog.vpn.ui.screens.connect

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiredog.vpn.data.local.preferences.SettingsPreferences
import com.wiredog.vpn.data.remote.IpService
import com.wiredog.vpn.data.repository.AnnouncementRepository
import com.wiredog.vpn.data.repository.AuthRepository
import com.wiredog.vpn.data.repository.ServerRepository
import com.wiredog.vpn.domain.model.Announcement
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
    val isReconnecting: Boolean = false,
    val isSwitchingServer: Boolean = false,
    val connectionStartTime: Long? = null,
    val error: String? = null,
    val vpnPermissionIntent: Intent? = null,
    val splitTunnelingEnabled: Boolean = false,
    val needsSubscription: Boolean = false,
    val pendingServer: Server? = null,
    val showReviewPrompt: Boolean = false
)

/**
 * True while a connect/disconnect/reconnect/server-switch is in flight — the UI should show
 * "Loading..." for IP/location instead of a stale value from before the transition.
 */
val ConnectUiState.isTransitioning: Boolean
    get() = isSwitchingServer || isReconnecting || isLoadingIp ||
        connectionState == ConnectionState.CONNECTING ||
        connectionState == ConnectionState.DISCONNECTING

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val ipService: IpService,
    private val vpnConnectionManager: VpnConnectionManager,
    private val authRepository: AuthRepository,
    private val settingsPreferences: SettingsPreferences,
    private val announcementRepository: AnnouncementRepository
) : ViewModel() {

    val announcements: StateFlow<List<Announcement>> = announcementRepository.messages
    val readAnnouncementIds: StateFlow<Set<String>> = announcementRepository.readIds
    val unreadAnnouncementCount: StateFlow<Int> = announcementRepository.unreadCount

    fun markAnnouncementsRead(id: String) = announcementRepository.markRead(listOf(id))
    fun markAnnouncementUnread(id: String) = announcementRepository.markUnread(id)

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
        observeConnectionError()
        observeReviewPrompt()
        observeConnectionStartTime()
        observeReconnecting()
        observeSwitchingServer()
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
                // Only probe while the tunnel is down. With it up, every probe socket routes
                // through the tunnel (you -> exit node -> target), so non-current servers read
                // inflated by ~your RTT to the exit. Freeze on the last disconnected values
                // instead; observeConnectionState() re-measures on the next disconnect.
                if (vpnConnectionManager.connectionState.value == ConnectionState.DISCONNECTED) {
                    serverRepository.measureAndUpdateLatencies()
                }
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
                    // Show the backend-assigned exit IP verbatim (null -> the card renders
                    // "Unknown"). Deliberately not `?: publicIp` — falling back to the stale
                    // pre-connect IP here would misreport the user as exiting from their real
                    // address. Location is taken blindly from the selected server, no lookup.
                    val vpnIp = vpnConnectionManager.vpnIp.value
                    val server = selectedServer.value
                    _uiState.update {
                        it.copy(
                            publicIp = vpnIp,
                            location = if (server != null) "${server.city}, ${ipService.abbreviateState(server.state)}" else it.location,
                            isLoadingIp = false
                        )
                    }
                }

                if (state == ConnectionState.DISCONNECTED &&
                    (previousState == ConnectionState.CONNECTED || previousState == ConnectionState.DISCONNECTING) &&
                    !vpnConnectionManager.isSwitchingServer.value) {
                    // Deliberately do NOT show cached IP/location here — isSwitchingServer
                    // already covers the "in progress" display during a live server switch, and
                    // writing a stale cached value here (even briefly, before isLoadingIp flips
                    // true) is exactly what caused a wrong-data flash on plain disconnects too.
                    // isLoadingIp masks the stale underlying fields via isTransitioning until
                    // fetchIpWithRetryThenGeoLocation() below has fully resolved both IP and
                    // location.
                    _uiState.update { it.copy(isLoadingIp = true) }
                    delay(500)
                    fetchIpWithRetryThenGeoLocation()
                    // Latency probing was frozen while connected (would have measured through
                    // the tunnel) — refresh now that we're back on the direct path.
                    viewModelScope.launch { serverRepository.measureAndUpdateLatencies() }
                }
            }
        }
    }

    private fun observeConnectionError() {
        viewModelScope.launch {
            vpnConnectionManager.connectionError.collect { message ->
                if (message != null) {
                    _uiState.update { it.copy(error = message) }
                    vpnConnectionManager.clearConnectionError()
                }
            }
        }
    }

    private fun observeReviewPrompt() {
        viewModelScope.launch {
            vpnConnectionManager.showReviewPrompt.collect { show ->
                if (show) {
                    _uiState.update { it.copy(showReviewPrompt = true) }
                    vpnConnectionManager.acknowledgeReviewPrompt()
                }
            }
        }
    }

    fun dismissReviewPrompt() {
        _uiState.update { it.copy(showReviewPrompt = false) }
    }

    fun recordReviewPromptPositive() {
        vpnConnectionManager.recordReviewPromptPositiveResponse()
    }

    private fun observeConnectionStartTime() {
        viewModelScope.launch {
            vpnConnectionManager.connectionStartTime.collect { startTime ->
                _uiState.update { it.copy(connectionStartTime = startTime) }
            }
        }
    }

    private fun observeReconnecting() {
        viewModelScope.launch {
            vpnConnectionManager.isReconnecting.collect { reconnecting ->
                _uiState.update { it.copy(isReconnecting = reconnecting) }
            }
        }
    }

    private fun observeSwitchingServer() {
        viewModelScope.launch {
            vpnConnectionManager.isSwitchingServer.collect { switching ->
                _uiState.update { it.copy(isSwitchingServer = switching) }
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
                ipv4.await().onSuccess { ip ->
                    // Never let a third-party IP echo overwrite the exit IP while connected —
                    // the connected display is the backend-assigned exit IP only. realPublicIp
                    // (the "My IP" real address) is always safe to update.
                    _uiState.update {
                        it.copy(
                            publicIp = if (it.connectionState == ConnectionState.CONNECTED) it.publicIp else ip,
                            realPublicIp = ip
                        )
                    }
                }
                ipv6.await()
            }
            val location = ipService.getGeoLocation(ipService.cachedPublicIpv6 ?: ipService.cachedPublicIp)
            if (location != null && _uiState.value.connectionState != ConnectionState.CONNECTED) {
                _uiState.update { it.copy(location = location) }
            }
            // Only flip isLoadingIp false once both IP and location have been resolved (or
            // attempted) — flipping it as soon as the IP lands leaves a window where the UI
            // shows a fresh IP next to a stale location (e.g. still the VPN server's city).
            _uiState.update { it.copy(isLoadingIp = false) }
        }
    }

    private fun fetchIpWithRetryThenGeoLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingIp = true) }
            // A pooled socket opened over the pre-disconnect network interface can otherwise
            // keep returning the stale (VPN) IP instead of the current one.
            ipService.resetConnections()
            coroutineScope {
                val ipv6 = async { ipService.getPublicIpv6() }
                for (attempt in 0 until 3) {
                    val result = ipService.getPublicIp()
                    if (result.isSuccess) {
                        val ip = result.getOrNull()
                        _uiState.update {
                            it.copy(
                                publicIp = if (it.connectionState == ConnectionState.CONNECTED) it.publicIp else ip,
                                realPublicIp = ip
                            )
                        }
                        break
                    }
                    if (attempt < 2) delay(2000)
                }
                ipv6.await()
            }
            val location = ipService.getGeoLocation(ipService.cachedPublicIpv6 ?: ipService.cachedPublicIp)
            if (location != null && _uiState.value.connectionState != ConnectionState.CONNECTED) {
                _uiState.update { it.copy(location = location) }
            }
            // Same reasoning as fetchIpThenGeoLocation() above — hold Loading... until location
            // has resolved too, not just the IP.
            _uiState.update { it.copy(isLoadingIp = false) }
        }
    }

    fun selectServer(server: Server) {
        viewModelScope.launch {
            val previous = selectedServer.value
            serverRepository.selectServer(server)
            val state = vpnConnectionManager.connectionState.value
            if (previous?.id != server.id &&
                (state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING)) {
                vpnConnectionManager.switchServer(server)
            }
        }
    }

    fun toggleFavorite(server: Server) {
        viewModelScope.launch { serverRepository.toggleFavorite(server.id) }
    }

    fun toggleConnection() {
        // Read the manager's live state, not the mirrored UI copy — on a fast double-tap the
        // mirror can still say DISCONNECTED after the first tap already started a connect,
        // which would make the second tap fire a *second* connect instead of cancelling.
        val currentState = vpnConnectionManager.connectionState.value
        val server = selectedServer.value

        when (currentState) {
            ConnectionState.DISCONNECTED -> {
                if (server == null) {
                    _uiState.update { it.copy(error = "Please select a server first") }
                    return
                }
                viewModelScope.launch {
                    try {
                        // A recent network change (e.g. relaunch after switching Wi-Fi/cellular)
                        // can otherwise leave this request stuck on a dead pooled socket and
                        // fail here even though the session/subscription are actually fine.
                        authRepository.resetConnections()
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
            ConnectionState.CONNECTING -> vpnConnectionManager.cancelConnect()
            ConnectionState.DISCONNECTING -> { /* Already disconnecting */ }
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
        vpnConnectionManager.retryPendingDisconnects()
        startLatencyPolling()
    }
}
