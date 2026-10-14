package com.wiredog.vpn.data.remote.api

import org.json.JSONObject
import retrofit2.HttpException

/**
 * A backend error body, `{ "error": "...", "code": "..." }`. `code` is only present on the few
 * errors the app branches on (e.g. two_factor_challenge_expired, server_not_available).
 */
data class ApiError(val status: Int, val code: String?, val message: String?)

/**
 * Reads the error body, so call it at most once per exception (the body can only be consumed once).
 * Returns null for anything that isn't an HTTP error. The body is parsed, never logged.
 */
fun Throwable.toApiError(): ApiError? {
    val http = this as? HttpException ?: return null
    val body = try { http.response()?.errorBody()?.string() } catch (_: Exception) { null }
    var code: String? = null
    var message: String? = null
    if (!body.isNullOrBlank()) {
        try {
            val json = JSONObject(body)
            code = json.optString("code").takeIf { it.isNotBlank() }
            message = json.optString("error").takeIf { it.isNotBlank() }
        } catch (_: Exception) { }
    }
    return ApiError(http.code(), code, message)
}
