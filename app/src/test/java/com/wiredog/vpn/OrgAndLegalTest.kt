package com.wiredog.vpn

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wiredog.vpn.data.remote.api.dto.LegalVersions
import com.wiredog.vpn.data.remote.api.dto.LoginResponse
import com.wiredog.vpn.data.remote.api.dto.ServerDto
import com.wiredog.vpn.data.remote.api.dto.UserProfileDto
import com.wiredog.vpn.data.repository.SsoManager
import com.wiredog.vpn.domain.model.LegalAcceptance
import com.wiredog.vpn.domain.model.LegalDocument
import com.wiredog.vpn.domain.model.OrganizationAccess
import com.wiredog.vpn.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OrgAndLegalTest {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private fun user(
        endDate: LocalDate? = null,
        isActive: Boolean = false,
        orgManaged: Boolean = false,
        access: OrganizationAccess? = null,
        orgName: String? = null
    ) = User(
        id = "1", username = "u", accountNumber = "", accountType = "standard",
        subscriptionPlan = "Free", subscriptionEndDate = endDate, isActive = isActive,
        subscriptionManagedByOrganization = orgManaged, organizationName = orgName,
        organizationAccess = access
    )

    // ---- Entitlement

    @Test
    fun personalAccountUsesExpiryDate() {
        assertTrue(user(endDate = LocalDate.now().plusDays(3)).hasEntitlement)
        assertFalse(user(endDate = LocalDate.now().minusDays(1)).hasEntitlement)
        assertFalse(user(endDate = null, isActive = true).hasEntitlement)
    }

    @Test
    fun orgSeatTrustsIsActiveEvenWithoutExpiry() {
        assertTrue(user(endDate = null, isActive = true, orgManaged = true).hasEntitlement)
        assertFalse(user(endDate = null, isActive = false, orgManaged = true).hasEntitlement)
    }

    @Test
    fun lostOrganizationAccessOnlyForInactiveOrRevoked() {
        assertNull(user(access = null).lostOrganizationAccess)
        assertNull(user(access = OrganizationAccess.ACTIVE).lostOrganizationAccess)
        assertEquals(OrganizationAccess.INACTIVE, user(access = OrganizationAccess.INACTIVE).lostOrganizationAccess)
        assertEquals(OrganizationAccess.REVOKED, user(access = OrganizationAccess.REVOKED).lostOrganizationAccess)
    }

    @Test
    fun inactiveOrgStillOwnsTheSeatButRevokedDoesNot() {
        assertTrue(user(access = OrganizationAccess.INACTIVE).isOrganizationMember)
        assertFalse(user(access = OrganizationAccess.REVOKED).isOrganizationMember)
        assertTrue(user(orgName = "Acme").isOrganizationMember)
    }

    @Test
    fun unknownOrganizationAccessFallsBackToActive() {
        assertNull(OrganizationAccess.fromApi(null))
        assertEquals(OrganizationAccess.ACTIVE, OrganizationAccess.fromApi("some_future_state"))
        assertEquals(OrganizationAccess.INACTIVE, OrganizationAccess.fromApi("inactive"))
        assertEquals(OrganizationAccess.REVOKED, OrganizationAccess.fromApi("revoked"))
    }

    // ---- Decoding

    @Test
    fun twoFactorChallengeDecodesWithoutToken() {
        val adapter = moshi.adapter(LoginResponse::class.java)
        val response = adapter.fromJson("""{"requiresTwoFactor":true,"challengeToken":"abc"}""")!!
        assertTrue(response.requiresTwoFactor)
        assertEquals("abc", response.challengeToken)
        assertNull(response.token)
    }

    @Test
    fun normalLoginStillDecodes() {
        val adapter = moshi.adapter(LoginResponse::class.java)
        val response = adapter.fromJson("""{"token":"t"}""")!!
        assertEquals("t", response.token)
        assertFalse(response.requiresTwoFactor)
    }

    @Test
    fun olderBackendProfileWithoutOrgFieldsStillDecodes() {
        val adapter = moshi.adapter(UserProfileDto::class.java)
        val profile = adapter.fromJson(
            """{"id":"7","username":null,"accountNumber":"1","accountType":"standard","planTier":null,"subscriptionExpiresAt":null}"""
        )!!
        assertFalse(profile.mustChangePassword)
        assertFalse(profile.twoFactorRequiredByOrg)
        assertNull(profile.organizationAccess)
    }

    // ---- Gateways

    @Test
    fun plainServerKeepsItsIdAndConnectsWithIt() {
        val server = ServerDto(
            id = "CA-SFO-001", state = "California", stateCode = "CA", city = "San Jose",
            latitude = null, longitude = null
        ).toDomain()
        assertEquals("CA-SFO-001", server.id)
        assertEquals("CA-SFO-001", server.connectServerId)
        assertFalse(server.isDedicated)
        assertNull(server.gatewayId)
    }

    @Test
    fun gatewayRowGetsCompositeIdButConnectsToTheRealNode() {
        val server = ServerDto(
            id = "CA-SFO-001", state = "California", stateCode = "CA", city = "San Jose",
            latitude = null, longitude = null, gatewayId = 12, gatewayName = "HQ Gateway"
        ).toDomain()
        assertEquals("CA-SFO-001_gw12", server.id)
        assertEquals("CA-SFO-001", server.connectServerId)
        assertEquals(12, server.gatewayId)
        assertTrue(server.isDedicated)
    }

    // ---- Legal acceptance

    private val legal = LegalVersions("p2", "t2", "summary")

    @Test
    fun firstLaunchWithNoConfigShowsBothDocuments() {
        assertEquals(
            setOf(LegalDocument.PRIVACY, LegalDocument.TERMS),
            LegalAcceptance.pending(null, "", "", legacyAccepted = false)
        )
    }

    @Test
    fun returningUserWithNoConfigIsNeverBlocked() {
        assertTrue(LegalAcceptance.pending(null, "", "", legacyAccepted = true).isEmpty())
        assertTrue(LegalAcceptance.pending(null, "p1", "", legacyAccepted = false).isEmpty())
    }

    @Test
    fun legacyAcceptedUserIsNotRePromptedUntilAVersionBump() {
        // Accepted under the old boolean, no stored versions yet.
        assertTrue(LegalAcceptance.pending(legal, "", "", legacyAccepted = true).isEmpty())
        // After adoption they hold the current versions...
        val (p, t) = LegalAcceptance.adoptLegacyAcceptance(legal, "", "", legacyAccepted = true)
        assertEquals("p2" to "t2", p to t)
        assertTrue(LegalAcceptance.pending(legal, p, t, legacyAccepted = true).isEmpty())
        // ...and a later bump re-prompts only for the document that changed.
        val bumped = LegalVersions("p3", "t2", null)
        assertEquals(setOf(LegalDocument.PRIVACY), LegalAcceptance.pending(bumped, p, t, legacyAccepted = true))
    }

    @Test
    fun adoptionDoesNothingForUsersWhoNeverAccepted() {
        assertEquals("" to "", LegalAcceptance.adoptLegacyAcceptance(legal, "", "", legacyAccepted = false))
    }

    @Test
    fun isUpdateOnlyWhenSomethingWasAcceptedBefore() {
        assertFalse(LegalAcceptance.isUpdate("", "", legacyAccepted = false))
        assertTrue(LegalAcceptance.isUpdate("", "", legacyAccepted = true))
        assertTrue(LegalAcceptance.isUpdate("p1", "", legacyAccepted = false))
    }

    // ---- SSO PKCE (RFC 7636 appendix B)

    @Test
    fun pkceChallengeMatchesRfcVector() {
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            SsoManager.codeChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")
        )
    }

    @Test
    fun codeVerifierIs43UrlSafeCharsAndRandom() {
        val a = SsoManager.makeCodeVerifier()
        val b = SsoManager.makeCodeVerifier()
        assertEquals(43, a.length)
        assertTrue(a.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        assertNotNull(b)
        assertFalse(a == b)
    }
}
