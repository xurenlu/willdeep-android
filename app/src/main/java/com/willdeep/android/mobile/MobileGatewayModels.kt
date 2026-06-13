package com.willdeep.android.mobile

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

const val MOBILE_GATEWAY_PROTOCOL_VERSION = "mobile-gateway.v1"

data class PairingPayload(
    val baseUrl: String,
    val pairingToken: String,
    val protocolVersion: String,
    val desktopName: String,
    val expiresAt: String,
) {
    companion object {
        fun parse(raw: String): PairingPayload {
            val json = JSONObject(raw.trim())
            return PairingPayload(
                baseUrl = json.getString("base_url").trimEnd('/'),
                pairingToken = json.getString("pairing_token"),
                protocolVersion = json.optString("protocol_version", MOBILE_GATEWAY_PROTOCOL_VERSION),
                desktopName = json.optString("desktop_name", "WillDeep Mac"),
                expiresAt = json.optString("expires_at"),
            )
        }
    }
}

data class PairedDevice(
    val id: String,
    val name: String,
    val createdAt: String,
)

data class PairingClaim(
    val device: PairedDevice,
    val deviceToken: String,
    val protocolVersion: String,
)

data class GatewaySession(
    val id: String,
    val title: String,
    val workspaceName: String,
    val messageCount: Int,
    val isActive: Boolean,
    val isResponding: Boolean,
)

data class PendingToolApproval(
    val id: String,
    val title: String,
    val summary: String,
    val toolName: String,
    val inputPreview: String,
    val requiresAnswer: Boolean,
    val sessionId: String?,
)

data class PatchProposal(
    val id: String,
    val title: String,
    val summary: String,
    val path: String,
    val stats: String,
    val sessionId: String?,
)

data class PatchDiff(
    val patchId: String,
    val title: String,
    val diff: String,
    val sessionId: String?,
)

data class GatewayEnvelope(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val sessionId: String? = null,
    val payload: JSONObject = JSONObject(),
) {
    fun toJsonString(): String {
        val json = JSONObject()
            .put("id", id)
            .put("type", type)
            .put("payload", payload)
        if (!sessionId.isNullOrBlank()) {
            json.put("session_id", sessionId)
        }
        return json.toString()
    }
}

sealed interface GatewayEvent {
    data object Connected : GatewayEvent
    data object Disconnected : GatewayEvent

    data class Snapshot(
        val sessions: List<GatewaySession>,
        val activeSessionId: String?,
    ) : GatewayEvent

    data class SessionUpsert(val session: GatewaySession) : GatewayEvent
    data class Ack(val commandType: String, val sessionId: String?) : GatewayEvent
    data class Error(val message: String) : GatewayEvent
    data class MessageDelta(val sessionId: String?, val text: String) : GatewayEvent
    data class ToolPending(val approval: PendingToolApproval) : GatewayEvent
    data class PatchUpsert(val proposal: PatchProposal) : GatewayEvent
    data class PatchDiffLoaded(val diff: PatchDiff) : GatewayEvent
    data class Raw(val type: String) : GatewayEvent
}

fun JSONObject.asApiData(): JSONObject {
    if (!optBoolean("ok", false)) {
        throw IllegalStateException(optString("error", "Gateway request failed"))
    }
    return optJSONObject("data") ?: JSONObject()
}

fun parsePairingClaim(raw: String): PairingClaim {
    val data = JSONObject(raw).asApiData()
    val device = data.getJSONObject("device")
    return PairingClaim(
        device = PairedDevice(
            id = device.getString("id"),
            name = device.optString("name", "Android Device"),
            createdAt = device.optString("created_at"),
        ),
        deviceToken = data.getString("device_token"),
        protocolVersion = data.optString("protocol_version", MOBILE_GATEWAY_PROTOCOL_VERSION),
    )
}

