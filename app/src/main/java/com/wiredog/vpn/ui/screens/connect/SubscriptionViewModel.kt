package com.wiredog.vpn.ui.screens.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiredog.vpn.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isResolvingCheckout = MutableStateFlow(false)
    val isResolvingCheckout: StateFlow<Boolean> = _isResolvingCheckout.asStateFlow()

    /**
     * Resolves the checkout URL (with a one-time handoff code so the logged-in user isn't
     * asked to sign in again) and hands it back to the caller to open. Falls back to the
     * public funnel URL on failure.
     */
    fun openCheckout(onUrl: (String) -> Unit) {
        if (_isResolvingCheckout.value) return
        _isResolvingCheckout.value = true
        viewModelScope.launch {
            val url = authRepository.checkoutUrl()
            _isResolvingCheckout.value = false
            onUrl(url)
        }
    }
}
