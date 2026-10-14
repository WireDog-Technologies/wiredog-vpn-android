package com.wiredog.vpn.data.remote.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppConfigResponse(
    @Json(name = "platforms") val platforms: PlatformsDto,
    @Json(name = "maintenanceMode") val maintenanceMode: Boolean,
    // Absent/null when the backend has no legal versions configured (or predates the field).
    @Json(name = "legal") val legal: LegalVersions? = null
)

// Currently published Privacy Policy / Terms of Service versions. Opaque strings, only compared
// for equality against what the user last accepted.
@JsonClass(generateAdapter = true)
data class LegalVersions(
    @Json(name = "privacyVersion") val privacyVersion: String,
    @Json(name = "termsVersion") val termsVersion: String,
    @Json(name = "summary") val summary: String? = null
)

@JsonClass(generateAdapter = true)
data class PlatformsDto(
    @Json(name = "android") val android: PlatformConfigDto
)

@JsonClass(generateAdapter = true)
data class PlatformConfigDto(
    @Json(name = "minSupportedVersion") val minSupportedVersion: Int,
    @Json(name = "latestVersion") val latestVersion: Int,
    @Json(name = "forceUpdate") val forceUpdate: Boolean,
    @Json(name = "updateMessage") val updateMessage: String? = null
)
