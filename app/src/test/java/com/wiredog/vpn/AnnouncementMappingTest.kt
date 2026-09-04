package com.wiredog.vpn

import com.wiredog.vpn.data.remote.api.dto.AnnouncementDto
import com.wiredog.vpn.domain.model.AnnouncementSeverity
import com.wiredog.vpn.domain.model.toDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class AnnouncementMappingTest {

    private fun dto(severity: String, startAt: String = "2026-09-03T12:00:00.000Z") = AnnouncementDto(
        id = "a1",
        title = "t",
        body = "b",
        severity = severity,
        target = null,
        startAt = startAt,
        endAt = null
    )

    @Test
    fun `severity maps the three known values`() {
        assertEquals(AnnouncementSeverity.INFO, dto("info").toDomain().severity)
        assertEquals(AnnouncementSeverity.MAINTENANCE, dto("maintenance").toDomain().severity)
        assertEquals(AnnouncementSeverity.INCIDENT, dto("incident").toDomain().severity)
    }

    @Test
    fun `unknown severity falls back to INFO instead of throwing`() {
        assertEquals(AnnouncementSeverity.INFO, dto("something-new").toDomain().severity)
    }

    @Test
    fun `startAt parses ISO-8601, unparseable value becomes null`() {
        assertEquals(Instant.parse("2026-09-03T12:00:00.000Z"), dto("info").toDomain().startAt)
        assertNull(dto("info", startAt = "not a date").toDomain().startAt)
    }
}
