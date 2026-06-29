package com.wiredog.vpn.data.remote

import com.wiredog.vpn.domain.model.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LatencyService @Inject constructor() {

    suspend fun measureAll(servers: List<Server>): Map<String, Int> = coroutineScope {
        val semaphore = Semaphore(20)
        servers
            .filter { it.host.isNotEmpty() }
            .map { server ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        server.id to measureLatency(server.host)
                    }
                }
            }
            .map { it.await() }
            .mapNotNull { (id, ms) -> ms?.let { id to it } }
            .toMap()
    }

    private fun measureLatency(host: String): Int? {
        val address = try {
            InetAddress.getByName(host)
        } catch (_: Exception) {
            return null
        }
        val samples = mutableListOf<Int>()
        repeat(3) { i ->
            try {
                val start = System.currentTimeMillis()
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(address, 443), 3000)
                    }
                } catch (_: ConnectException) {
                    // TCP RST — host reachable, elapsed = RTT
                }
                val elapsed = (System.currentTimeMillis() - start).toInt()
                if (elapsed < 2900) samples.add(elapsed)
            } catch (_: Exception) { }
            if (i < 2) Thread.sleep(200)
        }
        return samples.minOrNull()
    }
}
