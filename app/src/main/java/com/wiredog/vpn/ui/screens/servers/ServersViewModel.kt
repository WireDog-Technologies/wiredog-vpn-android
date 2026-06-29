package com.wiredog.vpn.ui.screens.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiredog.vpn.data.repository.ServerRepository
import com.wiredog.vpn.domain.model.Server
import com.wiredog.vpn.domain.model.ServerGroup
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
    val sheetGroup: ServerGroup? = null
)

@HiltViewModel
class ServersViewModel @Inject constructor(
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServersUiState())
    val uiState: StateFlow<ServersUiState> = _uiState.asStateFlow()

    val selectedServer: StateFlow<Server?> = serverRepository.selectedServer

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
            serverRepository.selectServer(server)
        }
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
