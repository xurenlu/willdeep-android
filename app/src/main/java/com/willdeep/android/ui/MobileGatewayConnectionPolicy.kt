package com.willdeep.android.ui

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
            ConnectionStatus.Reconnecting,
            ConnectionStatus.Connected -> false
        }
    }
}
