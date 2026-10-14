package com.wiredog.vpn

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiredog.vpn.data.remote.api.WireDogApi
import com.wiredog.vpn.data.repository.AppConfigRepository
import com.wiredog.vpn.data.repository.AuthRepository
import com.wiredog.vpn.data.repository.UpdateAction
import com.wiredog.vpn.domain.model.LegalAcceptance
import com.wiredog.vpn.domain.model.LegalDocument
import com.wiredog.vpn.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val isLoading: Boolean = true,
    val isLoggedIn: Boolean = false,
    val updateAction: UpdateAction = UpdateAction.None,
    val softUpdateDismissed: Boolean = false,
    // Legal documents the user still has to accept (empty = none). isLegalUpdate switches the
    // screen from first-launch copy to "we have updated..." copy.
    val pendingLegal: Set<LegalDocument> = emptySet(),
    val isLegalUpdate: Boolean = false,
    val legalSummary: String? = null
) {
    val showPrivacyDisclosure: Boolean
        get() = pendingLegal.isNotEmpty()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val appConfigRepository: AppConfigRepository,
    @ApplicationContext private val context: Context,
    val wireDogApi: WireDogApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Business accounts can be gated before the app opens (temporary password, org-required 2FA).
    val currentUser: StateFlow<User?> = authRepository.currentUser

    private val legalPrefs = context.getSharedPreferences(LegalAcceptance.PREFS_NAME, Context.MODE_PRIVATE)
    private var lastConfigFetchMs = 0L

    init {
        recomputeLegal()
        if (_uiState.value.showPrivacyDisclosure) {
            _uiState.value = _uiState.value.copy(isLoading = false)
        } else {
            startApp()
        }
    }

    private fun startApp() {
        // Seed the logged-in state from the stored auth token so a returning user lands
        // straight on the app. Without this the UI renders the login screen for the
        // moment the async session check takes, then snaps to the app (a visible blip).
        _uiState.value = _uiState.value.copy(isLoggedIn = authRepository.isLoggedIn.value)
        checkAppConfigThenSession()
    }

    private fun checkAppConfigThenSession() {
        viewModelScope.launch {
            val action = appConfigRepository.fetchAndEvaluate(BuildConfig.VERSION_CODE)
            lastConfigFetchMs = System.currentTimeMillis()

            when (action) {
                is UpdateAction.ForceUpdate, is UpdateAction.Maintenance -> {
                    _uiState.value = MainUiState(
                        isLoading = false,
                        updateAction = action
                    )
                    return@launch
                }
                else -> {
                    _uiState.value = _uiState.value.copy(updateAction = action)
                }
            }

            // The config carries the published legal versions: this may now require a re-accept.
            recomputeLegal()

            // Reveal the UI now. isLoggedIn is already seeded from the stored token, so a
            // returning user sees the app, not the login screen. checkSession() below only
            // flips to logged-out if the token turned out to be invalid (a 401 cleared it).
            _uiState.value = _uiState.value.copy(isLoading = false)

            val isLoggedIn = authRepository.checkSession()
            _uiState.value = _uiState.value.copy(isLoggedIn = isLoggedIn)
        }
    }

    // --- Legal acceptance ---

    private fun acceptedPrivacy() = legalPrefs.getString(LegalAcceptance.PRIVACY_KEY, "") ?: ""
    private fun acceptedTerms() = legalPrefs.getString(LegalAcceptance.TERMS_KEY, "") ?: ""
    private fun legacyAccepted() = legalPrefs.getBoolean(LegalAcceptance.LEGACY_ACCEPTED_KEY, false)

    private fun recomputeLegal() {
        val legal = appConfigRepository.legal.value
        // Users who accepted under the old boolean have no stored versions: record the current
        // ones so shipping versioning does not re-prompt anyone.
        if (legal != null) {
            val (privacy, terms) = LegalAcceptance.adoptLegacyAcceptance(legal, acceptedPrivacy(), acceptedTerms(), legacyAccepted())
            legalPrefs.edit()
                .putString(LegalAcceptance.PRIVACY_KEY, privacy)
                .putString(LegalAcceptance.TERMS_KEY, terms)
                .apply()
        }
        _uiState.value = _uiState.value.copy(
            pendingLegal = LegalAcceptance.pending(legal, acceptedPrivacy(), acceptedTerms(), legacyAccepted()),
            isLegalUpdate = LegalAcceptance.isUpdate(acceptedPrivacy(), acceptedTerms(), legacyAccepted()),
            legalSummary = legal?.summary
        )
    }

    /** Picks up a legal-version bump without waiting for a cold launch. */
    fun onForeground() {
        if (System.currentTimeMillis() - lastConfigFetchMs < FOREGROUND_REFRESH_MIN_INTERVAL_MS) return
        viewModelScope.launch {
            appConfigRepository.refreshLegal()
            lastConfigFetchMs = System.currentTimeMillis()
            if (!_uiState.value.isLoading) recomputeLegal()
        }
    }

    fun acceptPrivacyDisclosure() {
        val state = _uiState.value
        // Only record versions for documents actually shown; with no config (first launch
        // offline) the legacy flag alone marks acceptance and versions are adopted later.
        val edit = legalPrefs.edit()
        appConfigRepository.legal.value?.let { legal ->
            if (LegalDocument.PRIVACY in state.pendingLegal) edit.putString(LegalAcceptance.PRIVACY_KEY, legal.privacyVersion)
            if (LegalDocument.TERMS in state.pendingLegal) edit.putString(LegalAcceptance.TERMS_KEY, legal.termsVersion)
        }
        edit.putBoolean(LegalAcceptance.LEGACY_ACCEPTED_KEY, true).apply()

        if (state.isLegalUpdate) {
            // The app is already running behind this screen: just dismiss it.
            recomputeLegal()
        } else {
            _uiState.value = state.copy(
                pendingLegal = emptySet(),
                isLoading = true,
                isLoggedIn = authRepository.isLoggedIn.value
            )
            checkAppConfigThenSession()
        }
    }

    fun dismissSoftUpdate() {
        _uiState.value = _uiState.value.copy(softUpdateDismissed = true)
    }

    fun onLoginSuccess() {
        _uiState.value = _uiState.value.copy(isLoggedIn = true)
    }

    fun onLogout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = _uiState.value.copy(isLoggedIn = false)
        }
    }

    private companion object {
        const val FOREGROUND_REFRESH_MIN_INTERVAL_MS = 30_000L
    }
}
