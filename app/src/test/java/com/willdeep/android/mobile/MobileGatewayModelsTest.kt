package com.willdeep.android.mobile

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileGatewayModelsTest {
    @Test
    fun pairingPayloadParsesQrContent() {
        val payload = PairingPayload.parse(
            """
            {
              "base_url": "http://192.168.1.20:8876/",
              "pairing_token": "pair_123",
              "protocol_version": "mobile-gateway.v1",
              "desktop_name": "Rocky's Mac",
              "expires_at": "2026-06-13T12:02:00Z"
            }
            """.trimIndent()
        )

        assertEquals("http://192.168.1.20:8876", payload.baseUrl)
        assertEquals("pair_123", payload.pairingToken)
        assertEquals("mobile-gateway.v1", payload.protocolVersion)
        assertEquals("Rocky's Mac", payload.desktopName)
    }

    @Test
    fun pairingClaimParsesApiEnvelope() {
        val claim = parsePairingClaim(
            """
            {
              "ok": true,
              "data": {
                "device": {
                  "id": "dev_123",
                  "name": "Pixel",
                  "created_at": "2026-06-13T12:00:00Z"
                },
                "device_token": "device_token",
                "protocol_version": "mobile-gateway.v1"
              }
            }
            """.trimIndent()
        )

        assertEquals("dev_123", claim.device.id)
        assertEquals("Pixel", claim.device.name)
        assertEquals("device_token", claim.deviceToken)
    }

    @Test
    fun stateSnapshotParsesSessions() {
        val event = parseGatewayEvent(
            """
            {
              "type": "state.snapshot",
              "payload": {
                "active_session_id": "s1",
                "sessions": [
                  {
                    "id": "s1",
                    "title": "Build mobile gateway",
                    "workspace_name": "Xedit",
                    "message_count": 3,
                    "is_active": true,
                    "is_responding": false
                  }
                ]
              }
            }
            """.trimIndent()
        )

        assertTrue(event is GatewayEvent.Snapshot)
        val snapshot = event as GatewayEvent.Snapshot
        assertEquals("s1", snapshot.activeSessionId)
        assertEquals("Build mobile gateway", snapshot.sessions.single().title)
        assertEquals(3, snapshot.sessions.single().messageCount)
    }

    @Test
    fun commandEnvelopeEncodesSessionAndPayload() {
        val envelope = GatewayEnvelope(
            id = "cmd_1",
            type = "message.send",
            sessionId = "s1",
            payload = JSONObject().put("text", "hello"),
        )

        val json = JSONObject(envelope.toJsonString())
        assertEquals("cmd_1", json.getString("id"))
        assertEquals("message.send", json.getString("type"))
        assertEquals("s1", json.getString("session_id"))
        assertEquals("hello", json.getJSONObject("payload").getString("text"))
    }
}
