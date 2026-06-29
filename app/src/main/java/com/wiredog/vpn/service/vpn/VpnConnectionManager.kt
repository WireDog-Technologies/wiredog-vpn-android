package com.wiredog.vpn.service.vpn

import android.content.Context
import android.content.Intent
import android.util.Log
import com.wiredog.vpn.BuildConfig
import com.wiredog.vpn.data.config.Config
import com.wiredog.vpn.data.local.preferences.SettingsPreferences
import com.wiredog.vpn.data.logging.LogLevel
import com.wiredog.vpn.data.logging.LogService
import com.wiredog.vpn.data.repository.ServerRepository
import com.wiredog.vpn.data.repository.VpnRepository
import com.wiredog.vpn.domain.model.ConnectionState
import com.wiredog.vpn.domain.model.ConnectionStats
import com.wiredog.vpn.domain.model.Server
import com.wiredog.vpn.domain.model.SplitTunnelingSettings
import org.amnezia.awg.backend.GoBackend
import org.amnezia.awg.backend.Tunnel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class VpnConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vpnRepository: VpnRepository,
    private val serverRepository: ServerRepository,
    private val settingsPreferences: SettingsPreferences,
    private val logService: LogService
) {
    companion object {
        private const val TAG = "VpnConnectionManager"
        private const val MAX_RECONNECT_DELAY_MS = 15_000L
        private const val STATS_FAILURE_THRESHOLD = 5
        // Grace period on foreground resume: skip health checks for this many ms
        private const val FOREGROUND_GRACE_PERIOD_MS = 8_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val backend: GoBackend by lazy { GoBackend(context) }
    private val tunnel = WireDogTunnel()

    init {
        scope.launch { runStartupCleanup() }
    }

    private suspend fun runStartupCleanup() {
        val tunnelState = try {
            withContext(Dispatchers.IO) { backend.getState(tunnel) }
        } catch (_: Exception) {
            return
        }
        if (tunnelState != Tunnel.State.DOWN) return

        val cleaned = try {
            vpnRepository.cleanupStaleSession()
        } catch (_: Exception) {
            false
        }
        if (cleaned && BuildConfig.DEBUG) Log.i(TAG, "Startup cleanup: stale session counter decremented")
    }

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _statistics = MutableStateFlow(ConnectionStats())
    val statistics: StateFlow<ConnectionStats> = _statistics.asStateFlow()

    private val _connectionStartTime = MutableStateFlow<Long?>(null)
    val connectionStartTime: StateFlow<Long?> = _connectionStartTime.asStateFlow()

    private val _vpnIp = MutableStateFlow<String?>(null)
    val vpnIp: StateFlow<String?> = _vpnIp.asStateFlow()

    private var statsJob: Job? = null
    private var previousRxBytes: Long = 0
    private var previousTxBytes: Long = 0
    private var consecutiveStatsFailures: Int = 0

    // Track current server for auto-reconnect
    private var currentServer: Server? = null
    private var isUserDisconnect: Boolean = false
    // Foreground grace period: timestamp after which health checks resume
    private var gracePeriodUntil: Long = 0L

    fun prepareVpn(): Intent? {
        return GoBackend.VpnService.prepare(context)
    }

    fun connect(server: Server) {
        if (_connectionState.value == ConnectionState.CONNECTING ||
            _connectionState.value == ConnectionState.CONNECTED) {
            return
        }

        isUserDisconnect = false
        currentServer = server

        scope.launch {
            _connectionState.value = ConnectionState.CONNECTING
            logService.logService("Connection attempt to ${server.displayName}")

            try {
                // 1. Call API to get WireGuard config
                val result = vpnRepository.connect(server.id)
                val response = result.getOrElse { e ->
                    if (BuildConfig.DEBUG) Log.e(TAG, "API connect failed", e)
                    logService.logService("Connection failed: API error", LogLevel.ERROR)
                    _connectionState.value = ConnectionState.DISCONNECTED
                    return@launch
                }

                // 2. Read settings
                val ipv6Enabled = settingsPreferences.ipv6Enabled.first()
                val splitTunneling = readSplitTunnelingSettings()

                // 3. Build WireGuard Config from API response
                val config = TunnelConfigBuilder.build(response.config, ipv6Enabled, splitTunneling)

                // 4. Start tunnel via GoBackend
                withContext(Dispatchers.IO) {
                    backend.setState(tunnel, Tunnel.State.UP, config)
                }

                // 5. Resolve endpoint IP for display
                val endpointHost = response.config.peer.endpoint.substringBefore(":")
                _vpnIp.value = try {
                    withContext(Dispatchers.IO) {
                        java.net.InetAddress.getByName(endpointHost).hostAddress
                    }
                } catch (_: Exception) { null }

                // 6. Mark connected
                _connectionState.value = ConnectionState.CONNECTED
                _connectionStartTime.value = System.currentTimeMillis()
                previousRxBytes = 0
                previousTxBytes = 0
                consecutiveStatsFailures = 0

                // 7. Save last connected server
                serverRepository.setLastConnectedServer(server.id)

                // 8. Start stats polling
                startStatsPolling()

                if (BuildConfig.DEBUG) Log.i(TAG, "Connected to ${server.displayName}")
                logService.logService("Connected to ${server.displayName}")

            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Connection failed", e)
                logService.logService("Connection failed: ${e.message}", LogLevel.ERROR)
                _connectionState.value = ConnectionState.DISCONNECTED
                try {
                    withContext(Dispatchers.IO) {
                        backend.setState(tunnel, Tunnel.State.DOWN, null)
                    }
                } catch (_: Exception) { }
            }
        }
    }

    fun disconnect() {
        if (_connectionState.value == ConnectionState.DISCONNECTED ||
            _connectionState.value == ConnectionState.DISCONNECTING) {
            return
        }

        isUserDisconnect = true

        scope.launch {
            _connectionState.value = ConnectionState.DISCONNECTING

            try {
                stopStatsPolling()

                withContext(Dispatchers.IO) {
                    backend.setState(tunnel, Tunnel.State.DOWN, null)
                }

                try {
                    vpnRepository.disconnect()
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) Log.w(TAG, "API disconnect failed (non-critical)", e)
                }

                if (BuildConfig.DEBUG) Log.i(TAG, "Disconnected")
                logService.logService("Disconnected")

            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Disconnect error", e)
            } finally {
                _connectionState.value = ConnectionState.DISCONNECTED
                _connectionStartTime.value = null
                _vpnIp.value = null
                _statistics.value = ConnectionStats()
                currentServer = null
            }
        }
    }

    private fun attemptReconnect() {
        val server = currentServer ?: return
        if (isUserDisconnect) return

        if (BuildConfig.DEBUG) Log.i(TAG, "Attempting auto-reconnect to ${server.displayName}")
        logService.logService("Auto-reconnecting to ${server.displayName}")

        scope.launch {
            _connectionState.value = ConnectionState.CONNECTING

            // Try to tear down old tunnel first
            try {
                withContext(Dispatchers.IO) {
                    backend.setState(tunnel, Tunnel.State.DOWN, null)
                }
            } catch (_: Exception) { }

            // Exponential backoff retry
            var delayMs = 1000L
            var attempt = 0

            while (!isUserDisconnect && _connectionState.value == ConnectionState.CONNECTING) {
                attempt++
                if (BuildConfig.DEBUG) Log.i(TAG, "Reconnect attempt $attempt (delay: ${delayMs}ms)")
                logService.logService("Reconnect attempt $attempt", LogLevel.INFO)

                try {
                    val result = vpnRepository.connect(server.id)
                    val response = result.getOrElse { e ->
                        if (BuildConfig.DEBUG) Log.w(TAG, "Reconnect API failed", e)
                        logService.logService("Reconnect attempt $attempt failed: API error", LogLevel.WARNING)
                        val jitter = Random.nextDouble(0.5, 1.5)
                        delay((delayMs * jitter).toLong())
                        delayMs = (delayMs * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
                        return@launch // Don't retry API failures indefinitely
                    }

                    val ipv6Enabled = settingsPreferences.ipv6Enabled.first()
                    val splitTunneling = readSplitTunnelingSettings()
                    val config = TunnelConfigBuilder.build(response.config, ipv6Enabled, splitTunneling)

                    withContext(Dispatchers.IO) {
                        backend.setState(tunnel, Tunnel.State.UP, config)
                    }

                    // Resolve endpoint IP
                    val endpointHost = response.config.peer.endpoint.substringBefore(":")
                    _vpnIp.value = try {
                        withContext(Dispatchers.IO) {
                            java.net.InetAddress.getByName(endpointHost).hostAddress
                        }
                    } catch (_: Exception) { null }

                    // Success
                    _connectionState.value = ConnectionState.CONNECTED
                    _connectionStartTime.value = System.currentTimeMillis()
                    previousRxBytes = 0
                    previousTxBytes = 0
                    consecutiveStatsFailures = 0
                    startStatsPolling()
                    if (BuildConfig.DEBUG) Log.i(TAG, "Reconnected successfully")
                    logService.logService("Reconnected successfully after attempt $attempt")
                    return@launch

                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) Log.w(TAG, "Reconnect attempt $attempt failed", e)
                    logService.logService("Reconnect attempt $attempt failed: ${e.message}", LogLevel.WARNING)
                    val jitter = Random.nextDouble(0.5, 1.5)
                    delay((delayMs * jitter).toLong())
                    delayMs = (delayMs * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
                }
            }
        }
    }

    private fun startStatsPolling() {
        statsJob?.cancel()
        statsJob = scope.launch {
            while (true) {
                delay(Config.statisticsPollingIntervalMs)
                try {
                    val stats = withContext(Dispatchers.IO) {
                        backend.getStatistics(tunnel)
                    }

                    consecutiveStatsFailures = 0

                    val rxBytes = stats.totalRx()
                    val txBytes = stats.totalTx()

                    val downloadSpeed = if (previousRxBytes > 0) {
                        ((rxBytes - previousRxBytes) * 8.0) / 1_000_000.0
                    } else 0.0

                    val uploadSpeed = if (previousTxBytes > 0) {
                        ((txBytes - previousTxBytes) * 8.0) / 1_000_000.0
                    } else 0.0

                    previousRxBytes = rxBytes
                    previousTxBytes = txBytes

                    val startTime = _connectionStartTime.value
                    val connectionTimeSec = if (startTime != null) {
                        (System.currentTimeMillis() - startTime) / 1000
                    } else 0L

                    val totalDataGb = (rxBytes + txBytes) / (1024.0 * 1024.0 * 1024.0)

                    _statistics.value = ConnectionStats(
                        downloadSpeed = downloadSpeed.coerceAtLeast(0.0),
                        uploadSpeed = uploadSpeed.coerceAtLeast(0.0),
                        connectionTime = connectionTimeSec,
                        dataTransferred = totalDataGb
                    )
                } catch (e: Exception) {
                    when (e) {
                        is java.io.IOException -> {
                            // Network-level failure — count toward reconnect threshold
                            if (System.currentTimeMillis() < gracePeriodUntil) {
                                if (BuildConfig.DEBUG) Log.d(TAG, "Stats error during grace period — ignoring")
                            } else {
                                consecutiveStatsFailures++
                                if (BuildConfig.DEBUG) Log.w(TAG, "Stats polling error ($consecutiveStatsFailures)", e)

                                if (consecutiveStatsFailures >= STATS_FAILURE_THRESHOLD) {
                                    if (BuildConfig.DEBUG) Log.w(TAG, "Connection appears to have dropped")
                                    logService.logService("Connection lost - initiating auto-reconnect", LogLevel.WARNING)
                                    stopStatsPolling()
                                    attemptReconnect()
                                    return@launch
                                }
                            }
                        }
                        else -> {
                            // Backend/internal error — log but do not trigger reconnect
                            if (BuildConfig.DEBUG) Log.w(TAG, "Stats backend error (non-network), skipping", e)
                        }
                    }
                }
            }
        }
    }

    private fun stopStatsPolling() {
        statsJob?.cancel()
        statsJob = null
    }

    fun reconnect() {
        val server = currentServer ?: return
        scope.launch {
            // Disconnect first
            stopStatsPolling()
            try {
                withContext(Dispatchers.IO) {
                    backend.setState(tunnel, Tunnel.State.DOWN, null)
                }
            } catch (_: Exception) { }

            try {
                vpnRepository.disconnect()
            } catch (_: Exception) { }

            _connectionState.value = ConnectionState.DISCONNECTED
            _statistics.value = ConnectionStats()

            // Short delay then reconnect
            delay(500)
            connect(server)
        }
    }

    private suspend fun readSplitTunnelingSettings(): SplitTunnelingSettings {
        return SplitTunnelingSettings(
            enabled = settingsPreferences.splitTunnelingEnabled.first(),
            mode = settingsPreferences.splitTunnelingMode.first(),
            apps = settingsPreferences.splitTunnelingApps.first(),
            ips = settingsPreferences.splitTunnelingIps.first()
        )
    }

    fun pauseStatsPolling() {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            stopStatsPolling()
        }
    }

    fun resumeStatsPolling() {
        if (_connectionState.value == ConnectionState.CONNECTED && statsJob == null) {
            // Set grace period so the first ~8 seconds after resume don't trigger false reconnects
            gracePeriodUntil = System.currentTimeMillis() + FOREGROUND_GRACE_PERIOD_MS
            consecutiveStatsFailures = 0
            startStatsPolling()
        }
    }
}
