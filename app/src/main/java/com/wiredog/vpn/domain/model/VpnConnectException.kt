package com.wiredog.vpn.domain.model

/**
 * Thrown by [com.wiredog.vpn.data.repository.VpnRepository.connect] when the backend
 * rejects a connect attempt with a user-facing reason (e.g. device connection limit).
 * The message is safe to show directly in the UI.
 */
class VpnConnectException(message: String) : Exception(message)
