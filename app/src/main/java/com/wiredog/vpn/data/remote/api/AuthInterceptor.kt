package com.wiredog.vpn.data.remote.api

import com.wiredog.vpn.data.local.keystore.SecureStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val secureStorage: SecureStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip auth header for public endpoints
        val path = originalRequest.url.encodedPath
        if (path.endsWith("auth/login") || path.endsWith("auth/register") ||
            path.endsWith("auth/forgot-password") || path.endsWith("auth/verify-reset-code") ||
            path.endsWith("auth/reset-password") || path.endsWith("app/config") ||
            path.endsWith("app/announcements")) {
            return chain.proceed(originalRequest)
        }

        val token = secureStorage.getAuthToken()

        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(newRequest)

        // Sliding session renewal: the backend reissues a fresh token with a renewed expiry on
        // every authenticated request (X-Refreshed-Token header), so an actively-used app never
        // hits its token's flat TTL. Persist it whenever present — even on a non-2xx response,
        // since the token was still valid enough for the backend to make the renewal decision.
        response.header("X-Refreshed-Token")?.let { secureStorage.saveAuthToken(it) }

        // Clear stale token on 401 so the app returns to login state
        if (response.code == 401) {
            secureStorage.deleteAuthToken()
        }

        return response
    }
}
