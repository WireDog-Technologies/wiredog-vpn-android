package com.wiredog.vpn.data.remote.api

import com.wiredog.vpn.data.remote.api.dto.AnnouncementDto
import com.wiredog.vpn.data.remote.api.dto.AnonymousAccountRequest
import com.wiredog.vpn.data.remote.api.dto.AnonymousLoginRequest
import com.wiredog.vpn.data.remote.api.dto.AnonymousRegisterResponse
import com.wiredog.vpn.data.remote.api.dto.AppConfigResponse
import com.wiredog.vpn.data.remote.api.dto.ChangePasswordRequest
import com.wiredog.vpn.data.remote.api.dto.ConnectRequest
import com.wiredog.vpn.data.remote.api.dto.ConnectResponse
import com.wiredog.vpn.data.remote.api.dto.DisconnectRequest
import com.wiredog.vpn.data.remote.api.dto.DisconnectResponse
import com.wiredog.vpn.data.remote.api.dto.ForgotPasswordRequest
import com.wiredog.vpn.data.remote.api.dto.HandoffTokenResponse
import com.wiredog.vpn.data.remote.api.dto.LoginResponse
import com.wiredog.vpn.data.remote.api.dto.LogoutResponse
import com.wiredog.vpn.data.remote.api.dto.MessageResponse
import com.wiredog.vpn.data.remote.api.dto.ReportBugRequest
import com.wiredog.vpn.data.remote.api.dto.ReportBugResponse
import com.wiredog.vpn.data.remote.api.dto.ResetPasswordRequest
import com.wiredog.vpn.data.remote.api.dto.ServerDto
import com.wiredog.vpn.data.remote.api.dto.SsoExchangeRequest
import com.wiredog.vpn.data.remote.api.dto.SsoLookupResponse
import com.wiredog.vpn.data.remote.api.dto.StandardAccountRequest
import com.wiredog.vpn.data.remote.api.dto.StandardAccountResponse
import com.wiredog.vpn.data.remote.api.dto.StandardLoginRequest
import com.wiredog.vpn.data.remote.api.dto.TwoFactorVerifyRequest
import com.wiredog.vpn.data.remote.api.dto.UserProfileDto
import com.wiredog.vpn.data.remote.api.dto.VerifyResetCodeRequest
import com.wiredog.vpn.data.remote.api.dto.VerifyResetCodeResponse
import com.wiredog.vpn.data.remote.api.dto.MilestoneRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface WireDogApi {

    // App config
    @GET("app/config")
    suspend fun getAppConfig(): AppConfigResponse

    // In-app announcements (public, no auth)
    @GET("app/announcements")
    suspend fun getAnnouncements(): List<AnnouncementDto>

    // Auth endpoints
    @POST("auth/login")
    suspend fun loginStandard(@Body request: StandardLoginRequest): LoginResponse

    @POST("auth/login")
    suspend fun loginAnonymous(@Body request: AnonymousLoginRequest): LoginResponse

    // Second step of a login that returned requiresTwoFactor.
    @POST("auth/2fa/verify")
    suspend fun verifyTwoFactor(@Body request: TwoFactorVerifyRequest): LoginResponse

    // Public: does this email's domain have verified SSO?
    @GET("auth/login/lookup")
    suspend fun loginLookup(@Query("identifier") identifier: String): SsoLookupResponse

    @POST("auth/sso/exchange")
    suspend fun ssoExchange(@Body request: SsoExchangeRequest): LoginResponse

    // Admin-provisioned employees replace their temporary password with this.
    @PATCH("auth/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): MessageResponse

    // Pre-payment funnel milestone (aggregate-only counter on the backend; 204 No Content).
    @POST("auth/milestones")
    suspend fun recordMilestone(@Body request: MilestoneRequest)

    @POST("auth/logout")
    suspend fun logout(): LogoutResponse

    @GET("auth/me")
    suspend fun getProfile(): UserProfileDto

    @POST("auth/register/standard")
    suspend fun registerStandard(@Body request: StandardAccountRequest): StandardAccountResponse

    @POST("auth/register/anonymous")
    suspend fun registerAnonymous(@Body request: AnonymousAccountRequest): AnonymousRegisterResponse

    // Mints a one-time code so the checkout website recognizes this already-authenticated user.
    @POST("auth/handoff-token")
    suspend fun handoffToken(): HandoffTokenResponse

    @DELETE("auth/account")
    suspend fun deleteAccount(): MessageResponse

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): MessageResponse

    @POST("auth/verify-reset-code")
    suspend fun verifyResetCode(@Body request: VerifyResetCodeRequest): VerifyResetCodeResponse

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): MessageResponse

    // Server endpoints. gateways=1 opts in to organization Dedicated IP gateway entries; the
    // backend returns the plain list for callers that omit it.
    @GET("vpn/servers")
    suspend fun getServers(@Query("gateways") gateways: Int = 1): List<ServerDto>

    // VPN endpoints
    @POST("vpn/connect")
    suspend fun vpnConnect(@Body request: ConnectRequest): ConnectResponse

    @POST("vpn/disconnect")
    suspend fun vpnDisconnect(@Body request: DisconnectRequest): DisconnectResponse

    // Feedback endpoints
    @POST("feedback/report-issue")
    suspend fun reportBug(@Body request: ReportBugRequest): ReportBugResponse
}
