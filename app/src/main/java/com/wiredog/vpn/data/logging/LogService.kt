package com.wiredog.vpn.data.logging

import android.content.Context
import com.wiredog.vpn.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

enum class LogLevel { DEBUG, INFO, WARNING, ERROR }

@Singleton
class LogService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val appLogFile by lazy { File(context.filesDir, "app_logs.txt") }
    private val serviceLogFile by lazy { File(context.filesDir, "service_logs.txt") }
    private val tunnelLogFile by lazy { File(context.filesDir, "tunnel_logs.txt") }

    companion object {
        private const val MAX_FILE_BYTES = 1_048_576L // 1 MB
        private const val MAX_LINES = 1000
    }

    fun logApp(message: String, level: LogLevel = LogLevel.INFO) {
        write(appLogFile, message, level, "APP")
    }

    fun logService(message: String, level: LogLevel = LogLevel.INFO) {
        write(serviceLogFile, message, level, "SERVICE")
    }

    fun logTunnel(message: String, level: LogLevel = LogLevel.INFO) {
        write(tunnelLogFile, message, level, "TUNNEL")
    }

    private fun write(file: File, message: String, level: LogLevel, source: String) {
        if (level == LogLevel.DEBUG && !BuildConfig.DEBUG) return

        scope.launch {
            val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            val line = "[$timestamp] [${level.name}] [$source] $message\n"
            synchronized(file.path.intern()) {
                rotateIfNeeded(file)
                file.appendText(line)
            }
        }
    }

    private fun rotateIfNeeded(file: File) {
        if (!file.exists()) return
        val needsRotation = file.length() > MAX_FILE_BYTES || file.readLines().size > MAX_LINES
        if (needsRotation) {
            val lines = file.readLines()
            val trimmed = lines.takeLast(MAX_LINES / 2)
            file.writeText(trimmed.joinToString("\n") + "\n")
        }
    }
}
