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
            ConnectionStatus.AwaitingDesktop,
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

    @Test
    fun gatewayHealthTargetKeepsFallbackBaseUrls() {
        val target = resolveGatewayHealthTarget(
            MobileGatewayUiState(
                baseUrl = "http://192.168.1.20:8877",
                fallbackBaseUrls = listOf("http://100.90.80.70:8877"),
                desktopName = "Mac",
                protocolVersion = "mobile-gateway.v1",
            )
        )

        assertTrue(target != null)
        assertTrue(target?.fallbackBaseUrls == listOf("http://100.90.80.70:8877"))
    }
}
