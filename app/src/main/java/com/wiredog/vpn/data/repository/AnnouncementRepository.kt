package com.wiredog.vpn.data.repository

import com.wiredog.vpn.data.local.preferences.AnnouncementPreferences
import com.wiredog.vpn.data.logging.LogLevel
import com.wiredog.vpn.data.logging.LogService
import com.wiredog.vpn.data.remote.api.WireDogApi
import com.wiredog.vpn.domain.model.Announcement
import com.wiredog.vpn.domain.model.toDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * In-app announcements (maintenance / info / incident notices). Android analog of the iOS
 * `BroadcastService`: polls `GET /app/announcements` on a jittered interval while the app is
 * foregrounded, keeps a local read/unread set, and prunes read-ids the backend no longer
 * returns so the set can't grow forever.
 */
@Singleton
class AnnouncementRepository @Inject constructor(
    private val api: WireDogApi,
    private val prefs: AnnouncementPreferences,
    private val logService: LogService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private companion object {
        const val POLL_INTERVAL_MS = 5 * 60_000L
        const val POLL_JITTER_MS = 60_000L
    }

    private val _messages = MutableStateFlow<List<Announcement>>(emptyList())
    /** Active announcements, newest first (matches iOS `activeMessages()`). */
    val messages: StateFlow<List<Announcement>> = _messages.asStateFlow()

    private val _readIds = MutableStateFlow<Set<String>>(emptySet())
    val readIds: StateFlow<Set<String>> = _readIds.asStateFlow()

    val unreadCount: StateFlow<Int> =
        combine(_messages, _readIds) { msgs, read -> msgs.count { it.id !in read } }
            .stateIn(scope, SharingStarted.Eagerly, 0)

    private var pollingJob: Job? = null

    init {
        scope.launch { prefs.readIds.collect { _readIds.value = it } }
    }

    /** Call when the app enters the foreground: refresh now and (re)start polling. */
    fun onAppForeground() {
        scope.launch { refresh() }
        startPolling()
    }

    /** Call when the app leaves the foreground: stop polling. */
    fun onAppBackground() {
        pollingJob?.cancel()
        pollingJob = null
    }

    suspend fun refresh() {
        try {
            val fetched = api.getAnnouncements()
                .map { it.toDomain() }
                .sortedByDescending { it.startAt ?: Instant.EPOCH }
            _messages.value = fetched
            pruneReadIds(fetched.map { it.id }.toSet())
        } catch (e: Exception) {
            logService.logService("Announcements fetch failed: ${e.message}", LogLevel.WARNING)
        }
    }

    fun markRead(ids: List<String>) {
        val next = _readIds.value + ids
        if (next == _readIds.value) return
        _readIds.value = next
        scope.launch { prefs.setReadIds(next) }
    }

    fun markUnread(id: String) {
        if (id !in _readIds.value) return
        val next = _readIds.value - id
        _readIds.value = next
        scope.launch { prefs.setReadIds(next) }
    }

    /** Drop read-ids for announcements the backend no longer returns. */
    private fun pruneReadIds(present: Set<String>) {
        val pruned = _readIds.value intersect present
        if (pruned == _readIds.value) return
        _readIds.value = pruned
        scope.launch { prefs.setReadIds(pruned) }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS + Random.nextLong(0, POLL_JITTER_MS))
                refresh()
            }
        }
    }
}
