package com.wiredog.vpn.data.remote.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wiredog.vpn.domain.model.Server

@JsonClass(generateAdapter = true)
data class ServerDto(
    @Json(name = "id") val id: String,
    @Json(name = "state") val state: String,
    @Json(name = "stateCode") val stateCode: String,
    @Json(name = "city") val city: String,
    @Json(name = "latitude") val latitude: Double?,
    @Json(name = "longitude") val longitude: Double?,
    @Json(name = "isRecommended") val isRecommended: Boolean = false,
    @Json(name = "latency") val latency: Int = 0,
    @Json(name = "load") val load: Int = 0,
    @Json(name = "host") val host: String = "",
    // Set only on an organization member's gateway entry (named Dedicated IP). `id` is still
    // the node's real server id; gatewayId is passed back when connecting.
    @Json(name = "gatewayId") val gatewayId: Int? = null,
    @Json(name = "gatewayName") val gatewayName: String? = null
) {
    fun toDomain(isFavorite: Boolean = false): Server {
        return Server(
            // A gateway shares its node's real id, so give it a unique list id.
            id = gatewayId?.let { "${this.id}_gw$it" } ?: id,
            state = state,
            stateCode = stateCode,
            city = city,
            latitude = latitude,
            longitude = longitude,
            isRecommended = isRecommended,
            latency = latency,
            load = load,
            isFavorite = isFavorite,
            host = host,
            gatewayId = gatewayId,
            gatewayName = gatewayName,
            apiServerId = if (gatewayId == null) null else id
        )
    }
}
