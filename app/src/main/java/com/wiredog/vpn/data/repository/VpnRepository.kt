package com.wiredog.vpn.data.repository

import com.wiredog.vpn.data.local.keystore.SecureStorage
import com.wiredog.vpn.data.logging.LogLevel
import com.wiredog.vpn.data.logging.LogService
import com.wiredog.vpn.data.remote.api.WireDogApi
import com.wiredog.vpn.data.remote.api.dto.ConnectRequest
import com.wiredog.vpn.data.remote.api.dto.ConnectResponse
import com.wiredog.vpn.data.remote.api.dto.DisconnectRequest
import com.wiredog.vpn.domain.model.VpnConnectException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnRepository @Inject constructor(
    private val api: WireDogApi,
    private val secureStorage: SecureStorage,
    private val logService: LogService
) {
    private var currentSessionId: String? = null

    // The backend's /disconnect is NOT idempotent — endSession() unconditionally runs
    // `active_connection_counter = GREATEST(0, counter - 1)` on every valid call. Two
    // concurrent /disconnect calls for the same session double-decrement the device
    // counter. disconnect(), cleanupStaleSession() and retryPendingDisconnects() can all
    // fire for the same id within one launch (startup cleanup + every foreground onResume),
    // so every /disconnect goes through notifyDisconnect() below, which guarantees only one
    // call per session id is ever in flight at a time.
    private val disconnectMutex = Mutex()
    private val disconnectsInFlight = mutableSetOf<String>()

    suspend fun connect(
        serverId: String,
        blockAds: Boolean = true,
        blockMalware: Boolean = true
    ): Result<ConnectResponse> {
        return try {
            // NonCancellable: the backend creates the session (and increments the device
            // counter) as soon as it receives this request, and does NOT roll it back if the
            // client disconnects mid-flight. If the caller's coroutine is cancelled here
            // (rapid connect→disconnect), we MUST still read the response and persist the
            // sessionId — otherwise the session is orphaned server-side with no id on record
            // for anyone to release it. `saveSessionId` also covers a crash between here and
            // tunnel-up (crash-recovery reads it on next launch). The caller re-checks
            // cancellation right after this returns and runs its cleanup, which calls
            // disconnect() for this exact id.
            val response = withContext(NonCancellable) {
                val r = api.vpnConnect(ConnectRequest(serverId, blockAds, blockMalware))
                currentSessionId = r.sessionId
                secureStorage.saveSessionId(r.sessionId)
                r
            }
            Result.success(response)
        } catch (e: HttpException) {
            Result.failure(mapConnectError(e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapConnectError(e: HttpException): Exception {
        // The backend returns 429 both for real rate-limiting and for the 5-device connection
        // cap — tell them apart by the error message so a rate-limit isn't misreported as a
        // device cap (matches the iOS client).
        if (e.code() == 429) {
            val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
            if (body != null && body.contains("Connection limit exceeded")) {
                return VpnConnectException(
                    "You've reached your 5-device connection limit. Disconnect another device to continue."
                )
            }
        }
        return e
    }

    /**
     * Sends exactly one /disconnect for [sessionId], guaranteeing no other concurrent call is
     * doing the same (see [disconnectsInFlight]). Removes the id from the pending-disconnect
     * queue on success; leaves it there on failure or if a duplicate call was already in
     * flight, so it is retried later. Returns true only if this call confirmed the disconnect.
     */
    private suspend fun notifyDisconnect(sessionId: String): Boolean {
        val claimed = disconnectMutex.withLock {
            if (disconnectsInFlight.contains(sessionId)) false
            else disconnectsInFlight.add(sessionId)
        }
        if (!claimed) {
            logService.logService("VPN: /disconnect already in flight for this session — skipping duplicate")
            return false
        }
        return try {
            api.vpnDisconnect(DisconnectRequest(sessionId))
            secureStorage.removePendingDisconnect(sessionId)
            logService.logService("VPN: /disconnect confirmed by backend")
            true
        } catch (e: Exception) {
            // Left in the pending list — retried via retryPendingDisconnects() on next
            // launch/foreground instead of leaking the backend's device-count slot forever.
            logService.logService("VPN: /disconnect failed, left pending: ${e.message}", LogLevel.WARNING)
            false
        } finally {
            // NonCancellable: if the caller's coroutine was cancelled during the request, the
            // id must still be released here or that session could never be retried.
            withContext(NonCancellable) {
                disconnectMutex.withLock { disconnectsInFlight.remove(sessionId) }
            }
        }
    }

    suspend fun disconnect(): Result<Boolean> {
        val sessionId = currentSessionId ?: secureStorage.getSessionId() ?: return Result.success(true)
        // Clear local session accounting immediately — the tunnel is already down locally either
        // way. Only the backend notification is retried on failure.
        currentSessionId = null
        secureStorage.clearSessionId()
        secureStorage.addPendingDisconnect(sessionId)
        notifyDisconnect(sessionId)
        return Result.success(true)
    }

    /**
     * Called once at startup. If a sessionId was persisted from a previous run but the
     * tunnel is now down (crash/kill recovery), notify the backend to decrement the
     * connection counter. Clears the persisted sessionId regardless of API success.
     */
    suspend fun cleanupStaleSession(): Boolean {
        val sessionId = secureStorage.getSessionId() ?: return false
        secureStorage.clearSessionId()
        secureStorage.addPendingDisconnect(sessionId)
        return notifyDisconnect(sessionId)
    }

    /**
     * Retries any /disconnect calls that were owed but never confirmed. Safe to call
     * unconditionally/repeatedly — notifyDisconnect() collapses overlapping calls for the
     * same session so the non-idempotent backend counter is never double-decremented.
     */
    suspend fun retryPendingDisconnects() {
        for (sessionId in secureStorage.getPendingDisconnectIds()) {
            notifyDisconnect(sessionId)
        }
    }
}
