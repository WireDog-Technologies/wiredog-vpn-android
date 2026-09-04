package com.wiredog.vpn.data.remote.api

import com.wiredog.vpn.data.remote.api.dto.AnnouncementDto
import com.wiredog.vpn.data.remote.api.dto.AnonymousAccountRequest
import com.wiredog.vpn.data.remote.api.dto.AnonymousLoginRequest
import com.wiredog.vpn.data.remote.api.dto.AnonymousRegisterResponse
import com.wiredog.vpn.data.remote.api.dto.AppConfigResponse
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
import com.wiredog.vpn.data.remote.api.dto.StandardAccountRequest
import com.wiredog.vpn.data.remote.api.dto.StandardAccountResponse
import com.wiredog.vpn.data.remote.api.dto.StandardLoginRequest
import com.wiredog.vpn.data.remote.api.dto.UserProfileDto
import com.wiredog.vpn.data.remote.api.dto.VerifyResetCodeRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST

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
    suspend fun verifyResetCode(@Body request: VerifyResetCodeRequest): MessageResponse

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): MessageResponse

    // Server endpoints
    @GET("vpn/servers")
    suspend fun getServers(): List<ServerDto>

    // VPN endpoints
    @POST("vpn/connect")
    suspend fun vpnConnect(@Body request: ConnectRequest): ConnectResponse

    @POST("vpn/disconnect")
    suspend fun vpnDisconnect(@Body request: DisconnectRequest): DisconnectResponse

    // Feedback endpoints
    @POST("feedback/report-issue")
    suspend fun reportBug(@Body request: ReportBugRequest): ReportBugResponse
}
