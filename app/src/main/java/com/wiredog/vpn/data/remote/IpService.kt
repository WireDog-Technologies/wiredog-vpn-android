package com.wiredog.vpn.data.remote

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.wiredog.vpn.data.logging.LogLevel
import com.wiredog.vpn.data.logging.LogService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.Inet4Address
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IpService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logService: LogService
) {
    private val client = OkHttpClient.Builder()
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                return Dns.SYSTEM.lookup(hostname).filterIsInstance<Inet4Address>()
            }
        })
        .connectTimeout(4, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(4, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val clientUnrestricted = OkHttpClient.Builder()
        .connectTimeout(4, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(4, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "ip_cache",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    val cachedPublicIp: String?
        get() = prefs.getString("public_ip", null)

    val cachedPublicIpv6: String?
        get() = prefs.getString("public_ipv6", null)

    val cachedLocation: String?
        get() = prefs.getString("location", null)

    /**
     * Drops any pooled/keep-alive connections. Should be called after the VPN tunnel disconnects
     * — otherwise a request can keep reusing a socket opened over the pre-disconnect network
     * interface and silently keep returning the stale (VPN) IP instead of the current one.
     * Evicting the pool closes live sockets, which is itself blocking I/O — must run off the
     * main thread or it throws NetworkOnMainThreadException.
     */
    suspend fun resetConnections() = withContext(Dispatchers.IO) {
        client.connectionPool.evictAll()
        clientUnrestricted.connectionPool.evictAll()
    }

    suspend fun getPublicIp(): Result<String> = withContext(Dispatchers.IO) {
        try {
            logService.logService("Fetching public IP address")
            val request = Request.Builder()
                .url("https://api.ipify.org")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()?.trim()
            if (response.isSuccessful && !body.isNullOrEmpty()) {
                prefs.edit().putString("public_ip", body).apply()
                logService.logService("Public IP fetched successfully")
                Result.success(body)
            } else {
                logService.logService("Failed to fetch public IP: HTTP ${response.code}", LogLevel.WARNING)
                Result.failure(Exception("Failed to fetch IP: ${response.code}"))
            }
        } catch (e: Exception) {
            logService.logService("Public IP fetch error: ${e.message}", LogLevel.WARNING)
            Result.failure(e)
        }
    }

    suspend fun getPublicIpv6(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api6.ipify.org")
                .get()
                .build()
            val response = clientUnrestricted.newCall(request).execute()
            val body = response.body?.string()?.trim()
            if (response.isSuccessful && !body.isNullOrEmpty()) {
                prefs.edit().putString("public_ipv6", body).apply()
                Result.success(body)
            } else {
                Result.failure(Exception("Failed to fetch IPv6: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGeoLocation(ip: String? = null): String? = withContext(Dispatchers.IO) {
        try {
            logService.logService("Fetching geo location")
            val url = if (ip != null) "https://ipwho.is/$ip" else "https://ipwho.is/"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            val response = clientUnrestricted.newCall(request).execute()
            val body = response.body?.string()
            logService.logService("Geo location response received (${response.code})")
            if (body == null) return@withContext null
            val json = JSONObject(body)
            if (!json.optBoolean("success", false)) return@withContext null
            val city = json.optString("city").takeIf { it.isNotEmpty() } ?: return@withContext null
            val regionCode = json.optString("region_code").takeIf { it.isNotEmpty() }
            val countryCode = json.optString("country_code").takeIf { it.isNotEmpty() }
            val locationStr = if (countryCode == "US" && regionCode != null) {
                "$city, $regionCode"
            } else if (countryCode != null) {
                "$city, $countryCode"
            } else {
                city
            }
            prefs.edit().putString("location", locationStr).apply()
            logService.logService("Geo location resolved: $locationStr")
            locationStr
        } catch (e: Exception) {
            logService.logService("Geo location fetch error: ${e.message}", LogLevel.WARNING)
            null
        }
    }

    suspend fun resolveEndpointIp(endpoint: String): String? = withContext(Dispatchers.IO) {
        try {
            val hostname = endpoint.substringBefore(":")
            InetAddress.getByName(hostname).hostAddress
        } catch (_: Exception) {
            null
        }
    }

    fun abbreviateState(state: String): String = US_STATE_ABBREVIATIONS[state] ?: state

    companion object {
        private val US_STATE_ABBREVIATIONS = mapOf(
            "Alabama" to "AL", "Alaska" to "AK", "Arizona" to "AZ", "Arkansas" to "AR",
            "California" to "CA", "Colorado" to "CO", "Connecticut" to "CT", "Delaware" to "DE",
            "Florida" to "FL", "Georgia" to "GA", "Hawaii" to "HI", "Idaho" to "ID",
            "Illinois" to "IL", "Indiana" to "IN", "Iowa" to "IA", "Kansas" to "KS",
            "Kentucky" to "KY", "Louisiana" to "LA", "Maine" to "ME", "Maryland" to "MD",
            "Massachusetts" to "MA", "Michigan" to "MI", "Minnesota" to "MN", "Mississippi" to "MS",
            "Missouri" to "MO", "Montana" to "MT", "Nebraska" to "NE", "Nevada" to "NV",
            "New Hampshire" to "NH", "New Jersey" to "NJ", "New Mexico" to "NM", "New York" to "NY",
            "North Carolina" to "NC", "North Dakota" to "ND", "Ohio" to "OH", "Oklahoma" to "OK",
            "Oregon" to "OR", "Pennsylvania" to "PA", "Rhode Island" to "RI", "South Carolina" to "SC",
            "South Dakota" to "SD", "Tennessee" to "TN", "Texas" to "TX", "Utah" to "UT",
            "Vermont" to "VT", "Virginia" to "VA", "Washington" to "WA", "West Virginia" to "WV",
            "Wisconsin" to "WI", "Wyoming" to "WY", "District of Columbia" to "DC"
        )
    }
}
