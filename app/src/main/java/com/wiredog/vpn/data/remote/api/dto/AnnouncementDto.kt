package com.wiredog.vpn.data.remote.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * One in-app announcement from `GET /app/announcements` (public, no auth). The backend
 * query is the source of truth for which announcements are live — whatever it returns is
 * exactly what should be shown, with no client-side lifecycle filtering.
 */
@JsonClass(generateAdapter = true)
data class AnnouncementDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "body") val body: String,
    // "info" | "maintenance" | "incident" — kept as a String; mapped to a domain enum
    // with a safe fallback so an unknown value can never crash the fetch.
    @Json(name = "severity") val severity: String,
    // Carried for backend organisation/reporting only — every active announcement is shown
    // to every user, so this is not used for filtering on the client.
    @Json(name = "target") val target: AnnouncementTargetDto? = null,
    @Json(name = "startAt") val startAt: String,
    @Json(name = "endAt") val endAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AnnouncementTargetDto(
    @Json(name = "type") val type: String,
    @Json(name = "value") val value: String? = null
)
