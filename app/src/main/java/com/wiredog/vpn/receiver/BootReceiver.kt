package com.wiredog.vpn.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wiredog.vpn.BuildConfig
import com.wiredog.vpn.data.local.preferences.SettingsPreferences
import com.wiredog.vpn.data.repository.ServerRepository
import com.wiredog.vpn.service.vpn.VpnConnectionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsPreferences: SettingsPreferences
    @Inject lateinit var serverRepository: ServerRepository
    @Inject lateinit var vpnConnectionManager: VpnConnectionManager

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val autoConnect = settingsPreferences.autoConnectEnabled.first()
                if (!autoConnect) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Auto-connect disabled, skipping")
                    return@launch
                }

                // Fetch servers so we can look up the last connected one
                serverRepository.fetchServers()

                val lastServerId = serverRepository.lastConnectedServerId.first()
                if (lastServerId == null) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "No last connected server, skipping")
                    return@launch
                }

                val server = serverRepository.getServerById(lastServerId)
                if (server == null) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Last connected server not found, skipping")
                    return@launch
                }

                if (BuildConfig.DEBUG) Log.i(TAG, "Auto-connecting to ${server.displayName}")
                vpnConnectionManager.connect(server)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Auto-connect on boot failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
