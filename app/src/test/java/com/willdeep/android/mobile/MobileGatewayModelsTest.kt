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

    @Test
    fun toolPendingParsesApprovalPayload() {
        val event = parseGatewayEvent(
            """
            {
              "type": "tool.pending",
              "session_id": "s1",
              "payload": {
                "approval_id": "tool_1",
                "tool_name": "shell",
                "summary": "Run tests",
                "command": "go test ./..."
              }
            }
            """.trimIndent()
        )

        assertTrue(event is GatewayEvent.ToolPending)
        val pending = event as GatewayEvent.ToolPending
        assertEquals("tool_1", pending.approval.id)
        assertEquals("shell", pending.approval.title)
        assertEquals("Run tests", pending.approval.summary)
        assertEquals("go test ./...", pending.approval.inputPreview)
        assertEquals(false, pending.approval.requiresAnswer)
        assertEquals("s1", pending.approval.sessionId)
    }

    @Test
    fun askUserToolPendingRequiresAnswer() {
        val event = parseGatewayEvent(
            """
            {
              "type": "tool.pending",
              "session_id": "s1",
              "payload": {
                "approval_id": "ask_1",
                "tool_name": "ask_user",
                "summary": "Waiting for your answer",
                "input": {
                  "question": "Which branch should I use?"
                }
              }
            }
            """.trimIndent()
        )

        assertTrue(event is GatewayEvent.ToolPending)
        val pending = event as GatewayEvent.ToolPending
        assertEquals("ask_1", pending.approval.id)
        assertEquals("ask_user", pending.approval.toolName)
        assertTrue(pending.approval.requiresAnswer)
    }

    @Test
    fun patchUpsertParsesProposalPayload() {
        val event = parseGatewayEvent(
            """
            {
              "type": "patch.upsert",
              "payload": {
                "patch_id": "patch_1",
                "title": "Update Android gateway client",
                "path": "app/src/main/java/MainActivity.kt",
                "summary": "Adds approval controls",
                "diffstat": "+12 -2",
                "session_id": "s2"
              }
            }
            """.trimIndent()
        )

        assertTrue(event is GatewayEvent.PatchUpsert)
        val proposal = event as GatewayEvent.PatchUpsert
        assertEquals("patch_1", proposal.proposal.id)
        assertEquals("Update Android gateway client", proposal.proposal.title)
        assertEquals("app/src/main/java/MainActivity.kt", proposal.proposal.path)
        assertEquals("+12 -2", proposal.proposal.stats)
        assertEquals("s2", proposal.proposal.sessionId)
    }

    @Test
    fun decisionEnvelopeEncodesApprovalDecision() {
        val envelope = GatewayEnvelope(
            id = "cmd_2",
            type = "tool.decide",
            sessionId = "s1",
            payload = JSONObject()
                .put("id", "tool_1")
                .put("decision", "approve")
                .put("approved", true)
                .put("answer", "use main"),
        )

        val json = JSONObject(envelope.toJsonString())
        assertEquals("tool.decide", json.getString("type"))
        assertEquals("s1", json.getString("session_id"))
        assertEquals("tool_1", json.getJSONObject("payload").getString("id"))
        assertTrue(json.getJSONObject("payload").getBoolean("approved"))
        assertEquals("use main", json.getJSONObject("payload").getString("answer"))
    }
}
