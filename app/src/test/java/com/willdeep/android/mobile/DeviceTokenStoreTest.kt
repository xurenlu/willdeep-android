package com.willdeep.android.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DeviceTokenStoreTest {
    @Test
    fun storedCredentialsRoundTripMultipleMacs() {
        val credentials = listOf(
            credential(
                baseUrl = "https://relay.example.com",
                relayRoom = "macbook-room",
                desktopName = "Rocky 的 MacBook Pro",
                lastResponseAt = 1234L,
            ),
            credential(
                baseUrl = "http://192.168.1.12:8787",
                relayRoom = null,
                desktopName = "工作室 Mac mini",
                lastResponseAt = 5678L,
            ),
        )

        assertEquals(credentials, decodeStoredGatewayCredentials(encodeStoredGatewayCredentials(credentials)))
    }

    @Test
    fun credentialIdentityUsesRelayRoomWhenAvailable() {
        val first = credential(
            baseUrl = "https://first-relay.example.com",
            relayRoom = "stable-room",
            desktopName = "Mac",
        )
        val moved = first.copy(baseUrl = "https://second-relay.example.com")
        val other = first.copy(relayRoom = "other-room")

        assertEquals(first.id, moved.id)
        assertNotEquals(first.id, other.id)
    }

    private fun credential(
        baseUrl: String,
        relayRoom: String?,
        desktopName: String,
        lastResponseAt: Long = 0L,
    ): StoredGatewayCredential {
        return StoredGatewayCredential(
            baseUrl = baseUrl,
            fallbackBaseUrls = listOf("http://10.0.0.2:8787"),
            deviceToken = "test-token-$desktopName",
            desktopName = desktopName,
            protocolVersion = MOBILE_GATEWAY_PROTOCOL_VERSION,
            relayRoom = relayRoom,
            lastDesktopResponseAtEpochMillis = lastResponseAt,
        )
    }
}
