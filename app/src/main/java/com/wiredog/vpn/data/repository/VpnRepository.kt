package com.wiredog.vpn.data.repository

import com.wiredog.vpn.data.local.keystore.SecureStorage
import com.wiredog.vpn.data.remote.api.WireDogApi
import com.wiredog.vpn.data.remote.api.dto.ConnectRequest
import com.wiredog.vpn.data.remote.api.dto.ConnectResponse
import com.wiredog.vpn.data.remote.api.dto.DisconnectRequest
import com.wiredog.vpn.domain.model.VpnConnectException
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnRepository @Inject constructor(
    private val api: WireDogApi,
    private val secureStorage: SecureStorage
) {
    private var currentSessionId: String? = null

    suspend fun connect(
        serverId: String,
        blockAds: Boolean = true,
        blockMalware: Boolean = true
    ): Result<ConnectResponse> {
        return try {
            val response = api.vpnConnect(ConnectRequest(serverId, blockAds, blockMalware))
            currentSessionId = response.sessionId
            secureStorage.saveSessionId(response.sessionId)
            Result.success(response)
        } catch (e: HttpException) {
            Result.failure(mapConnectError(e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapConnectError(e: HttpException): Exception {
        return if (e.code() == 429) {
            VpnConnectException("You've reached your 5-device connection limit. Disconnect another device to continue.")
        } else {
            e
        }
    }

    suspend fun disconnect(): Result<Boolean> {
        val sessionId = currentSessionId ?: secureStorage.getSessionId() ?: return Result.success(true)
        // Clear local session accounting immediately — the tunnel is already down locally either
        // way. Only the backend notification below is retried on failure.
        currentSessionId = null
        secureStorage.clearSessionId()
        secureStorage.addPendingDisconnect(sessionId)
        return try {
            api.vpnDisconnect(DisconnectRequest(sessionId))
            secureStorage.removePendingDisconnect(sessionId)
            Result.success(true)
        } catch (e: Exception) {
            // Left in the pending list — retried via retryPendingDisconnects() on next
            // launch/foreground instead of leaking the backend's device-count slot forever.
            Result.success(true)
        }
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
        return try {
            api.vpnDisconnect(DisconnectRequest(sessionId))
            secureStorage.removePendingDisconnect(sessionId)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Retries any /disconnect calls that were owed but never confirmed. Safe to call
     * unconditionally/repeatedly — the backend's /disconnect is idempotent.
     */
    suspend fun retryPendingDisconnects() {
        for (sessionId in secureStorage.getPendingDisconnectIds()) {
            try {
                api.vpnDisconnect(DisconnectRequest(sessionId))
                secureStorage.removePendingDisconnect(sessionId)
            } catch (e: Exception) {
                // Still pending — retried again next time.
            }
        }
    }
}
