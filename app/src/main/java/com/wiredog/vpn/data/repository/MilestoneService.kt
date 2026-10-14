package com.wiredog.vpn.data.repository

import android.content.Context
import com.wiredog.vpn.data.logging.LogLevel
import com.wiredog.vpn.data.logging.LogService
import com.wiredog.vpn.data.remote.api.WireDogApi
import com.wiredog.vpn.data.remote.api.dto.MilestoneRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pre-payment funnel milestones, each reported at most once per account. The backend turns them
 * into bare daily counters (no account id is stored) and ignores paid, organization and test
 * accounts, so this only answers "what share of new accounts got this far". Android checkout is
 * web-only, so only the web-checkout paywall actions exist here.
 */
enum class Milestone(val key: String, val isPaywallAction: Boolean) {
    CONNECT_ATTEMPTED("connect_attempted", false),

    /** Sent alongside the first of any paywall action below, so "saw the paywall and left"
     *  can be counted without per-account data (1 - engaged / connectAttempted). */
    PAYWALL_ENGAGED("paywall_engaged", false),
    PAYWALL_CHECKOUT_TAPPED("paywall_checkout_tapped", true),
    PAYWALL_BETTER_PLANS_TAPPED("paywall_better_plans_tapped", true)
}

@Singleton
class MilestoneService @Inject constructor(
    private val api: WireDogApi,
    private val authRepository: AuthRepository,
    private val logService: LogService,
    @ApplicationContext context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences("milestones", Context.MODE_PRIVATE)
    private val inFlight: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * Fire-and-forget. Marked sent only after the backend accepts it, so a failed send (offline)
     * is retried the next time the user repeats the action.
     */
    fun record(milestone: Milestone) {
        val user = authRepository.currentUser.value ?: return
        if (user.hasEntitlement || user.isOrganizationMember) return
        send(milestone, user.id)
        if (milestone.isPaywallAction) send(Milestone.PAYWALL_ENGAGED, user.id)
    }

    private fun send(milestone: Milestone, accountId: String) {
        val prefsKey = SENT_KEY_PREFIX + accountId
        if (prefs.getStringSet(prefsKey, emptySet())?.contains(milestone.key) == true) return
        val flightKey = "$accountId:${milestone.key}"
        if (!inFlight.add(flightKey)) return

        scope.launch {
            try {
                api.recordMilestone(MilestoneRequest(milestone.key))
                synchronized(prefs) {
                    val sent = prefs.getStringSet(prefsKey, emptySet()).orEmpty().toMutableSet()
                    sent.add(milestone.key)
                    prefs.edit().putStringSet(prefsKey, sent).apply()
                }
            } catch (e: Exception) {
                logService.logApp("[Milestone] ${milestone.key} not sent: ${e.message}", LogLevel.WARNING)
            } finally {
                inFlight.remove(flightKey)
            }
        }
    }

    private companion object {
        const val SENT_KEY_PREFIX = "sent_milestones_"
    }
}
