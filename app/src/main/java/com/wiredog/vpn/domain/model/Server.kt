package com.wiredog.vpn.domain.model

data class Server(
    val id: String,
    val state: String,
    val stateCode: String,
    val city: String,
    val latitude: Double?,
    val longitude: Double?,
    val isRecommended: Boolean = false,
    val latency: Int = 0,
    val load: Int = 0,
    val isFavorite: Boolean = false,
    val host: String = ""
) {
    val displayName: String
        get() = "$state - $city"
}

data class ServerGroup(
    val state: String,
    val stateCode: String,
    val servers: List<Server>
)
