package com.wiredog.vpn.service.vpn

import com.wiredog.vpn.data.remote.api.dto.WireGuardConfigDto
import com.wiredog.vpn.domain.model.SplitTunnelingSettings
import org.amnezia.awg.config.Config
import org.amnezia.awg.config.InetEndpoint
import org.amnezia.awg.config.InetNetwork
import org.amnezia.awg.config.Interface
import org.amnezia.awg.config.Peer
import org.amnezia.awg.crypto.Key
import java.math.BigInteger
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

object TunnelConfigBuilder {

    fun build(
        dto: WireGuardConfigDto,
        ipv6Enabled: Boolean = true,
        splitTunneling: SplitTunnelingSettings = SplitTunnelingSettings()
    ): Config {
        val interfaceBuilder = Interface.Builder()
            .parsePrivateKey(dto.privateKey)
            .setJunkPacketCount(dto.awg.jc)
            .setJunkPacketMinSize(dto.awg.jmin)
            .setJunkPacketMaxSize(dto.awg.jmax)
            .setInitPacketJunkSize(dto.awg.s1)
            .setResponsePacketJunkSize(dto.awg.s2)
            .setInitPacketMagicHeader(dto.awg.h1.toString())
            .setResponsePacketMagicHeader(dto.awg.h2.toString())
            .setUnderloadPacketMagicHeader(dto.awg.h3.toString())
            .setTransportPacketMagicHeader(dto.awg.h4.toString())

        // Parse address(es) - could be "10.0.0.2/32" or "10.0.0.2/32, fd00::2/128"
        dto.address.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { addr ->
            val network = InetNetwork.parse(addr)
            // Skip IPv6 addresses when IPv6 is disabled
            if (!ipv6Enabled && network.address is Inet6Address) return@forEach
            interfaceBuilder.addAddress(network)
        }

        // Parse DNS server(s) - could be "1.1.1.1" or "1.1.1.1,1.1.1.2"
        dto.dns.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { dns ->
            interfaceBuilder.addDnsServer(InetNetwork.parse(dns).address)
        }

        // Apply per-app split tunneling
        if (splitTunneling.enabled && splitTunneling.apps.isNotEmpty()) {
            when (splitTunneling.mode) {
                "exclude" -> splitTunneling.apps.forEach { pkg ->
                    interfaceBuilder.excludeApplication(pkg)
                }
                "include" -> splitTunneling.apps.forEach { pkg ->
                    interfaceBuilder.includeApplication(pkg)
                }
            }
        }

        val peerBuilder = Peer.Builder()
            .parsePublicKey(dto.peer.publicKey)
            .parseEndpoint(dto.peer.endpoint)
            .parsePersistentKeepalive(dto.peer.persistentKeepalive.toString())

        // Build allowedIPs based on split tunneling mode
        buildAllowedIps(dto, splitTunneling, peerBuilder)

        return Config.Builder()
            .setInterface(interfaceBuilder.build())
            .addPeer(peerBuilder.build())
            .build()
    }

    private fun buildAllowedIps(
        dto: WireGuardConfigDto,
        splitTunneling: SplitTunnelingSettings,
        peerBuilder: Peer.Builder
    ) {
        if (!splitTunneling.enabled || splitTunneling.ips.isEmpty()) {
            // Default: route all traffic through tunnel
            dto.peer.allowedIPs.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { ip ->
                peerBuilder.addAllowedIp(InetNetwork.parse(ip))
            }
            return
        }

        when (splitTunneling.mode) {
            "include" -> {
                // Include mode: only route specified IPs + DNS through the tunnel
                val allIncludedIps = mutableSetOf<String>()

                splitTunneling.ips.forEach { ip -> allIncludedIps.add(ip.trim()) }

                // Add DNS server IPs so name resolution works
                dto.dns.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { dns ->
                    try {
                        val dnsAddress = InetNetwork.parse(dns).address
                        val prefix = if (dnsAddress is Inet6Address) "/128" else "/32"
                        val dnsIp = dnsAddress.hostAddress
                        if (dnsIp != null) allIncludedIps.add("$dnsIp$prefix")
                    } catch (_: Exception) { }
                }

                allIncludedIps.forEach { ip ->
                    try {
                        peerBuilder.addAllowedIp(InetNetwork.parse(ip))
                    } catch (_: Exception) { }
                }
            }
            "exclude" -> {
                // Exclude mode: route everything EXCEPT specified IPs
                // Parse the default allowedIPs (typically 0.0.0.0/0, ::/0)
                val defaultRanges = dto.peer.allowedIPs.split(",")
                    .map { it.trim() }.filter { it.isNotEmpty() }

                // Parse excluded IPs
                val excludedNetworks = splitTunneling.ips.mapNotNull { ip ->
                    try {
                        parseNetwork(ip.trim())
                    } catch (_: Exception) { null }
                }

                // Separate IPv4 and IPv6 exclusions
                val ipv4Exclusions = excludedNetworks.filter { it.first is Inet4Address }
                val ipv6Exclusions = excludedNetworks.filter { it.first is Inet6Address }

                for (range in defaultRanges) {
                    try {
                        val network = parseNetwork(range)
                        val isIpv4 = network.first is Inet4Address
                        val relevantExclusions = if (isIpv4) ipv4Exclusions else ipv6Exclusions

                        if (relevantExclusions.isEmpty()) {
                            // No exclusions for this address family, keep the full range
                            peerBuilder.addAllowedIp(InetNetwork.parse(range))
                        } else {
                            // Compute complement routes
                            val complementRoutes = computeComplement(network, relevantExclusions)
                            complementRoutes.forEach { route ->
                                peerBuilder.addAllowedIp(InetNetwork.parse(route))
                            }
                        }
                    } catch (_: Exception) { }
                }
            }
        }
    }

