package com.wiredog.vpn.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Where an account stands with an organization (auth/me `organizationAccess`). Unknown values
 * fall back to [ACTIVE] so a future backend state never locks someone out of the app.
 */
enum class OrganizationAccess {
    ACTIVE,
    // The org's own subscription lapsed or was canceled; the member's seat is intact.
    INACTIVE,
    // An admin removed this person from the org.
    REVOKED;

    val userMessage: String
        get() = when (this) {
            ACTIVE -> ""
            INACTIVE -> "Your organization's WireDog subscription isn't active right now. Contact your administrator."
            REVOKED -> "Your organization has removed your access to WireDog VPN. Contact your administrator if you think this is a mistake."
        }

    companion object {
        fun fromApi(raw: String?): OrganizationAccess? = when (raw?.lowercase()) {
            null -> null
            "inactive" -> INACTIVE
            "revoked" -> REVOKED
            else -> ACTIVE
        }
    }
}

data class User(
    val id: String,
    val username: String,
    val accountNumber: String,
    val accountType: String,
    val subscriptionPlan: String,
    val subscriptionEndDate: LocalDate?,
    // /auth/me's isActive — for org seats this reflects the organization's live subscription.
    val isActive: Boolean = false,
    val totpEnabled: Boolean = false,
    val twoFactorRequiredByOrg: Boolean = false,
    val mustChangePassword: Boolean = false,
    val subscriptionManagedByOrganization: Boolean = false,
    val organizationName: String? = null,
    val organizationRole: String? = null,
    val organizationAccess: OrganizationAccess? = null
) {
    val isAnonymous: Boolean
        get() = accountType == "anonymous"

    val daysRemaining: Long
        get() = subscriptionEndDate?.let {
            ChronoUnit.DAYS.between(LocalDate.now(), it).coerceAtLeast(0)
        } ?: 0

    val isSubscriptionActive: Boolean
        get() = subscriptionEndDate?.isAfter(LocalDate.now()) ?: false

    /**
     * Whether the backend will let this account connect. An org seat's expiry can be absent, so
     * for those trust [isActive]. Personal accounts keep the expiry check.
     */
    val hasEntitlement: Boolean
        get() = if (subscriptionManagedByOrganization) isActive else isSubscriptionActive

    /** An 'inactive' org still owns the seat; 'revoked' does not (a plain consumer again). */
    val isOrganizationMember: Boolean
        get() = organizationName != null || subscriptionManagedByOrganization ||
            organizationAccess == OrganizationAccess.INACTIVE

    /** Set only when an organization used to cover this account and no longer does. */
    val lostOrganizationAccess: OrganizationAccess?
        get() = organizationAccess?.takeIf { it != OrganizationAccess.ACTIVE }
}
