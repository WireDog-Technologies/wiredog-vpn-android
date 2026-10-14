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
    val host: String = "",
    // Organization gateway entry (named Dedicated IP), null for a normal server. For a gateway,
    // `id` is the composite "<serverId>_gw<gatewayId>" and apiServerId is the node's real id.
    val gatewayId: Int? = null,
    val gatewayName: String? = null,
    val apiServerId: String? = null
) {
    val displayName: String
        get() = gatewayName ?: "$state - $city"

    /** What vpn/connect takes as `serverId`. */
    val connectServerId: String
        get() = apiServerId ?: id

    val isDedicated: Boolean
        get() = gatewayId != null
}

data class ServerGroup(
    val state: String,
    val stateCode: String,
    val servers: List<Server>
)
