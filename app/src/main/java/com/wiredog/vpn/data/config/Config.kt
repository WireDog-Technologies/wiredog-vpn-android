package com.wiredog.vpn.data.config

import com.wiredog.vpn.BuildConfig

object Config {
    val apiBaseURL: String = BuildConfig.WIREDOG_API_BASE_URL
    val dashboardURL: String = BuildConfig.WIREDOG_URL_DASHBOARD
    val privacyPolicyURL: String = BuildConfig.WIREDOG_URL_PRIVACY
    val termsOfServiceURL: String = BuildConfig.WIREDOG_URL_TERMS
    val getStartedURL: String = BuildConfig.WIREDOG_URL_GET_STARTED
    val appStoreURL: String = BuildConfig.WIREDOG_APP_STORE_URL
    val pricingURL: String = "https://www.wiredogvpn.com/#pricing"
    val loginURL: String = "https://www.wiredogvpn.com/login"
    val supportURL: String = "https://www.wiredogvpn.com/help"
    const val supportEmail: String = "support@wiredogvpn.com"

    const val apiRequestTimeoutSeconds: Long = 30L
    const val vpnConnectionTimeoutSeconds: Long = 30L
    const val statisticsPollingIntervalMs: Long = 2000L
}
