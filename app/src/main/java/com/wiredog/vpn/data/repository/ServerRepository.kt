package com.wiredog.vpn.data.repository

import com.wiredog.vpn.data.local.preferences.ServerPreferences
import com.wiredog.vpn.data.logging.LogLevel
import com.wiredog.vpn.data.logging.LogService
import com.wiredog.vpn.data.remote.LatencyService
import com.wiredog.vpn.data.remote.api.WireDogApi
import com.wiredog.vpn.domain.model.Server
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor(
    private val api: WireDogApi,
    private val serverPreferences: ServerPreferences,
    private val logService: LogService,
    private val latencyService: LatencyService,
    authRepository: AuthRepository
) {
    private val _servers = MutableStateFlow<List<Server>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedServer = MutableStateFlow<Server?>(null)
    val selectedServer: StateFlow<Server?> = _selectedServer.asStateFlow()

    // True while the selection is our own default (nothing saved by the user yet): it is then
    // re-picked once latencies arrive, so the default is the fastest server, not just a guess.
    private var autoSelected = false

    private var lastFetchTime: Long = 0L
    private val cacheTtlMs: Long = 5 * 60 * 1000L // 5 minutes

    val servers: Flow<List<Server>> = combine(
        _servers,
        serverPreferences.favoriteServerIds
    ) { serverList, favoriteIds ->
        serverList.map { server ->
            server.copy(isFavorite = server.id in favoriteIds)
        }
    }

    init {
        // The server list depends on who is signed in: an organization member gets a different
        // list (Dedicated IP gateways, restricted locations) than the public one. Drop the cache
        // whenever auth flips and reload.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            authRepository.isLoggedIn.drop(1).distinctUntilChanged().collect { loggedIn ->
                lastFetchTime = 0L
                _servers.value = emptyList()
                _selectedServer.value = null
                autoSelected = false
                if (loggedIn) fetchServers(forceRefresh = true).onSuccess { restoreSelectedServer() }
            }
        }
    }

    val favoriteServerIds: Flow<Set<String>> = serverPreferences.favoriteServerIds
    val lastConnectedServerId: Flow<String?> = serverPreferences.lastConnectedServerId

    suspend fun fetchServers(forceRefresh: Boolean = false): Result<List<Server>> {
        // Return cache if still fresh
        val now = System.currentTimeMillis()
        if (!forceRefresh && _servers.value.isNotEmpty() && (now - lastFetchTime) < cacheTtlMs) {
            return Result.success(_servers.value)
        }

        return try {
            _isLoading.value = true
            logService.logService("Fetching server list")
            val favoriteIds = serverPreferences.favoriteServerIds.first()
            val cachedLatencies = serverPreferences.getCachedLatencies()
            val serverDtos = api.getServers()
            val serverList = serverDtos.map { dto ->
                dto.toDomain(isFavorite = dto.id in favoriteIds).let { server ->
                    cachedLatencies[server.id]?.let { server.copy(latency = it) } ?: server
                }
            }
            _servers.value = serverList
            lastFetchTime = System.currentTimeMillis()
            _isLoading.value = false
            logService.logService("Server list fetched: ${serverList.size} servers")
            Result.success(serverList)
        } catch (e: Exception) {
            _isLoading.value = false
            logService.logService("Server list fetch failed: ${e.message}", LogLevel.WARNING)
            // If we have cached data, return it on error
            if (_servers.value.isNotEmpty()) {
                logService.logService("Using cached server list")
                Result.success(_servers.value)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun toggleFavorite(serverId: String) {
        serverPreferences.toggleFavorite(serverId)
        // Update local cache
        _servers.value = _servers.value.map { server ->
            if (server.id == serverId) {
                server.copy(isFavorite = !server.isFavorite)
            } else {
                server
            }
        }
    }

    suspend fun selectServer(server: Server) {
        autoSelected = false
        _selectedServer.value = server
        serverPreferences.setSelectedServer(server.id)
    }

    suspend fun setLastConnectedServer(serverId: String) {
        serverPreferences.setLastConnectedServer(serverId)
        serverPreferences.addToRecent(serverId)
    }

    fun getServerById(serverId: String): Server? {
        return _servers.value.find { it.id == serverId }
    }

    suspend fun restoreSelectedServer() {
        val selectedId = serverPreferences.selectedServerId.first()
        _selectedServer.value = selectedId?.let { getServerById(it) }
        if (_selectedServer.value != null || _servers.value.isEmpty()) return

        // No saved selection (new install), or it is no longer offered (e.g. the account just
        // changed and the list is now restricted to the organization's own servers): fall back to
        // the last connected one, then the best available. Not persisted, so it never masquerades
        // as a choice the user made.
        val lastId = serverPreferences.lastConnectedServerId.first()
        val fallback = lastId?.let { getServerById(it) }
        if (fallback != null) {
            selectServer(fallback)
        } else {
            autoSelected = true
            _selectedServer.value = bestServer(_servers.value)
        }
    }

    /** Lowest latency (with load as a tiebreaker), same ranking as the Recommended section. */
    private fun bestServer(servers: List<Server>): Server? {
        val best = servers
            .filter { it.latency > 0 && it.load < 80 }
            .minByOrNull { it.latency + (it.load * 2) }
        return best ?: servers.firstOrNull { it.isRecommended } ?: servers.firstOrNull()
    }

    /**
     * [isStillDisconnected] is re-checked after the sweep: a connect that started mid-measurement
     * routes the remaining probes through the tunnel, so those samples are discarded.
     */
    suspend fun measureAndUpdateLatencies(isStillDisconnected: () -> Boolean = { true }) {
        val measured = latencyService.measureAll(_servers.value)
        if (!isStillDisconnected()) return
        if (measured.isNotEmpty()) {
            updateLatencies(measured)
            serverPreferences.saveLatencies(measured)
        }
    }

    private fun updateLatencies(measured: Map<String, Int>) {
        _servers.value = _servers.value.map { server ->
            measured[server.id]?.let { server.copy(latency = it) } ?: server
        }
        if (autoSelected) _selectedServer.value = bestServer(_servers.value)
    }
}
