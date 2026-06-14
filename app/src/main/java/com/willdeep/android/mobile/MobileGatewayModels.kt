package com.willdeep.android.mobile

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

const val MOBILE_GATEWAY_PROTOCOL_VERSION = "mobile-gateway.v1"

class InvalidPairingPayloadException(cause: Throwable? = null) : IllegalArgumentException(
    "Invalid pairing payload",
    cause,
)

data class PairingPayload(
    val baseUrl: String,
    val pairingToken: String,
    val protocolVersion: String,
    val desktopName: String,
    val expiresAt: String,
) {
    fun isExpired(now: Instant = Instant.now()): Boolean {
        val expiry = runCatching {
            expiresAt.takeIf { it.isNotBlank() }?.let(Instant::parse)
        }.getOrNull() ?: return false
        return !expiry.isAfter(now)
    }

    fun hasCompatibleProtocol(): Boolean {
        return protocolVersion == MOBILE_GATEWAY_PROTOCOL_VERSION
    }

    companion object {
        fun parse(raw: String): PairingPayload {
            try {
                val json = JSONObject(raw.trim())
                val baseUrl = json.optString("base_url").trim().trimEnd('/')
                val pairingToken = json.optString("pairing_token").trim()
                if (baseUrl.isBlank() || baseUrl.toHttpUrlOrNull() == null || pairingToken.isBlank()) {
                    throw InvalidPairingPayloadException()
                }
                return PairingPayload(
                    baseUrl = baseUrl,
                    pairingToken = pairingToken,
                    protocolVersion = json.optString("protocol_version", MOBILE_GATEWAY_PROTOCOL_VERSION),
                    desktopName = json.optString("desktop_name", "WillDeep Mac"),
                    expiresAt = json.optString("expires_at"),
                )
            } catch (error: InvalidPairingPayloadException) {
                throw error
            } catch (error: RuntimeException) {
                throw InvalidPairingPayloadException(error)
            }
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

data class GatewayHealth(
    val status: String,
    val appVersion: String,
    val serverVersion: String,
    val protocolVersion: String,
    val pairingAllowed: Boolean,
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

data class GatewayJob(
    val id: String,
    val handle: String,
    val command: String,
    val status: String,
    val isAlive: Boolean,
    val pid: Int,
    val exitCode: Int?,
    val outputByteCount: Int,
    val sessionId: String?,
)

data class GatewayFile(
    val path: String,
    val content: String,
    val truncated: Boolean,
    val byteCount: Int,
    val sessionId: String?,
)

data class GatewayQueuedMessage(
    val id: String,
    val textPreview: String,
    val imageCount: Int,
    val textAttachmentCount: Int,
    val sessionId: String?,
)

data class GatewayMessage(
    val id: String,
    val role: String,
    val content: String,
    val createdAt: String,
    val sessionId: String?,
    val isStreaming: Boolean = false,
)

data class GatewayWorktreeFile(
    val path: String,
    val kind: String,
    val addedLines: Int,
    val deletedLines: Int,
)

data class GatewayWorktree(
    val repositoryRoot: String?,
    val fileCount: Int,
    val totalAddedLines: Int,
    val totalDeletedLines: Int,
    val files: List<GatewayWorktreeFile>,
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
    data class ConnectionClosed(val code: Int, val reason: String) : GatewayEvent
    data class ConnectionFailed(val message: String, val httpCode: Int?) : GatewayEvent

    data class Snapshot(
        val sessions: List<GatewaySession>,
        val activeSessionId: String?,
        val pendingTools: List<PendingToolApproval>,
        val patchProposals: List<PatchProposal>,
        val jobs: List<GatewayJob>,
        val queuedMessages: List<GatewayQueuedMessage>,
        val messages: List<GatewayMessage>,
        val worktrees: List<GatewayWorktree>,
    ) : GatewayEvent

    data class SessionUpsert(val session: GatewaySession) : GatewayEvent
    data class Ack(val commandId: String?, val commandType: String, val sessionId: String?) : GatewayEvent
    data class Error(val commandId: String?, val message: String) : GatewayEvent
    data class MessageAppend(val message: GatewayMessage) : GatewayEvent
    data class MessageDelta(val sessionId: String?, val messageId: String?, val text: String) : GatewayEvent
    data class MessageDone(val sessionId: String?, val messageId: String?) : GatewayEvent
    data class WorktreeUpdated(val worktree: GatewayWorktree) : GatewayEvent
    data class ToolPending(val approval: PendingToolApproval) : GatewayEvent
    data class ToolUpdated(val id: String, val status: String, val sessionId: String?) : GatewayEvent
    data class PatchUpsert(val proposal: PatchProposal) : GatewayEvent
    data class PatchUpdated(val id: String, val status: String, val sessionId: String?) : GatewayEvent
    data class PatchDiffLoaded(val commandId: String?, val diff: PatchDiff) : GatewayEvent
    data class JobUpdated(val job: GatewayJob) : GatewayEvent
    data class FileLoaded(val commandId: String?, val file: GatewayFile) : GatewayEvent
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
    val id = json.optString("id").ifBlank { null }
    val type = json.optString("type")
    val payload = json.optJSONObject("payload") ?: JSONObject()
    val sessionId = json.optString("session_id").ifBlank { null }
    return when (type) {
        "state.snapshot" -> GatewayEvent.Snapshot(
            sessions = payload.optJSONArray("sessions").toSessions(),
            activeSessionId = payload.optString("active_session_id").ifBlank { null },
            pendingTools = payload.optJSONArray("pending_tools").toPendingToolApprovals(),
            patchProposals = payload.optJSONArray("patch_proposals").toPatchProposals(),
            jobs = payload.optJSONArray("jobs").toJobs(),
            queuedMessages = payload.optJSONArray("queued_messages").toQueuedMessages(),
            messages = payload.optJSONArray("messages").toMessages(),
            worktrees = payload.optJSONArray("worktree_changes").toWorktrees(),
        )
        "session.upsert" -> GatewayEvent.SessionUpsert(payload.getJSONObject("session").toSession())
        "ack" -> payload.toAckEvent(id, sessionId)
        "error", "command.error" -> GatewayEvent.Error(id, payload.optString("message", "Unknown gateway error"))
        "message.append" -> GatewayEvent.MessageAppend(payload.toMessage(sessionId))
        "message.delta" -> GatewayEvent.MessageDelta(
            sessionId = sessionId,
            messageId = payload.firstString("message_id", "id").ifBlank { null },
            text = payload.firstString("delta", "text", "content"),
        )
        "message.done" -> GatewayEvent.MessageDone(
            sessionId = sessionId,
            messageId = payload.firstString("message_id", "id").ifBlank { null },
        )
        "worktree.updated" -> GatewayEvent.WorktreeUpdated(payload.toWorktree(sessionId))
        "tool.pending" -> GatewayEvent.ToolPending(payload.toPendingToolApproval(sessionId))
        "tool.updated" -> payload.toToolUpdatedEvent(sessionId)
        "patch.upsert" -> payload.toPatchEvent(sessionId)
        "job.updated" -> GatewayEvent.JobUpdated(payload.toJob(sessionId))
        else -> GatewayEvent.Raw(type)
    }
}

private fun JSONObject.toAckEvent(commandId: String?, sessionId: String?): GatewayEvent {
    val commandType = optString("type", "command")
    if (commandType == "diff.get" && optString("diff").isNotBlank()) {
        return GatewayEvent.PatchDiffLoaded(
            commandId = commandId,
            diff = PatchDiff(
                patchId = firstString("patch_id", "id"),
                title = firstString("title"),
                diff = optString("diff"),
                sessionId = firstString("session_id").ifBlank { sessionId },
            )
        )
    }
    if (commandType == "file.read" && has("content")) {
        return GatewayEvent.FileLoaded(
            commandId = commandId,
            file = GatewayFile(
                path = firstString("path", "file_path"),
                content = optString("content"),
                truncated = optBoolean("truncated", false),
                byteCount = optInt("byte_count", 0),
                sessionId = firstString("session_id").ifBlank { sessionId },
            )
        )
    }
    return GatewayEvent.Ack(
        commandId = commandId,
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

private fun JSONArray?.toPendingToolApprovals(): List<PendingToolApproval> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            add(getJSONObject(index).toPendingToolApproval(null))
        }
    }
}

private fun JSONArray?.toPatchProposals(): List<PatchProposal> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            add(getJSONObject(index).toPatchProposal(null))
        }
    }
}

