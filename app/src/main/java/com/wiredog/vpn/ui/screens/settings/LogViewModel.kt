package com.wiredog.vpn.ui.screens.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class LogViewerUiState(
    val logs: String = "",
    val isLoading: Boolean = true,
    val title: String = "Logs"
)

@HiltViewModel
class LogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val logType: String = savedStateHandle.get<String>("logType") ?: "application"

    private val _uiState = MutableStateFlow(LogViewerUiState())
    val uiState: StateFlow<LogViewerUiState> = _uiState.asStateFlow()

    init {
        loadLogs()
    }

    private fun loadLogs() {
        val title = if (logType == "service") "Service Logs" else "Application Logs"
        _uiState.value = _uiState.value.copy(isLoading = true, title = title)

        viewModelScope.launch {
            val logText = withContext(Dispatchers.IO) {
                try {
                    val command = if (logType == "service") {
                        // Filter by VPN-related tags
                        arrayOf("logcat", "-d", "-v", "time", "-s",
                            "VpnConnectionManager:*", "BootReceiver:*")
                    } else {
                        // Filter by current app process
                        arrayOf("logcat", "-d", "-v", "time",
                            "--pid=${android.os.Process.myPid()}")
                    }
                    val process = Runtime.getRuntime().exec(command)
                    process.inputStream.bufferedReader().readText()
                } catch (e: Exception) {
                    "Error reading logs: ${e.message}"
                }
            }
            _uiState.value = _uiState.value.copy(
                logs = logText.ifBlank { "No logs available." },
                isLoading = false
            )
        }
    }

    fun refreshLogs() {
        loadLogs()
    }
}
