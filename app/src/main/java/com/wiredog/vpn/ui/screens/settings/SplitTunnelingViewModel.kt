package com.wiredog.vpn.ui.screens.settings

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiredog.vpn.data.local.preferences.SettingsPreferences
import com.wiredog.vpn.domain.model.ConnectionState
import com.wiredog.vpn.service.vpn.VpnConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AppInfo(
    val packageName: String,
    val name: String,
    val icon: Drawable?
)

data class SplitTunnelingUiState(
    val enabled: Boolean = false,
    val mode: String = "include",
    val selectedApps: Set<String> = emptySet(),
    val ipAddresses: List<String> = emptyList(),
    val allApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val hasUnsavedChanges: Boolean = false,
    val ipError: String? = null
)

@HiltViewModel
class SplitTunnelingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsPreferences: SettingsPreferences,
    private val vpnConnectionManager: VpnConnectionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplitTunnelingUiState())
    val uiState: StateFlow<SplitTunnelingUiState> = _uiState.asStateFlow()

    private val _showReconnectDialog = MutableSharedFlow<Unit>()
    val showReconnectDialog: SharedFlow<Unit> = _showReconnectDialog.asSharedFlow()

    private val _savedEvent = MutableSharedFlow<Unit>()
    val savedEvent: SharedFlow<Unit> = _savedEvent.asSharedFlow()

    // Store initial values to detect changes
    private var initialEnabled = false
    private var initialMode = "include"
    private var initialApps = emptySet<String>()
    private var initialIps = emptyList<String>()

    init {
        loadSettings()
        loadInstalledApps()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val enabled = settingsPreferences.splitTunnelingEnabled.first()
            val mode = settingsPreferences.splitTunnelingMode.first()
            val apps = settingsPreferences.splitTunnelingApps.first()
            val ips = settingsPreferences.splitTunnelingIps.first().toList()

            initialEnabled = enabled
            initialMode = mode
            initialApps = apps
            initialIps = ips

            _uiState.value = _uiState.value.copy(
                enabled = enabled,
                mode = mode,
                selectedApps = apps,
                ipAddresses = ips
            )
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { appInfo ->
                        // Exclude our own app
                        appInfo.packageName != context.packageName &&
                        // Only show apps that have a launcher icon (user-facing apps)
                        pm.getLaunchIntentForPackage(appInfo.packageName) != null
                    }
                    .map { appInfo ->
                        AppInfo(
                            packageName = appInfo.packageName,
                            name = appInfo.loadLabel(pm).toString(),
                            icon = try { appInfo.loadIcon(pm) } catch (_: Exception) { null }
                        )
                    }
                    .sortedBy { it.name.lowercase() }
            }
            _uiState.value = _uiState.value.copy(allApps = apps, isLoading = false)
        }
    }

    fun setEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(enabled = enabled)
        updateHasChanges()
    }

    fun setMode(mode: String) {
        _uiState.value = _uiState.value.copy(mode = mode)
        updateHasChanges()
    }

    fun toggleApp(packageName: String) {
        val current = _uiState.value.selectedApps
        val updated = if (current.contains(packageName)) current - packageName else current + packageName
        _uiState.value = _uiState.value.copy(selectedApps = updated)
        updateHasChanges()
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun addIpAddress(ip: String) {
        val trimmed = ip.trim()
        if (trimmed.isEmpty()) return
        if (!isValidCidr(trimmed)) {
            _uiState.value = _uiState.value.copy(ipError = "Invalid IP or CIDR format (e.g. 192.168.1.0/24)")
            return
        }
        if (!_uiState.value.ipAddresses.contains(trimmed)) {
            _uiState.value = _uiState.value.copy(
                ipAddresses = _uiState.value.ipAddresses + trimmed,
                ipError = null
            )
            updateHasChanges()
        }
    }

    fun updateIpAddress(index: Int, newIp: String) {
        val trimmed = newIp.trim()
        if (!isValidCidr(trimmed)) {
            _uiState.value = _uiState.value.copy(ipError = "Invalid IP or CIDR format (e.g. 192.168.1.0/24)")
            return
        }
        val list = _uiState.value.ipAddresses.toMutableList()
        if (index in list.indices) {
            list[index] = trimmed
            _uiState.value = _uiState.value.copy(ipAddresses = list, ipError = null)
            updateHasChanges()
        }
    }

    fun clearIpError() {
        _uiState.value = _uiState.value.copy(ipError = null)
    }

    private fun isValidCidr(input: String): Boolean {
        val parts = input.split("/")
        if (parts.isEmpty() || parts.size > 2) return false
        val octets = parts[0].split(".")
        if (octets.size != 4) return false
        if (octets.any { it.toIntOrNull() !in 0..255 }) return false
        if (parts.size == 2 && parts[1].toIntOrNull() !in 0..32) return false
        return true
    }

    fun removeIpAddress(index: Int) {
        val list = _uiState.value.ipAddresses.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _uiState.value = _uiState.value.copy(ipAddresses = list)
            updateHasChanges()
        }
    }

    fun isValidIpAddress(ip: String): Boolean {
        val trimmed = ip.trim()
        // IPv4: basic check
        val ipv4Regex = Regex("""^(\d{1,3}\.){3}\d{1,3}(/\d{1,2})?$""")
        // IPv6: basic check
        val ipv6Regex = Regex("""^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}(/\d{1,3})?$""")
        return ipv4Regex.matches(trimmed) || ipv6Regex.matches(trimmed)
    }

    private fun updateHasChanges() {
        val state = _uiState.value
        val changed = state.enabled != initialEnabled ||
                state.mode != initialMode ||
                state.selectedApps != initialApps ||
                state.ipAddresses != initialIps
        _uiState.value = state.copy(hasUnsavedChanges = changed)
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            settingsPreferences.setSplitTunnelingEnabled(state.enabled)
            settingsPreferences.setSplitTunnelingMode(state.mode)
            settingsPreferences.setSplitTunnelingApps(state.selectedApps)
            settingsPreferences.setSplitTunnelingIps(state.ipAddresses.toSet())

            // Update initial values
            initialEnabled = state.enabled
            initialMode = state.mode
            initialApps = state.selectedApps
            initialIps = state.ipAddresses
            _uiState.value = state.copy(hasUnsavedChanges = false)

            if (vpnConnectionManager.connectionState.value == ConnectionState.CONNECTED) {
                _showReconnectDialog.emit(Unit)
            } else {
                _savedEvent.emit(Unit)
            }
        }
    }

    fun reconnectNow() {
        viewModelScope.launch {
            vpnConnectionManager.reconnect()
            _savedEvent.emit(Unit)
        }
    }
}
