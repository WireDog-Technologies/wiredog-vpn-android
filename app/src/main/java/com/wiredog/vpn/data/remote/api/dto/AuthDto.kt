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
    // Absent when the account has 2FA on: the response is then a challenge to redeem at
    // auth/2fa/verify instead of a session.
    @Json(name = "token") val token: String? = null,
    @Json(name = "user") val user: UserProfileDto? = null,
    @Json(name = "requiresTwoFactor") val requiresTwoFactor: Boolean = false,
    @Json(name = "challengeToken") val challengeToken: String? = null
)

@JsonClass(generateAdapter = true)
data class TwoFactorVerifyRequest(
    @Json(name = "challengeToken") val challengeToken: String,
    // A 6-digit authenticator code or a recovery code; the backend accepts either.
    @Json(name = "code") val code: String
)

@JsonClass(generateAdapter = true)
data class SsoLookupResponse(
    @Json(name = "sso") val sso: Boolean = false,
    @Json(name = "domain") val domain: String? = null
)

@JsonClass(generateAdapter = true)
data class SsoExchangeRequest(
    @Json(name = "code") val code: String,
    @Json(name = "codeVerifier") val codeVerifier: String
)

@JsonClass(generateAdapter = true)
data class ChangePasswordRequest(
    @Json(name = "currentPassword") val currentPassword: String,
    @Json(name = "newPassword") val newPassword: String
)

@JsonClass(generateAdapter = true)
data class VerifyResetCodeResponse(
    // True when the account has 2FA on: the reset itself must then carry a current 2FA code.
    @Json(name = "twoFactorRequired") val twoFactorRequired: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class UserProfileDto(
    @Json(name = "id") val id: String,
    @Json(name = "username") val username: String?,
    @Json(name = "accountNumber") val accountNumber: String?,
    @Json(name = "accountType") val accountType: String,
    @Json(name = "planTier") val planTier: String?,
    @Json(name = "subscriptionExpiresAt") val subscriptionExpiresAt: String?,
    // Business/organization state from auth/me. All defaulted so an older backend still decodes.
    @Json(name = "isActive") val isActive: Boolean = false,
    @Json(name = "totpEnabled") val totpEnabled: Boolean = false,
    @Json(name = "twoFactorRequiredByOrg") val twoFactorRequiredByOrg: Boolean = false,
    @Json(name = "mustChangePassword") val mustChangePassword: Boolean = false,
    @Json(name = "subscriptionManagedByOrganization") val subscriptionManagedByOrganization: Boolean = false,
    @Json(name = "organizationName") val organizationName: String? = null,
    @Json(name = "organizationRole") val organizationRole: String? = null,
    @Json(name = "organizationAccess") val organizationAccess: String? = null
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
    @Json(name = "newPassword") val newPassword: String,
    @Json(name = "twoFactorCode") val twoFactorCode: String? = null
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

@JsonClass(generateAdapter = true)
data class MilestoneRequest(
    @Json(name = "key") val key: String,
    // Without this the backend counts the milestone as iOS.
    @Json(name = "platform") val platform: String = "Android"
)
