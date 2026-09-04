package com.wiredog.vpn.data.remote.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StandardLoginRequest(
    @Json(name = "identifier") val identifier: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class AnonymousLoginRequest(
    @Json(name = "identifier") val identifier: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "token") val token: String,
    @Json(name = "user") val user: UserProfileDto? = null
)

@JsonClass(generateAdapter = true)
data class UserProfileDto(
    @Json(name = "id") val id: String,
    @Json(name = "username") val username: String?,
    @Json(name = "accountNumber") val accountNumber: String?,
    @Json(name = "accountType") val accountType: String,
    @Json(name = "planTier") val planTier: String?,
    @Json(name = "subscriptionExpiresAt") val subscriptionExpiresAt: String?
)

@JsonClass(generateAdapter = true)
data class LogoutResponse(
    @Json(name = "message") val message: String
)

@JsonClass(generateAdapter = true)
data class HandoffTokenResponse(
    @Json(name = "token") val token: String,
    @Json(name = "expiresIn") val expiresIn: Int
)

@JsonClass(generateAdapter = true)
data class StandardAccountRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "referralCode") val referralCode: String? = null,
    // Surface the account was created from — backend records it for signup metrics.
    @Json(name = "platform") val platform: String = "Android"
)

@JsonClass(generateAdapter = true)
data class StandardAccountResponse(
    @Json(name = "message") val message: String,
    @Json(name = "accountNumber") val accountNumber: String
)

@JsonClass(generateAdapter = true)
data class AnonymousAccountRequest(
    // Surface the account was created from — backend records it for signup metrics.
    @Json(name = "platform") val platform: String = "Android"
)

@JsonClass(generateAdapter = true)
data class AnonymousRegisterResponse(
    @Json(name = "accountNumber") val accountNumber: String,
    @Json(name = "token") val token: String? = null,
    @Json(name = "user") val user: UserProfileDto? = null
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequest(
    @Json(name = "email") val email: String
)

@JsonClass(generateAdapter = true)
data class VerifyResetCodeRequest(
    @Json(name = "email") val email: String,
    @Json(name = "code") val code: String
)

@JsonClass(generateAdapter = true)
data class ResetPasswordRequest(
    @Json(name = "email") val email: String,
    @Json(name = "code") val code: String,
    @Json(name = "newPassword") val newPassword: String
)

@JsonClass(generateAdapter = true)
data class MessageResponse(
    @Json(name = "message") val message: String
)

@JsonClass(generateAdapter = true)
data class ReportBugRequest(
    @Json(name = "email") val email: String,
    @Json(name = "subject") val subject: String,
    @Json(name = "message") val message: String,
    @Json(name = "os") val os: String = "Android",
    @Json(name = "osVersion") val osVersion: String,
    @Json(name = "vpnVersion") val vpnVersion: String
)

@JsonClass(generateAdapter = true)
data class ReportBugResponse(
    @Json(name = "message") val message: String,
    @Json(name = "issueId") val issueId: Int
)
