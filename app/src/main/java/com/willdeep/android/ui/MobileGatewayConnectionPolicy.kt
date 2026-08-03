package com.willdeep.android.ui

import com.willdeep.android.mobile.ReconnectPolicy

object MobileGatewayConnectionPolicy {
    fun shouldAutoResume(
        isPaired: Boolean,
        status: ConnectionStatus,
        manuallyDisconnected: Boolean,
    ): Boolean {
        if (!isPaired || manuallyDisconnected) {
            return false
        }
        return when (status) {
            ConnectionStatus.Idle,
            ConnectionStatus.Disconnected,
            ConnectionStatus.Error -> true
            ConnectionStatus.Pairing,
            ConnectionStatus.Connecting,
            ConnectionStatus.AwaitingDesktop,
            ConnectionStatus.Reconnecting,
            ConnectionStatus.Connected -> false
        }
    }

    /// The relay accepts an old, well-formed token as a transport connection
    /// but isolates it from the Mac's current token. That looks exactly like
    /// "server connected, Mac silent", so surface re-pairing as the recovery
    /// action once the truthful desktop-response timeout has elapsed.
    fun shouldSuggestRePair(
        isTransportConnected: Boolean,
        desktopResponseAgeMillis: Long,
    ): Boolean {
        return isTransportConnected &&
            desktopResponseAgeMillis >= ReconnectPolicy.HEARTBEAT_TIMEOUT_MILLIS
    }
}
