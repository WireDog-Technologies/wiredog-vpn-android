package com.wiredog.vpn.service.vpn

import android.content.Context
import android.content.Intent
import android.util.Log
import com.wiredog.vpn.BuildConfig
import com.wiredog.vpn.data.config.Config
import com.wiredog.vpn.data.local.preferences.ReviewPromptPreferences
import com.wiredog.vpn.data.local.preferences.SettingsPreferences
import com.wiredog.vpn.data.logging.LogLevel
import com.wiredog.vpn.data.logging.LogService
import com.wiredog.vpn.data.repository.ServerRepository
import com.wiredog.vpn.data.repository.VpnRepository
import com.wiredog.vpn.domain.model.ConnectionState
import com.wiredog.vpn.domain.model.ConnectionStats
import com.wiredog.vpn.domain.model.Server
import com.wiredog.vpn.domain.model.SplitTunnelingSettings
import com.wiredog.vpn.domain.model.VpnConnectException
import org.amnezia.awg.backend.GoBackend
import org.amnezia.awg.backend.Tunnel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class VpnConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vpnRepository: VpnRepository,
    private val serverRepository: ServerRepository,
    private val settingsPreferences: SettingsPreferences,
    private val reviewPromptPreferences: ReviewPromptPreferences,
    private val logService: LogService
) {
    companion object {
        private const val TAG = "VpnConnectionManager"
        private const val MAX_RECONNECT_DELAY_MS = 15_000L
        private const val STATS_FAILURE_THRESHOLD = 5
        // Grace period on foreground resume: skip health checks for this many ms
        private const val FOREGROUND_GRACE_PERIOD_MS = 8_000L
        private const val MAX_RECONNECT_ATTEMPTS = 10
        // A connection that drops before staying up this long never really established — clean
        // up its backend session slot immediately instead of blindly reconnecting and leaking
        // another increment on top of it.
        private const val MINIMUM_STABLE_CONNECTION_DURATION_MS = 30_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val backend: GoBackend by lazy { GoBackend(context) }
    private val tunnel = WireDogTunnel()

    init {
        scope.launch { runStartupCleanup() }
    }

    private suspend fun runStartupCleanup() {
        try {
            val tunnelState = withContext(Dispatchers.IO) { backend.getState(tunnel) }
            if (tunnelState == Tunnel.State.DOWN) {
                val cleaned = try {
                    vpnRepository.cleanupStaleSession()
                } catch (_: Exception) {
                    false
                }
                if (cleaned && BuildConfig.DEBUG) Log.i(TAG, "Startup cleanup: stale session counter decremented")
            }
        } catch (_: Exception) { }

        // Retry any /disconnect calls that were owed but never confirmed — e.g. the app had no
        // connectivity right as a previous cleanup call went out. Independent of the crash-recovery
        // check above, which only covers the single current session. Safe to call unconditionally.
        try {
            vpnRepository.retryPendingDisconnects()
        } catch (_: Exception) { }
    }

    /** Retries any /disconnect calls that never confirmed. Safe to call unconditionally/repeatedly. */
    fun retryPendingDisconnects() {
        scope.launch {
            try {
                vpnRepository.retryPendingDisconnects()
            } catch (_: Exception) { }
        }
    }

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _statistics = MutableStateFlow(ConnectionStats())
    val statistics: StateFlow<ConnectionStats> = _statistics.asStateFlow()

    private val _connectionStartTime = MutableStateFlow<Long?>(null)
    val connectionStartTime: StateFlow<Long?> = _connectionStartTime.asStateFlow()

    private val _vpnIp = MutableStateFlow<String?>(null)
    val vpnIp: StateFlow<String?> = _vpnIp.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private val _showReviewPrompt = MutableStateFlow(false)
    val showReviewPrompt: StateFlow<Boolean> = _showReviewPrompt.asStateFlow()
    private var hasRecordedReviewPromptForCurrentConnection = false

    private val _isReconnecting = MutableStateFlow(false)
    val isReconnecting: StateFlow<Boolean> = _isReconnecting.asStateFlow()

    // True while a server switch is tearing down the old tunnel before bringing up the new one.
    // Lets the UI (e.g. the map marker) treat that whole disconnect-old -> connect-new sequence
    // as one continuous "in progress" state instead of flickering through the intermediate
    // connected/disconnected values connectionState genuinely passes through.
    private val _isSwitchingServer = MutableStateFlow(false)
    val isSwitchingServer: StateFlow<Boolean> = _isSwitchingServer.asStateFlow()

    private var statsJob: Job? = null
    private var previousRxBytes: Long = 0
    private var previousTxBytes: Long = 0
    private var consecutiveStatsFailures: Int = 0

    // Track current server for auto-reconnect
    private var currentServer: Server? = null
    private var isUserDisconnect: Boolean = false
    // Foreground grace period: timestamp after which health checks resume
    private var gracePeriodUntil: Long = 0L
    // The single in-flight connect or reconnect coroutine, if any — lets cancelConnect() stop
    // whichever one is currently running.
    private var activeJob: Job? = null
    private var reconnectAttempts: Int = 0

    fun prepareVpn(): Intent? {
        return GoBackend.VpnService.prepare(context)
    }

    fun clearConnectionError() {
        _connectionError.value = null
    }

    fun acknowledgeReviewPrompt() {
        _showReviewPrompt.value = false
    }

    fun recordReviewPromptPositiveResponse() {
        scope.launch { reviewPromptPreferences.recordPositiveResponse() }
    }

    private suspend fun maybeShowReviewPrompt() {
        if (hasRecordedReviewPromptForCurrentConnection) return
        hasRecordedReviewPromptForCurrentConnection = true
        if (reviewPromptPreferences.recordSuccessfulConnection()) {
            _showReviewPrompt.value = true
        }
    }

    fun connect(server: Server) {
        // `_connectionState` only flips to CONNECTING inside the launched coroutine below, so
        // two rapid connect() calls (double-tap, or a stale-state re-tap) could both get past a
        // state check and each fire their own /connect — leaking one session's counter slot.
        // `scope` is the main dispatcher, so this check + the assignment below are serialized.
        if (activeJob?.isActive == true) return
        if (_connectionState.value == ConnectionState.CONNECTING ||
            _connectionState.value == ConnectionState.CONNECTED) {
            return
        }

        isUserDisconnect = false
        currentServer = server
        _connectionError.value = null
        hasRecordedReviewPromptForCurrentConnection = false
        reconnectAttempts = 0

        activeJob = scope.launch {
            _connectionState.value = ConnectionState.CONNECTING
            logService.logService("Connection attempt to ${server.displayName}")

            try {
                // 1. Read settings
                val ipv6Enabled = settingsPreferences.ipv6Enabled.first()
                val blockAds = settingsPreferences.blockAdsEnabled.first()
                val blockMalware = settingsPreferences.blockMalwareEnabled.first()
                val splitTunneling = readSplitTunnelingSettings()

                // 2. Call API to get WireGuard config. vpnRepository.connect() runs the request
                // itself NonCancellable so the sessionId is always captured even if we were
                // cancelled mid-request — the ensureActive() below then routes that cancel to
                // the cleanup handler, which releases the just-created session.
                val result = vpnRepository.connect(server.id, blockAds, blockMalware)
                val response = result.getOrElse { e ->
                    if (e is CancellationException) throw e
                    if (BuildConfig.DEBUG) Log.e(TAG, "API connect failed", e)
                    logService.logService("Connection failed: API error", LogLevel.ERROR)
                    _connectionState.value = ConnectionState.DISCONNECTED
                    _connectionError.value = (e as? VpnConnectException)?.message
                        ?: "Unable to connect. Please try again."
                    // Belt-and-suspenders: no-ops if no session was claimed, but releases one
                    // if the failure somehow arrived after the backend created the session.
                    try { vpnRepository.disconnect() } catch (_: Exception) { }
                    return@launch
                }

                // A cancel (or disconnect()) that landed while the NonCancellable /connect was
                // in flight lands here — throw into the CancellationException handler so the
                // session that was just created server-side gets released.
                ensureActive()

                // 3. Build WireGuard Config from API response
                val config = TunnelConfigBuilder.build(response.config, ipv6Enabled, splitTunneling)

                // 4. Start tunnel via GoBackend
                withContext(Dispatchers.IO) {
                    backend.setState(tunnel, Tunnel.State.UP, config)
                }

                // A cancel/disconnect that landed while the tunnel was coming up: tear it back
                // down via the CancellationException handler rather than flashing CONNECTED.
                ensureActive()

                // 5. Display the backend-assigned exit IP verbatim — it's authoritative (the
                // backend SNATs the session to this address). Never probe a third-party
                // IP-echo service or resolve the peer endpoint (that's the entry/handshake
                // host, not the exit IP). A blank value means "unknown" — the UI shows that.
                _vpnIp.value = response.server?.exitIp?.takeIf { it.isNotBlank() }

                // 6. Mark connected
                _connectionState.value = ConnectionState.CONNECTED
                _connectionStartTime.value = System.currentTimeMillis()
                previousRxBytes = 0
                previousTxBytes = 0
                consecutiveStatsFailures = 0
                reconnectAttempts = 0

                // 7. Save last connected server
                serverRepository.setLastConnectedServer(server.id)

                // 8. Start stats polling
                startStatsPolling()

                maybeShowReviewPrompt()

                if (BuildConfig.DEBUG) Log.i(TAG, "Connected to ${server.displayName}")
                logService.logService("Connected to ${server.displayName}")

            } catch (e: CancellationException) {
                // User-initiated cancel (tap-again-to-cancel) — expected, not a failure. No
                // _connectionError set, so no error surfaces to the user.
                logService.logService("Connect cancelled")
                withContext(NonCancellable) {
                    try {
                        withContext(Dispatchers.IO) {
                            backend.setState(tunnel, Tunnel.State.DOWN, null)
                        }
                    } catch (_: Exception) { }
                    // Release the session if the API call already obtained one before the
                    // cancel landed — vpnRepository.disconnect() no-ops if there's none.
                    try {
                        vpnRepository.disconnect()
                    } catch (_: Exception) { }
                }
                _connectionState.value = ConnectionState.DISCONNECTED
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Connection failed", e)
                logService.logService("Connection failed: ${e.message}", LogLevel.ERROR)
                withContext(NonCancellable) {
                    try {
                        withContext(Dispatchers.IO) {
                            backend.setState(tunnel, Tunnel.State.DOWN, null)
                        }
                    } catch (_: Exception) { }
                    // If the API call already obtained a session before this later step failed,
                    // the backend's device counter would otherwise leak until the next
                    // crash-recovery check. Releasing before we publish DISCONNECTED keeps a
                    // switchServer()/reconnect waiting on that state from starting a new session
                    // while this one's release is still in flight.
                    try {
                        vpnRepository.disconnect()
                    } catch (_: Exception) { }
                }
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    /** Cancels an in-progress connect or auto-reconnect. No-op unless currently CONNECTING. */
    fun cancelConnect() {
        if (_connectionState.value != ConnectionState.CONNECTING) return
        logService.logService("Cancel connect requested")
        isUserDisconnect = true
        _isReconnecting.value = false
        activeJob?.cancel()
    }

    /**
     * Switches the live tunnel to a different server: tears down whatever's currently
     * connected/connecting, waits for it to settle, then connects to [server]. No-op if
     * [server] is already the current one.
     */
    fun switchServer(server: Server) {
        if (currentServer?.id == server.id) return
        _isSwitchingServer.value = true
        val wasConnecting = _connectionState.value == ConnectionState.CONNECTING
        scope.launch {
            if (wasConnecting) cancelConnect() else disconnect()
            val reachedDisconnected = withTimeoutOrNull(8_000L) {
                connectionState.first { it == ConnectionState.DISCONNECTED }
            } != null
            if (!reachedDisconnected) {
                _connectionError.value = "Unable to switch servers. Please try again."
                _isSwitchingServer.value = false
                return@launch
            }
            connect(server)
            activeJob?.join()
            _isSwitchingServer.value = false
        }
    }

    fun disconnect() {
        if (_connectionState.value == ConnectionState.DISCONNECTED ||
            _connectionState.value == ConnectionState.DISCONNECTING) {
            return
        }

        isUserDisconnect = true
        _isReconnecting.value = false
        // Stop whichever connect/reconnect attempt is in flight, if any, so it doesn't race
        // the teardown below.
        activeJob?.cancel()

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
                hasRecordedReviewPromptForCurrentConnection = false
            }
        }
    }

    private fun attemptReconnect() {
        val server = currentServer ?: return
        if (isUserDisconnect) return

        if (BuildConfig.DEBUG) Log.i(TAG, "Attempting auto-reconnect to ${server.displayName}")
        logService.logService("Auto-reconnecting to ${server.displayName}")

        activeJob = scope.launch {
            _connectionState.value = ConnectionState.CONNECTING
            _isReconnecting.value = true

            suspend fun giveUp(reason: String) {
                logService.logService(reason, LogLevel.ERROR)
                // Release any session this attempt (or a prior one) claimed — no-ops if none.
                try { vpnRepository.disconnect() } catch (_: Exception) { }
                currentServer = null
                _connectionState.value = ConnectionState.DISCONNECTED
                _isReconnecting.value = false
                _connectionError.value = reason
            }

            try {
                // Try to tear down old tunnel first. Deliberately inside this try block (not a
                // separate catch-all before it) so a cancel landing in this narrow window is
                // caught by the CancellationException handler below instead of being silently
                // swallowed by a bare `catch (_: Exception)`, which would leave the state stuck
                // at CONNECTING forever.
                try {
                    withContext(Dispatchers.IO) {
                        backend.setState(tunnel, Tunnel.State.DOWN, null)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { }

                // Exponential backoff retry, capped at MAX_RECONNECT_ATTEMPTS so a dead server
                // slot doesn't retry forever and hold a session open indefinitely.
                var delayMs = 1000L

                while (!isUserDisconnect && _connectionState.value == ConnectionState.CONNECTING) {
                    if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                        giveUp("Reconnection failed after $MAX_RECONNECT_ATTEMPTS attempts")
                        return@launch
                    }
                    reconnectAttempts++
                    val attempt = reconnectAttempts
                    if (BuildConfig.DEBUG) Log.i(TAG, "Reconnect attempt $attempt (delay: ${delayMs}ms)")
                    logService.logService("Reconnect attempt $attempt", LogLevel.INFO)

                    val ipv6Enabled = settingsPreferences.ipv6Enabled.first()
                    val blockAds = settingsPreferences.blockAdsEnabled.first()
                    val blockMalware = settingsPreferences.blockMalwareEnabled.first()
                    val splitTunneling = readSplitTunnelingSettings()

                    val result = vpnRepository.connect(server.id, blockAds, blockMalware)
                    val response = result.getOrNull()
                    if (response == null) {
                        if (BuildConfig.DEBUG) Log.w(TAG, "Reconnect API failed", result.exceptionOrNull())
                        // Don't retry API failures indefinitely (e.g. device-limit, auth) — no
                        // session was claimed on this branch, so there's nothing to release.
                        giveUp("Unable to reconnect. Please try again.")
                        return@launch
                    }

                    try {
                        val config = TunnelConfigBuilder.build(response.config, ipv6Enabled, splitTunneling)

                        withContext(Dispatchers.IO) {
                            backend.setState(tunnel, Tunnel.State.UP, config)
                        }

                        // Display the backend-assigned exit IP verbatim (see connect() above).
                        // Blank = unknown.
                        _vpnIp.value = response.server?.exitIp?.takeIf { it.isNotBlank() }

                        // Success
                        _connectionState.value = ConnectionState.CONNECTED
                        _connectionStartTime.value = System.currentTimeMillis()
                        previousRxBytes = 0
                        previousTxBytes = 0
                        consecutiveStatsFailures = 0
                        reconnectAttempts = 0
                        _isReconnecting.value = false
                        startStatsPolling()
                        maybeShowReviewPrompt()
                        if (BuildConfig.DEBUG) Log.i(TAG, "Reconnected successfully")
                        logService.logService("Reconnected successfully after attempt $attempt")
                        return@launch

                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) Log.w(TAG, "Reconnect attempt $attempt failed", e)
                        logService.logService("Reconnect attempt $attempt failed: ${e.message}", LogLevel.WARNING)
                        // This attempt claimed a session via vpnRepository.connect() above but
                        // never brought the tunnel up — release it before backing off, so a
                        // failed attempt doesn't stack a leaked session on top of the next retry.
                        try { vpnRepository.disconnect() } catch (_: Exception) { }
                        val jitter = Random.nextDouble(0.5, 1.5)
                        delay((delayMs * jitter).toLong())
                        delayMs = (delayMs * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
                    }
                }
            } catch (e: CancellationException) {
                logService.logService("Reconnect cancelled")
                withContext(NonCancellable) {
                    try {
                        withContext(Dispatchers.IO) {
                            backend.setState(tunnel, Tunnel.State.DOWN, null)
                        }
                    } catch (_: Exception) { }
                    try { vpnRepository.disconnect() } catch (_: Exception) { }
                }
                currentServer = null
                _connectionState.value = ConnectionState.DISCONNECTED
                _isReconnecting.value = false
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
                                    // Release the dropped session's counter slot before reconnecting —
                                    // attemptReconnect() obtains a fresh session and overwrites the id,
                                    // so nothing else would ever tell the backend about this one. If
                                    // the network is genuinely down the call fails and the id stays in
                                    // the pending-disconnect queue for retry.
                                    try { vpnRepository.disconnect() } catch (_: Exception) { }
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
