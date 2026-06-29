package com.wiredog.vpn.domain.model

data class ConnectionStats(
    val downloadSpeed: Double = 0.0,  // Mbps
    val uploadSpeed: Double = 0.0,    // Mbps
    val connectionTime: Long = 0,     // seconds
    val dataTransferred: Double = 0.0 // GB
) {
    val formattedConnectionTime: String
        get() {
            val hours = connectionTime / 3600
            val minutes = (connectionTime % 3600) / 60
            val seconds = connectionTime % 60
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }

    val formattedDownloadSpeed: String
        get() = String.format("%.1f Mbps", downloadSpeed)

    val formattedUploadSpeed: String
        get() = String.format("%.1f Mbps", uploadSpeed)

    val formattedDataTransferred: String
        get() = if (dataTransferred < 1.0) {
            String.format("%.0f MB", dataTransferred * 1024)
        } else {
            String.format("%.2f GB", dataTransferred)
        }
}
