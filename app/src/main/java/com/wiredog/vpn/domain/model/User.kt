package com.wiredog.vpn.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class User(
    val id: String,
    val username: String,
    val accountNumber: String,
    val accountType: String,
    val subscriptionPlan: String,
    val subscriptionEndDate: LocalDate?
) {
    val isAnonymous: Boolean
        get() = accountType == "anonymous"

    val daysRemaining: Long
        get() = subscriptionEndDate?.let {
            ChronoUnit.DAYS.between(LocalDate.now(), it).coerceAtLeast(0)
        } ?: 0

    val isSubscriptionActive: Boolean
        get() = subscriptionEndDate?.isAfter(LocalDate.now()) ?: false
}
