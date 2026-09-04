package com.wiredog.vpn

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiredog.vpn.data.remote.api.WireDogApi
import com.wiredog.vpn.data.repository.AppConfigRepository
import com.wiredog.vpn.data.repository.AuthRepository
import com.wiredog.vpn.data.repository.UpdateAction
import com.wiredog.vpn.ui.screens.auth.hasAcceptedPrivacyDisclosure
import com.wiredog.vpn.ui.screens.auth.setPrivacyDisclosureAccepted
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
    val showPrivacyDisclosure: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val appConfigRepository: AppConfigRepository,
    @ApplicationContext private val context: Context,
    val wireDogApi: WireDogApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        val needsDisclosure = !hasAcceptedPrivacyDisclosure(context)
        if (needsDisclosure) {
            _uiState.value = _uiState.value.copy(showPrivacyDisclosure = true, isLoading = false)
        } else {
            // Seed the logged-in state from the stored auth token so a returning user lands
            // straight on the app. Without this the UI renders the login screen for the
            // moment the async session check takes, then snaps to the app (a visible blip).
            _uiState.value = _uiState.value.copy(isLoggedIn = authRepository.isLoggedIn.value)
            checkAppConfigThenSession()
        }
    }

    private fun checkAppConfigThenSession() {
        viewModelScope.launch {
            val action = appConfigRepository.fetchAndEvaluate(BuildConfig.VERSION_CODE)

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

            // Reveal the UI now. isLoggedIn is already seeded from the stored token, so a
            // returning user sees the app, not the login screen. checkSession() below only
            // flips to logged-out if the token turned out to be invalid (a 401 cleared it).
            _uiState.value = _uiState.value.copy(isLoading = false)

            val isLoggedIn = authRepository.checkSession()
            _uiState.value = _uiState.value.copy(isLoggedIn = isLoggedIn)
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

    fun acceptPrivacyDisclosure() {
        setPrivacyDisclosureAccepted(context)
        _uiState.value = _uiState.value.copy(
            showPrivacyDisclosure = false,
            isLoading = true,
            isLoggedIn = authRepository.isLoggedIn.value
        )
        checkAppConfigThenSession()
    }
}