private fun JSONArray?.toJobs(): List<GatewayJob> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            add(getJSONObject(index).toJob(null))
        }
    }
}

private fun JSONArray?.toQueuedMessages(): List<GatewayQueuedMessage> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            add(getJSONObject(index).toQueuedMessage(null))
        }
    }
}

private fun JSONArray?.toMessages(): List<GatewayMessage> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            add(getJSONObject(index).toMessage(null))
        }
    }
}

private fun JSONArray?.toWorktrees(): List<GatewayWorktree> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            add(getJSONObject(index).toWorktree(null))
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

private fun JSONObject.toToolUpdatedEvent(sessionId: String?): GatewayEvent {
    val status = firstString("status", "state", "decision").lowercase()
    if (status in PENDING_TOOL_STATUSES) {
        return GatewayEvent.ToolPending(toPendingToolApproval(sessionId))
    }
    return GatewayEvent.ToolUpdated(
        id = firstString("id", "approval_id", "tool_call_id").ifBlank { firstString("tool_name", "tool", "name") },
        status = status,
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

private fun JSONObject.toPatchEvent(sessionId: String?): GatewayEvent {
    val status = firstString("status", "state", "decision").lowercase()
    if (status.isBlank() || status in PENDING_PATCH_STATUSES) {
        return GatewayEvent.PatchUpsert(toPatchProposal(sessionId))
    }
    return GatewayEvent.PatchUpdated(
        id = firstString("id", "patch_id").ifBlank { firstString("path", "file", "file_path") },
        status = status,
        sessionId = firstString("session_id").ifBlank { sessionId },
    )
}

private fun JSONObject.toQueuedMessage(sessionId: String?): GatewayQueuedMessage {
    return GatewayQueuedMessage(
        id = firstString("id", "message_id", "queued_id"),
        textPreview = firstString("text_preview", "preview", "text"),
        imageCount = optInt("image_count", 0),
        textAttachmentCount = optInt("text_attachment_count", 0),
        sessionId = firstString("session_id").ifBlank { sessionId },
    )
}

private fun JSONObject.toMessage(sessionId: String?): GatewayMessage {
    return GatewayMessage(
        id = firstString("id", "message_id").ifBlank { UUID.randomUUID().toString() },
        role = firstString("role").ifBlank { "assistant" },
        content = firstString("content", "text", "delta"),
        createdAt = firstString("created_at", "ts"),
        sessionId = firstString("session_id").ifBlank { sessionId },
        isStreaming = optBoolean("is_streaming", false),
    )
}

private fun JSONObject.toWorktree(sessionId: String?): GatewayWorktree {
    return GatewayWorktree(
        repositoryRoot = firstString("repository_root", "repositoryRoot").ifBlank { null },
        fileCount = optInt("file_count", optInt("fileCount", 0)),
        totalAddedLines = optInt("total_added_lines", optInt("totalAddedLines", 0)),
        totalDeletedLines = optInt("total_deleted_lines", optInt("totalDeletedLines", 0)),
        files = optJSONArray("files").toWorktreeFiles(),
        sessionId = firstString("session_id").ifBlank { sessionId },
    )
}

private fun JSONArray?.toWorktreeFiles(): List<GatewayWorktreeFile> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = getJSONObject(index)
            add(
                GatewayWorktreeFile(
                    path = item.firstString("path", "file_path"),
                    kind = item.firstString("kind", "status"),
                    addedLines = item.optInt("added_lines", item.optInt("addedLines", 0)),
                    deletedLines = item.optInt("deleted_lines", item.optInt("deletedLines", 0)),
                )
            )
        }
    }
}

private fun JSONObject.toJob(sessionId: String?): GatewayJob {
    val id = firstString("id", "job_id")
    return GatewayJob(
        id = id,
        handle = firstString("handle").ifBlank { id },
        command = firstString("command"),
        status = firstString("status").ifBlank { "unknown" },
        isAlive = optBoolean("is_alive", false),
        pid = optInt("pid", 0),
        exitCode = if (has("exit_code") && !isNull("exit_code")) optInt("exit_code") else null,
        outputByteCount = optInt("output_byte_count", 0),
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

private val PENDING_TOOL_STATUSES = setOf(
    "pending",
    "pending_approval",
    "awaiting_confirm",
    "awaiting_user_answer",
    "waiting",
    "needs_approval",
)

private val PENDING_PATCH_STATUSES = setOf(
    "pending",
    "pending_review",
    "waiting",
    "needs_review",
)
