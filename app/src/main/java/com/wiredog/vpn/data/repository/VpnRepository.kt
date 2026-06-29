package com.wiredog.vpn.data.repository

import com.wiredog.vpn.data.local.keystore.SecureStorage
import com.wiredog.vpn.data.remote.api.WireDogApi
import com.wiredog.vpn.data.remote.api.dto.ConnectRequest
import com.wiredog.vpn.data.remote.api.dto.ConnectResponse
import com.wiredog.vpn.data.remote.api.dto.DisconnectRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnRepository @Inject constructor(
    private val api: WireDogApi,
    private val secureStorage: SecureStorage
) {
    private var currentSessionId: String? = null

    suspend fun connect(serverId: String): Result<ConnectResponse> {
        return try {
            val response = api.vpnConnect(ConnectRequest(serverId))
            currentSessionId = response.sessionId
            secureStorage.saveSessionId(response.sessionId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun disconnect(): Result<Boolean> {
        val sessionId = currentSessionId ?: secureStorage.getSessionId() ?: return Result.success(true)
        return try {
            api.vpnDisconnect(DisconnectRequest(sessionId))
            currentSessionId = null
            secureStorage.clearSessionId()
            Result.success(true)
        } catch (e: Exception) {
            // Clear session even if API call fails - tunnel is already down locally
            currentSessionId = null
            secureStorage.clearSessionId()
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
        return try {
            api.vpnDisconnect(DisconnectRequest(sessionId))
            secureStorage.clearSessionId()
            true
        } catch (e: Exception) {
            secureStorage.clearSessionId()
            false
        }
    }
}
