package com.wiredog.vpn.data.remote.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ConnectRequest(
    @Json(name = "serverId") val serverId: String
)

@JsonClass(generateAdapter = true)
data class ConnectResponse(
    @Json(name = "config") val config: WireGuardConfigDto,
    @Json(name = "sessionId") val sessionId: String,
    @Json(name = "server") val server: ConnectServerInfo? = null
)

@JsonClass(generateAdapter = true)
data class AwgParamsDto(
    @Json(name = "Jc") val jc: Int,
    @Json(name = "Jmin") val jmin: Int,
    @Json(name = "Jmax") val jmax: Int,
    @Json(name = "S1") val s1: Int,
    @Json(name = "S2") val s2: Int,
    @Json(name = "H1") val h1: Long,
    @Json(name = "H2") val h2: Long,
    @Json(name = "H3") val h3: Long,
    @Json(name = "H4") val h4: Long
)

@JsonClass(generateAdapter = true)
data class WireGuardConfigDto(
    @Json(name = "privateKey") val privateKey: String,
    @Json(name = "address") val address: String,
    @Json(name = "dns") val dns: String,
    @Json(name = "peer") val peer: PeerConfigDto,
    @Json(name = "awg") val awg: AwgParamsDto
)

@JsonClass(generateAdapter = true)
data class PeerConfigDto(
    @Json(name = "publicKey") val publicKey: String,
    @Json(name = "endpoint") val endpoint: String,
    @Json(name = "allowedIPs") val allowedIPs: String,
    @Json(name = "persistentKeepalive") val persistentKeepalive: Int = 15
)

@JsonClass(generateAdapter = true)
data class ConnectServerInfo(
    @Json(name = "id") val id: String,
    @Json(name = "state") val state: String? = null,
    @Json(name = "city") val city: String? = null
)

@JsonClass(generateAdapter = true)
data class DisconnectRequest(
    @Json(name = "sessionId") val sessionId: String
)

@JsonClass(generateAdapter = true)
data class DisconnectResponse(
    @Json(name = "message") val message: String
)
