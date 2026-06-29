package com.wiredog.vpn.data.remote.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppConfigResponse(
    @Json(name = "platforms") val platforms: PlatformsDto,
    @Json(name = "maintenanceMode") val maintenanceMode: Boolean
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