fun parseGatewayEvent(raw: String): GatewayEvent {
    val json = JSONObject(raw)
    val type = json.optString("type")
    val payload = json.optJSONObject("payload") ?: JSONObject()
    val sessionId = json.optString("session_id").ifBlank { null }
    return when (type) {
        "state.snapshot" -> GatewayEvent.Snapshot(
            sessions = payload.optJSONArray("sessions").toSessions(),
            activeSessionId = payload.optString("active_session_id").ifBlank { null },
        )
        "session.upsert" -> GatewayEvent.SessionUpsert(payload.getJSONObject("session").toSession())
        "ack" -> payload.toAckEvent(sessionId)
        "error", "command.error" -> GatewayEvent.Error(payload.optString("message", "Unknown gateway error"))
        "message.delta" -> GatewayEvent.MessageDelta(
            sessionId = sessionId,
            text = payload.optString("delta"),
        )
        "tool.pending" -> GatewayEvent.ToolPending(payload.toPendingToolApproval(sessionId))
        "tool.updated" -> GatewayEvent.ToolPending(payload.toPendingToolApproval(sessionId))
        "patch.upsert" -> GatewayEvent.PatchUpsert(payload.toPatchProposal(sessionId))
        else -> GatewayEvent.Raw(type)
    }
}

private fun JSONObject.toAckEvent(sessionId: String?): GatewayEvent {
    val commandType = optString("type", "command")
    if (commandType == "diff.get" && optString("diff").isNotBlank()) {
        return GatewayEvent.PatchDiffLoaded(
            PatchDiff(
                patchId = firstString("patch_id", "id"),
                title = firstString("title"),
                diff = optString("diff"),
                sessionId = firstString("session_id").ifBlank { sessionId },
            )
        )
    }
    return GatewayEvent.Ack(
        commandType = commandType,
        sessionId = sessionId,
    )
}

private fun JSONArray?.toSessions(): List<GatewaySession> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            add(getJSONObject(index).toSession())
        }
    }
}

private fun JSONObject.toSession(): GatewaySession {
    return GatewaySession(
        id = getString("id"),
        title = optString("title", "Session"),
        workspaceName = optString("workspace_name", ""),
        messageCount = optInt("message_count", 0),
        isActive = optBoolean("is_active", false),
        isResponding = optBoolean("is_responding", false),
    )
}

private fun JSONObject.toPendingToolApproval(sessionId: String?): PendingToolApproval {
    val toolName = firstString("tool_name", "tool", "name").ifBlank { "Tool" }
    val requiresAnswer = optBoolean("requires_answer", false) ||
        optString("kind").equals("ask_user", ignoreCase = true) ||
        toolName.equals("ask_user", ignoreCase = true)
    return PendingToolApproval(
        id = firstString("id", "approval_id", "tool_call_id").ifBlank { toolName },
        title = firstString("title").ifBlank { toolName },
        summary = firstString("summary", "message", "reason"),
        toolName = toolName,
        inputPreview = firstString("input_preview", "arguments", "command").ifBlank {
            optJSONObject("input")?.toString()?.limitPreview().orEmpty()
        },
        requiresAnswer = requiresAnswer,
        sessionId = firstString("session_id").ifBlank { sessionId },
    )
}

private fun JSONObject.toPatchProposal(sessionId: String?): PatchProposal {
    val path = firstString("path", "file", "file_path")
    return PatchProposal(
        id = firstString("id", "patch_id").ifBlank { path.ifBlank { "patch" } },
        title = firstString("title").ifBlank { path.ifBlank { "Patch proposal" } },
        summary = firstString("summary", "description", "message"),
        path = path,
        stats = firstString("stats", "diffstat", "status"),
        sessionId = firstString("session_id").ifBlank { sessionId },
    )
}

private fun JSONObject.firstString(vararg keys: String): String {
    for (key in keys) {
        val value = opt(key) ?: continue
        val text = when (value) {
            is String -> value
            is JSONObject, is JSONArray -> value.toString()
            else -> value.toString()
        }.trim()
        if (text.isNotBlank()) {
            return text.limitPreview()
        }
    }
    return ""
}

private fun String.limitPreview(maxLength: Int = 300): String {
    return if (length <= maxLength) this else take(maxLength).trimEnd() + "..."
}
