package com.willdeep.android.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GatewayHealthTargetTest {
    @Test
    fun pairingPayloadTakesPriorityWhenPresent() {
        val target = resolveGatewayHealthTarget(
            MobileGatewayUiState(
                pairingPayloadText = """
                    {
                      "base_url": "http://192.168.1.20:8876/",
                      "pairing_token": "pair_123",
                      "protocol_version": "mobile-gateway.v1",
                      "desktop_name": "Pairing Mac",
                      "expires_at": "2026-06-14T12:02:00Z"
                    }
                """.trimIndent(),
                baseUrl = "http://old-mac:8876",
                desktopName = "Old Mac",
                protocolVersion = "old",
            )
        )

        requireNotNull(target)
        assertEquals("http://192.168.1.20:8876", target.baseUrl)
        assertEquals("Pairing Mac", target.desktopName)
        assertEquals("mobile-gateway.v1", target.protocolVersion)
        assertEquals(true, target.requiresPairingAllowed)
    }

    @Test
    fun pairedGatewayIsUsedWhenPayloadIsEmpty() {
        val target = resolveGatewayHealthTarget(
            MobileGatewayUiState(
                baseUrl = "http://192.168.1.30:8876",
                desktopName = "Saved Mac",
                protocolVersion = "mobile-gateway.v1",
                isPaired = true,
            )
        )

        requireNotNull(target)
        assertEquals("http://192.168.1.30:8876", target.baseUrl)
        assertEquals("Saved Mac", target.desktopName)
        assertEquals("mobile-gateway.v1", target.protocolVersion)
        assertEquals(false, target.requiresPairingAllowed)
    }

    @Test
    fun missingPayloadAndBaseUrlHasNoTarget() {
        assertNull(resolveGatewayHealthTarget(MobileGatewayUiState()))
    }
}
