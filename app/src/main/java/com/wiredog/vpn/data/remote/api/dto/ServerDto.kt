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
    @Json(name = "host") val host: String = ""
) {
    fun toDomain(isFavorite: Boolean = false): Server {
        return Server(
            id = id,
            state = state,
            stateCode = stateCode,
            city = city,
            latitude = latitude,
            longitude = longitude,
            isRecommended = isRecommended,
            latency = latency,
            load = load,
            isFavorite = isFavorite,
            host = host
        )
    }
}
