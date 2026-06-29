package com.wiredog.vpn.domain.model

data class SplitTunnelingSettings(
    val enabled: Boolean = false,
    val mode: String = "include",
    val apps: Set<String> = emptySet(),
    val ips: Set<String> = emptySet()
)
