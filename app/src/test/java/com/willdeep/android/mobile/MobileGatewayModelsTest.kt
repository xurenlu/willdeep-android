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
                ],
                "jobs": [
                  {
                    "id": "job_uuid",
                    "handle": "job_abcdef",
                    "command": "yarn test",
                    "status": "running",
                    "is_alive": true,
                    "pid": 1234,
                    "output_byte_count": 42,
                    "session_id": "s1"
                  }
                ],
                "pending_tools": [
                  {
                    "approval_id": "tool_1",
                    "tool_name": "shell",
                    "summary": "Run Android tests",
                    "command": "./gradlew test",
                    "session_id": "s1"
                  }
                ],
                "patch_proposals": [
                  {
                    "patch_id": "patch_1",
                    "title": "Update mobile gateway client",
                    "summary": "Adds snapshot approvals",
                    "path": "app/src/main/java/com/willdeep/android/mobile/MobileGatewayModels.kt",
                    "diffstat": "+24 -0",
                    "session_id": "s1"
                  }
                ],
                "queued_messages": [
                  {
                    "id": "queue_1",
                    "text_preview": "Run Android tests next",
                    "image_count": 0,
                    "text_attachment_count": 1,
                    "session_id": "s1"
                  }
                ],
                "messages": [
                  {
                    "id": "m1",
                    "role": "user",
                    "content": "Please update the Android client",
                    "created_at": "2026-06-14T01:30:00Z",
                    "session_id": "s1"
                  },
                  {
                    "id": "m2",
                    "role": "assistant",
                    "content": "I'll make the change on the Mac.",
                    "created_at": "2026-06-14T01:30:02Z",
                    "session_id": "s1"
                  }
                ],
                "worktree_changes": [
                  {
                    "repository_root": "/tmp/project",
                    "file_count": 2,
                    "total_added_lines": 12,
                    "total_deleted_lines": 3,
                    "session_id": "s1",
                    "files": [
                      {
                        "path": "app/src/main/java/MainActivity.kt",
                        "kind": "M",
                        "added_lines": 10,
                        "deleted_lines": 2
                      },
                      {
                        "path": "README.md",
                        "kind": "A",
                        "added_lines": 2,
                        "deleted_lines": 1
                      }
                    ]
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
        assertEquals("tool_1", snapshot.pendingTools.single().id)
        assertEquals("Run Android tests", snapshot.pendingTools.single().summary)
        assertEquals("patch_1", snapshot.patchProposals.single().id)
        assertEquals("Update mobile gateway client", snapshot.patchProposals.single().title)
        assertEquals("job_abcdef", snapshot.jobs.single().handle)
        assertTrue(snapshot.jobs.single().isAlive)
        assertEquals("queue_1", snapshot.queuedMessages.single().id)
        assertEquals("Run Android tests next", snapshot.queuedMessages.single().textPreview)
        assertEquals(1, snapshot.queuedMessages.single().textAttachmentCount)
        assertEquals(2, snapshot.messages.size)
        assertEquals("user", snapshot.messages.first().role)
        assertEquals("Please update the Android client", snapshot.messages.first().content)
        assertEquals("assistant", snapshot.messages.last().role)
        assertEquals("/tmp/project", snapshot.worktrees.single().repositoryRoot)
        assertEquals(2, snapshot.worktrees.single().fileCount)
        assertEquals(12, snapshot.worktrees.single().totalAddedLines)
        assertEquals("app/src/main/java/MainActivity.kt", snapshot.worktrees.single().files.first().path)
        assertEquals("M", snapshot.worktrees.single().files.first().kind)
    }

    @Test
    fun worktreeUpdatedParsesChangedFiles() {
        val event = parseGatewayEvent(
            """
            {
              "type": "worktree.updated",
              "session_id": "s1",
              "payload": {
                "repository_root": "/tmp/project",
                "file_count": 1,
                "total_added_lines": 4,
                "total_deleted_lines": 0,
                "files": [
                  {
                    "path": "app/src/main/java/MainActivity.kt",
                    "kind": "M",
                    "added_lines": 4,
                    "deleted_lines": 0
                  }
                ]
              }
            }
            """.trimIndent()
        )

        assertTrue(event is GatewayEvent.WorktreeUpdated)
        val updated = event as GatewayEvent.WorktreeUpdated
        assertEquals("s1", updated.worktree.sessionId)
        assertEquals(1, updated.worktree.fileCount)
        assertEquals(4, updated.worktree.totalAddedLines)
        assertEquals("app/src/main/java/MainActivity.kt", updated.worktree.files.single().path)
    }

    @Test
    fun messageAppendParsesConversationMessage() {
        val event = parseGatewayEvent(
            """
            {
              "type": "message.append",
              "session_id": "s1",
              "payload": {
                "id": "m3",
                "role": "assistant",
                "content": "Patch is ready.",
                "created_at": "2026-06-14T01:31:00Z"
              }
            }
            """.trimIndent()
        )

        assertTrue(event is GatewayEvent.MessageAppend)
        val append = event as GatewayEvent.MessageAppend
        assertEquals("m3", append.message.id)
        assertEquals("assistant", append.message.role)
        assertEquals("Patch is ready.", append.message.content)
        assertEquals("s1", append.message.sessionId)
    }

    @Test
    fun messageDeltaParsesMessageIdAndText() {
        val event = parseGatewayEvent(
            """
            {
              "type": "message.delta",
              "session_id": "s1",
              "payload": {
                "message_id": "m3",
                "delta": "Streaming text"
              }
            }
            """.trimIndent()
        )

        assertTrue(event is GatewayEvent.MessageDelta)
        val delta = event as GatewayEvent.MessageDelta
        assertEquals("s1", delta.sessionId)
        assertEquals("m3", delta.messageId)
        assertEquals("Streaming text", delta.text)
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
    fun toolUpdatedWithPendingStatusKeepsApprovalPayload() {
        val event = parseGatewayEvent(
            """
            {
              "type": "tool.updated",
              "session_id": "s1",
              "payload": {
                "id": "tool_1",
                "status": "pending_approval",
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
        assertEquals("shell", pending.approval.toolName)
    }

    @Test
    fun toolUpdatedWithCompletedStatusRemovesApprovalPayload() {
        val event = parseGatewayEvent(
            """
            {
              "type": "tool.updated",
              "session_id": "s1",
              "payload": {
                "approval_id": "tool_1",
                "status": "approved",
                "tool_name": "shell"
              }
            }
            """.trimIndent()
        )

        assertTrue(event is GatewayEvent.ToolUpdated)
        val updated = event as GatewayEvent.ToolUpdated
        assertEquals("tool_1", updated.id)
        assertEquals("approved", updated.status)
        assertEquals("s1", updated.sessionId)
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
    fun diffGetAckParsesPatchDiffPayload() {
        val event = parseGatewayEvent(
            """
            {
              "id": "cmd_diff",
              "type": "ack",
              "session_id": "s1",
              "payload": {
                "type": "diff.get",
                "patch_id": "patch_1",
                "title": "Agent Patch",
                "diff": "diff --git a/App.kt b/App.kt\n+println(\"hi\")"
              }
            }
            """.trimIndent()
        )

        assertTrue(event is GatewayEvent.PatchDiffLoaded)
        val loaded = event as GatewayEvent.PatchDiffLoaded
        assertEquals("cmd_diff", loaded.commandId)
        assertEquals("patch_1", loaded.diff.patchId)
        assertEquals("Agent Patch", loaded.diff.title)
        assertEquals("diff --git a/App.kt b/App.kt\n+println(\"hi\")", loaded.diff.diff)
        assertEquals("s1", loaded.diff.sessionId)
    }

    @Test
    fun jobUpdatedParsesJobPayload() {
        val event = parseGatewayEvent(
            """
            {
              "type": "job.updated",
              "session_id": "s1",
              "payload": {
                "id": "job_uuid",
                "handle": "job_abcdef",
                "command": "go test ./...",
                "status": "killed",
                "is_alive": false,
                "pid": 1234,
                "exit_code": 143,
                "output_byte_count": 2048
              }
            }
            """.trimIndent()
        )

        assertTrue(event is GatewayEvent.JobUpdated)
        val updated = event as GatewayEvent.JobUpdated
        assertEquals("job_uuid", updated.job.id)
        assertEquals("job_abcdef", updated.job.handle)
        assertEquals("go test ./...", updated.job.command)
        assertEquals("killed", updated.job.status)
        assertEquals(false, updated.job.isAlive)
        assertEquals(143, updated.job.exitCode)
        assertEquals("s1", updated.job.sessionId)
    }

    @Test
    fun fileReadAckParsesFilePayload() {
        val event = parseGatewayEvent(
            """
            {
              "id": "cmd_file",
              "type": "ack",
              "session_id": "s1",
              "payload": {
                "type": "file.read",
                "path": "README.md",
                "content": "# WillDeep\n",
                "truncated": false,
                "byte_count": 11
              }
            }
            """.trimIndent()
        )

        assertTrue(event is GatewayEvent.FileLoaded)
        val loaded = event as GatewayEvent.FileLoaded
        assertEquals("cmd_file", loaded.commandId)
        assertEquals("README.md", loaded.file.path)
        assertEquals("# WillDeep\n", loaded.file.content)
        assertEquals(false, loaded.file.truncated)
        assertEquals(11, loaded.file.byteCount)
        assertEquals("s1", loaded.file.sessionId)
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

    @Test
    fun jobKillEnvelopeEncodesJobHandle() {
        val envelope = GatewayEnvelope(
            id = "cmd_3",
            type = "job.kill",
            sessionId = "s1",
            payload = JSONObject().put("job_id", "job_abcdef"),
        )

        val json = JSONObject(envelope.toJsonString())
        assertEquals("job.kill", json.getString("type"))
        assertEquals("s1", json.getString("session_id"))
        assertEquals("job_abcdef", json.getJSONObject("payload").getString("job_id"))
    }

    @Test
    fun fileReadEnvelopeEncodesPathAndLimit() {
        val envelope = GatewayEnvelope(
            id = "cmd_4",
            type = "file.read",
            sessionId = "s1",
            payload = JSONObject()
                .put("path", "README.md")
                .put("max_bytes", 65536),
        )

        val json = JSONObject(envelope.toJsonString())
        assertEquals("file.read", json.getString("type"))
        assertEquals("s1", json.getString("session_id"))
        assertEquals("README.md", json.getJSONObject("payload").getString("path"))
        assertEquals(65536, json.getJSONObject("payload").getInt("max_bytes"))
    }

    @Test
    fun queueUpdateEnvelopeEncodesActionAndMessageId() {
        val envelope = GatewayEnvelope(
            id = "cmd_5",
            type = "queue.update",
            sessionId = "s1",
            payload = JSONObject()
                .put("action", "send_now")
                .put("message_id", "queue_1"),
        )

        val json = JSONObject(envelope.toJsonString())
        assertEquals("queue.update", json.getString("type"))
        assertEquals("s1", json.getString("session_id"))
        assertEquals("send_now", json.getJSONObject("payload").getString("action"))
        assertEquals("queue_1", json.getJSONObject("payload").getString("message_id"))
    }
}
