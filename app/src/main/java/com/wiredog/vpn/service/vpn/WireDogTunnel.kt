package com.wiredog.vpn.service.vpn

import org.amnezia.awg.backend.Tunnel

class WireDogTunnel : Tunnel {

    override fun getName(): String = "WireDog"

    override fun onStateChange(newState: Tunnel.State) {
        // State changes are tracked by VpnConnectionManager
    }
}
