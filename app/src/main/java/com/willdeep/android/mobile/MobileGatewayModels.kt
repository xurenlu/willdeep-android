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
        "ack" -> GatewayEvent.Ack(
            commandType = payload.optString("type", "command"),
            sessionId = sessionId,
        )
        "error", "command.error" -> GatewayEvent.Error(payload.optString("message", "Unknown gateway error"))
        "message.delta" -> GatewayEvent.MessageDelta(
            sessionId = sessionId,
            text = payload.optString("delta"),
        )
        else -> GatewayEvent.Raw(type)
    }
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
