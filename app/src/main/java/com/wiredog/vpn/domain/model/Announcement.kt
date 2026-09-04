package com.wiredog.vpn.domain.model

import com.wiredog.vpn.data.remote.api.dto.AnnouncementDto
import java.time.Instant

enum class AnnouncementSeverity {
    INFO,
    MAINTENANCE,
    INCIDENT;

    companion object {
        fun fromApi(raw: String): AnnouncementSeverity = when (raw.lowercase()) {
            "maintenance" -> MAINTENANCE
            "incident" -> INCIDENT
            else -> INFO
        }
    }
}

data class Announcement(
    val id: String,
    val title: String,
    val body: String,
    val severity: AnnouncementSeverity,
    /** Parsed start time; null if the backend value could not be parsed. */
    val startAt: Instant?
)

fun AnnouncementDto.toDomain(): Announcement = Announcement(
    id = id,
    title = title,
    body = body,
    severity = AnnouncementSeverity.fromApi(severity),
    startAt = runCatching { Instant.parse(startAt) }.getOrNull()
)