    // ─── Complement CIDR Calculation ─────────────────────────────────────────

    private fun parseNetwork(cidr: String): Pair<InetAddress, Int> {
        val parts = cidr.split("/")
        val addr = InetAddress.getByName(parts[0])
        val prefix = if (parts.size > 1) parts[1].toInt()
            else if (addr is Inet6Address) 128 else 32
        return Pair(addr, prefix)
    }

    private fun addressToBigInteger(addr: InetAddress): BigInteger {
        return BigInteger(1, addr.address)
    }

    private fun bigIntegerToAddress(value: BigInteger, isIpv6: Boolean): String {
        val bytes = value.toByteArray()
        val targetSize = if (isIpv6) 16 else 4

        val padded = when {
            bytes.size == targetSize -> bytes
            bytes.size > targetSize -> bytes.takeLast(targetSize).toByteArray()
            else -> ByteArray(targetSize - bytes.size) + bytes
        }

        val addr = InetAddress.getByAddress(padded)
        return addr.hostAddress ?: value.toString()
    }

    /**
     * Computes CIDR ranges that cover [baseNetwork] minus all [exclusions].
     * Uses recursive binary splitting: at each level, split the range in half;
     * if a half doesn't overlap any exclusion, keep it whole; if it does, recurse.
     */
    private fun computeComplement(
        baseNetwork: Pair<InetAddress, Int>,
        exclusions: List<Pair<InetAddress, Int>>
    ): List<String> {
        val isIpv6 = baseNetwork.first is Inet6Address
        val maxBits = if (isIpv6) 128 else 32

        val result = mutableListOf<String>()

        fun recurse(rangeAddr: BigInteger, rangePrefix: Int) {
            // Find exclusions that overlap this range
            val rangeSize = BigInteger.ONE.shiftLeft(maxBits - rangePrefix)
            val rangeEnd = rangeAddr + rangeSize - BigInteger.ONE

            val overlapping = exclusions.filter { (exAddr, exPrefix) ->
                val exStart = addressToBigInteger(exAddr)
                // Mask to network address
                val exMask = if (exPrefix == 0) BigInteger.ZERO
                    else BigInteger.ONE.shiftLeft(maxBits - exPrefix) - BigInteger.ONE
                val exNetStart = exStart.and(exMask.not().and(
                    BigInteger.ONE.shiftLeft(maxBits) - BigInteger.ONE
                ))
                val exSize = BigInteger.ONE.shiftLeft(maxBits - exPrefix)
                val exEnd = exNetStart + exSize - BigInteger.ONE

                // Check overlap: ranges overlap if start1 <= end2 && start2 <= end1
                rangeAddr <= exEnd && exNetStart <= rangeEnd
            }

            if (overlapping.isEmpty()) {
                // No exclusions overlap — keep this entire range
                result.add("${bigIntegerToAddress(rangeAddr, isIpv6)}/$rangePrefix")
                return
            }

            if (rangePrefix >= maxBits) {
                // Can't split further — this single address is excluded, drop it
                return
            }

            // Split in half and recurse
            val childPrefix = rangePrefix + 1
            val halfSize = BigInteger.ONE.shiftLeft(maxBits - childPrefix)
            val leftAddr = rangeAddr
            val rightAddr = rangeAddr + halfSize

            recurse(leftAddr, childPrefix)
            recurse(rightAddr, childPrefix)
        }

        recurse(addressToBigInteger(baseNetwork.first), baseNetwork.second)
        return result
    }
}
