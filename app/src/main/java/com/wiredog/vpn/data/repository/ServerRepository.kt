package com.wiredog.vpn.data.repository

import com.wiredog.vpn.data.local.preferences.ServerPreferences
import com.wiredog.vpn.data.logging.LogLevel
import com.wiredog.vpn.data.logging.LogService
import com.wiredog.vpn.data.remote.LatencyService
import com.wiredog.vpn.data.remote.api.WireDogApi
import com.wiredog.vpn.domain.model.Server
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor(
    private val api: WireDogApi,
    private val serverPreferences: ServerPreferences,
    private val logService: LogService,
    private val latencyService: LatencyService
) {
    private val _servers = MutableStateFlow<List<Server>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedServer = MutableStateFlow<Server?>(null)
    val selectedServer: StateFlow<Server?> = _selectedServer.asStateFlow()

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
        if (selectedId != null) {
            _selectedServer.value = getServerById(selectedId)
        }
    }

    suspend fun measureAndUpdateLatencies() {
        val measured = latencyService.measureAll(_servers.value)
        if (measured.isNotEmpty()) {
            updateLatencies(measured)
            serverPreferences.saveLatencies(measured)
        }
    }

    private fun updateLatencies(measured: Map<String, Int>) {
        _servers.value = _servers.value.map { server ->
            measured[server.id]?.let { server.copy(latency = it) } ?: server
        }
    }
}
