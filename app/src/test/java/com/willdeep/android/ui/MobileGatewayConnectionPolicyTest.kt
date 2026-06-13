package com.willdeep.android.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileGatewayConnectionPolicyTest {
    @Test
    fun pairedDisconnectedDeviceCanAutoResume() {
        assertTrue(
            MobileGatewayConnectionPolicy.shouldAutoResume(
                isPaired = true,
                status = ConnectionStatus.Disconnected,
                manuallyDisconnected = false,
            )
        )
    }

    @Test
    fun manualDisconnectPreventsAutoResume() {
        assertFalse(
            MobileGatewayConnectionPolicy.shouldAutoResume(
                isPaired = true,
                status = ConnectionStatus.Disconnected,
                manuallyDisconnected = true,
            )
        )
    }

    @Test
    fun activeConnectionStatesDoNotStartAnotherConnection() {
        listOf(
            ConnectionStatus.Pairing,
            ConnectionStatus.Connecting,
            ConnectionStatus.Reconnecting,
            ConnectionStatus.Connected,
        ).forEach { status ->
            assertFalse(
                MobileGatewayConnectionPolicy.shouldAutoResume(
                    isPaired = true,
                    status = status,
                    manuallyDisconnected = false,
                )
            )
        }
    }

    @Test
    fun unpairedDeviceDoesNotAutoResume() {
        assertFalse(
            MobileGatewayConnectionPolicy.shouldAutoResume(
                isPaired = false,
                status = ConnectionStatus.Disconnected,
                manuallyDisconnected = false,
            )
        )
    }
}
