package com.wiredog.vpn.data.repository

import com.squareup.moshi.Moshi
import com.wiredog.vpn.data.local.preferences.AppConfigPreferences
import com.wiredog.vpn.data.logging.LogLevel
import com.wiredog.vpn.data.logging.LogService
import com.wiredog.vpn.data.remote.api.WireDogApi
import com.wiredog.vpn.data.remote.api.dto.AppConfigResponse
import javax.inject.Inject
import javax.inject.Singleton

sealed class UpdateAction {
    data object None : UpdateAction()
    data object SoftUpdate : UpdateAction()
    data object ForceUpdate : UpdateAction()
    data object Maintenance : UpdateAction()
}

@Singleton
class AppConfigRepository @Inject constructor(
    private val api: WireDogApi,
    private val appConfigPreferences: AppConfigPreferences,
    private val moshi: Moshi,
    private val logService: LogService
) {
    private val adapter = moshi.adapter(AppConfigResponse::class.java)

    suspend fun fetchAndEvaluate(currentVersionCode: Int): UpdateAction {
        val config = fetchConfig()
        return if (config != null) {
            evaluateConfig(config, currentVersionCode)
        } else {
            UpdateAction.None // fail-open when no config available
        }
    }

    private suspend fun fetchConfig(): AppConfigResponse? {
        // Try network first
        try {
            logService.logService("Fetching app configuration")
            val config = api.getAppConfig()
            // Cache on success
            val json = adapter.toJson(config)
            appConfigPreferences.saveConfig(json)
            logService.logService("App configuration updated")
            return config
        } catch (e: Exception) {
            // Network failed, try cache
            logService.logService("App config fetch failed: ${e.message}", LogLevel.WARNING)
        }

        // Fall back to cache if fresh enough
        if (appConfigPreferences.isCacheFresh()) {
            val json = appConfigPreferences.getCachedConfigJson()
            if (json != null) {
                return try {
                    logService.logService("Using fresh cached app configuration")
                    adapter.fromJson(json)
                } catch (_: Exception) {
                    null
                }
            }
        }

        // Stale/no cache + network failure: check if we have any cached config
        // that shows this version is known-bad (fail-closed for known-bad versions)
        val json = appConfigPreferences.getCachedConfigJson()
        if (json != null) {
            return try {
                logService.logService("Using stale cached app configuration")
                adapter.fromJson(json)
            } catch (_: Exception) {
                null
            }
        }

        logService.logService("No app configuration available", LogLevel.WARNING)
        return null
    }

    suspend fun getCachedConfig(): AppConfigResponse? {
        val json = appConfigPreferences.getCachedConfigJson() ?: return null
        return try {
            adapter.fromJson(json)
        } catch (_: Exception) {
            null
        }
    }

    private fun evaluateConfig(config: AppConfigResponse, currentVersionCode: Int): UpdateAction {
        val android = config.platforms.android

        if (config.maintenanceMode) {
            return UpdateAction.Maintenance
        }

        if (android.forceUpdate || currentVersionCode < android.minSupportedVersion) {
            return UpdateAction.ForceUpdate
        }

        if (currentVersionCode < android.latestVersion) {
            return UpdateAction.SoftUpdate
        }

        return UpdateAction.None
    }
}
