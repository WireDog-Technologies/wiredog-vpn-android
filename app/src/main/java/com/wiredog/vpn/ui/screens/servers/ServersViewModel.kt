package com.wiredog.vpn.ui.screens.servers

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiredog.vpn.data.repository.AuthRepository
import com.wiredog.vpn.data.repository.ServerRepository
import com.wiredog.vpn.domain.model.ConnectionState
import com.wiredog.vpn.domain.model.Server
import com.wiredog.vpn.domain.model.ServerGroup
import com.wiredog.vpn.service.vpn.VpnConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServersUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val showFavoritesOnly: Boolean = false,
    val expandedStateKey: String? = null,
    val sheetGroup: ServerGroup? = null,
    val vpnPermissionIntent: Intent? = null,
    val needsSubscription: Boolean = false,
    val pendingServer: Server? = null,
    // Distinct from `error` above, which means "server list failed to load" and replaces the
    // whole list with a retry screen — this is a transient connect-attempt failure surfaced as
    // a snackbar instead.
    val connectError: String? = null
)

@HiltViewModel
class ServersViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val vpnConnectionManager: VpnConnectionManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServersUiState())
    val uiState: StateFlow<ServersUiState> = _uiState.asStateFlow()

    val selectedServer: StateFlow<Server?> = serverRepository.selectedServer
    val connectionState: StateFlow<ConnectionState> = vpnConnectionManager.connectionState
    val isSwitchingServer: StateFlow<Boolean> = vpnConnectionManager.isSwitchingServer

    val serverGroups: StateFlow<List<ServerGroup>> = combine(
        serverRepository.servers,
        _uiState
    ) { serverList, state ->
        var filtered = serverList

        // Apply search filter
        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.lowercase()
            filtered = filtered.filter { server ->
                server.state.lowercase().contains(query) ||
                server.city.lowercase().contains(query) ||
                server.stateCode.lowercase().contains(query)
            }
        }

        // Apply favorites filter
        if (state.showFavoritesOnly) {
            filtered = filtered.filter { it.isFavorite }
        }

        // Group by state, then sort groups
        filtered
            .groupBy { it.state }
            .map { (state, servers) ->
                val first = servers.first()
                ServerGroup(
                    state = state,
                    stateCode = first.stateCode,
                    servers = servers.sortedWith(compareBy({ it.city }, { it.id }))
                )
            }
            .sortedWith(
                compareByDescending<ServerGroup> { group ->
                    group.servers.any { it.isFavorite }
                }
                    .thenBy { it.state }
            )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadServers()
    }

    fun loadServers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            serverRepository.fetchServers()
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    serverRepository.restoreSelectedServer()
                    launch { serverRepository.measureAndUpdateLatencies() }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load servers"
                        )
                    }
                }
        }
    }

    fun refreshServers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            serverRepository.fetchServers(forceRefresh = true)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    launch { serverRepository.measureAndUpdateLatencies() }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load servers"
                        )
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        val sanitized = query
            .filter { it.code in 0x20..0x7E }
            .take(100)
        _uiState.update { it.copy(searchQuery = sanitized) }
    }

    fun toggleFavoritesFilter() {
        _uiState.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }
    }

    fun toggleStateExpansion(group: ServerGroup) {
        if (group.servers.size > 3) {
            // Open bottom sheet for >3 servers
            _uiState.update { it.copy(sheetGroup = group) }
        } else {
            // Toggle inline expand for ≤3 servers
            _uiState.update {
                it.copy(
                    expandedStateKey = if (it.expandedStateKey == group.state) null else group.state
                )
            }
        }
    }

    fun dismissSheet() {
        _uiState.update { it.copy(sheetGroup = null) }
    }

    fun selectServer(server: Server) {
        viewModelScope.launch {
            val previous = selectedServer.value
            val isSameServer = previous?.id == server.id
            serverRepository.selectServer(server)
            val state = vpnConnectionManager.connectionState.value

            when (state) {
                ConnectionState.CONNECTED, ConnectionState.CONNECTING -> {
                    if (!isSameServer) vpnConnectionManager.switchServer(server)
                }
                ConnectionState.DISCONNECTING -> { /* mid-teardown from something else — let it settle */ }
                ConnectionState.DISCONNECTED -> {
                    // Re-tapping the already-selected server while idle reads as "connect to
                    // this" (matches iOS). Picking a different server just updates the
                    // selection — the user still has to hit Connect for that one.
                    if (isSameServer) connect(server)
                }
            }
        }
    }

    private suspend fun connect(server: Server) {
        try {
            // A recent network change can otherwise leave this request stuck on a dead pooled
            // socket and fail here even though the session/subscription are actually fine.
            authRepository.resetConnections()
            authRepository.fetchProfile()
            val user = authRepository.currentUser.value
            if (user != null && !user.isSubscriptionActive) {
                _uiState.update { it.copy(needsSubscription = true, pendingServer = server) }
                return
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(connectError = "Failed to verify subscription: ${e.message}") }
            return
        }

        val prepareIntent = vpnConnectionManager.prepareVpn()
        if (prepareIntent != null) {
            _uiState.update { it.copy(vpnPermissionIntent = prepareIntent) }
            return
        }

        vpnConnectionManager.connect(server)
    }

    /** Called by the UI after VPN permission is granted. */
    fun onVpnPermissionGranted() {
        _uiState.update { it.copy(vpnPermissionIntent = null) }
        val server = selectedServer.value ?: return
        vpnConnectionManager.connect(server)
    }

    /** Called by the UI if VPN permission is denied. */
    fun onVpnPermissionDenied() {
        _uiState.update {
            it.copy(
                vpnPermissionIntent = null,
                connectError = "VPN permission is required to connect"
            )
        }
    }

    fun closeSubscriptionSheet() {
        _uiState.update { it.copy(needsSubscription = false, pendingServer = null) }
    }

    fun clearConnectError() {
        _uiState.update { it.copy(connectError = null) }
    }

    fun toggleFavorite(server: Server) {
        viewModelScope.launch {
            serverRepository.toggleFavorite(server.id)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
