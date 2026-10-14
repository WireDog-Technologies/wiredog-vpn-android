package com.wiredog.vpn.data.config

import com.wiredog.vpn.BuildConfig

object Config {
    val apiBaseURL: String = BuildConfig.WIREDOG_API_BASE_URL
    val dashboardURL: String = BuildConfig.WIREDOG_URL_DASHBOARD

    // Where the one-time handoff code is redeemed (org-required 2FA setup, Manage Account). Same
    // environment rule as checkoutURL.
    val dashboardHandoffURL: String = BuildConfig.WIREDOG_URL_DASHBOARD_HANDOFF
    val privacyPolicyURL: String = BuildConfig.WIREDOG_URL_PRIVACY
    val termsOfServiceURL: String = BuildConfig.WIREDOG_URL_TERMS
    val getStartedURL: String = BuildConfig.WIREDOG_URL_GET_STARTED

    // Must point at the same environment WIREDOG_API_BASE_URL does — the checkout page
    // exchanges a handoff code against whichever API/DB minted it.
    val checkoutURL: String = BuildConfig.WIREDOG_URL_CHECKOUT
    val appStoreURL: String = BuildConfig.WIREDOG_APP_STORE_URL
    val pricingURL: String = BuildConfig.WIREDOG_URL_PRICING
    val loginURL: String = BuildConfig.WIREDOG_URL_LOGIN
    val supportURL: String = BuildConfig.WIREDOG_URL_SUPPORT
    const val supportEmail: String = "support@wiredogvpn.com"

    const val apiRequestTimeoutSeconds: Long = 30L
    const val vpnConnectionTimeoutSeconds: Long = 30L
    const val statisticsPollingIntervalMs: Long = 2000L
}
