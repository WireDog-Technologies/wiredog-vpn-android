package com.wiredog.vpn.domain.model

import com.wiredog.vpn.data.remote.api.dto.LegalVersions

enum class LegalDocument { PRIVACY, TERMS }

/**
 * Decides which legal documents the user still needs to accept. Accepted versions are stored
 * locally only (SharedPreferences) — nothing about acceptance is sent to the server.
 */
object LegalAcceptance {
    const val PREFS_NAME = "wiredog_prefs"
    const val LEGACY_ACCEPTED_KEY = "hasAcceptedPrivacyDisclosure"
    const val PRIVACY_KEY = "acceptedPrivacyVersion"
    const val TERMS_KEY = "acceptedTermsVersion"

    /**
     * @param legacyAccepted the pre-versioning boolean. Users who accepted under it have no stored
     * versions; they're treated as current until [adoptLegacyAcceptance] records the server's
     * versions, so shipping this doesn't re-prompt anyone.
     */
    fun pending(
        legal: LegalVersions?,
        acceptedPrivacy: String,
        acceptedTerms: String,
        legacyAccepted: Boolean
    ): Set<LegalDocument> {
        if (legal == null) {
            // No config: never block returning users (fail open). A first-ever launch still
            // shows the initial screen.
            val hasAcceptedBefore = legacyAccepted || acceptedPrivacy.isNotEmpty() || acceptedTerms.isNotEmpty()
            return if (hasAcceptedBefore) emptySet() else setOf(LegalDocument.PRIVACY, LegalDocument.TERMS)
        }

        val result = mutableSetOf<LegalDocument>()
        if (acceptedPrivacy != legal.privacyVersion && !(legacyAccepted && acceptedPrivacy.isEmpty())) {
            result.add(LegalDocument.PRIVACY)
        }
        if (acceptedTerms != legal.termsVersion && !(legacyAccepted && acceptedTerms.isEmpty())) {
            result.add(LegalDocument.TERMS)
        }
        return result
    }

    /** True when the user has accepted something before, i.e. the prompt is a re-accept. */
    fun isUpdate(acceptedPrivacy: String, acceptedTerms: String, legacyAccepted: Boolean): Boolean =
        legacyAccepted || acceptedPrivacy.isNotEmpty() || acceptedTerms.isNotEmpty()

    /**
     * Records the server's current versions for documents a legacy-accepted user has no stored
     * version for. Returns the (possibly updated) pair.
     */
    fun adoptLegacyAcceptance(
        legal: LegalVersions,
        acceptedPrivacy: String,
        acceptedTerms: String,
        legacyAccepted: Boolean
    ): Pair<String, String> {
        if (!legacyAccepted) return acceptedPrivacy to acceptedTerms
        return (acceptedPrivacy.ifEmpty { legal.privacyVersion }) to (acceptedTerms.ifEmpty { legal.termsVersion })
    }
}
