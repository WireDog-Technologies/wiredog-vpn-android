package com.wiredog.vpn.domain.model

data class VpnSettings(
    val protocol: String = "AmneziaWG",
    val isAutoConnectEnabled: Boolean = false,
    val isIPv6Enabled: Boolean = false
)
