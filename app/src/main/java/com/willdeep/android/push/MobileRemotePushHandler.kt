package com.willdeep.android.push

import android.content.Context
import android.util.Log
import com.willdeep.android.MobileAttentionActions
import com.willdeep.android.MobileAttentionNotifier
import com.willdeep.android.mobile.PatchProposal
import com.willdeep.android.mobile.PendingToolApproval
import org.json.JSONObject
import java.util.UUID

object MobileRemotePushHandler {
    private const val TAG = "RemotePush"

    fun handleUmengPayload(context: Context, payload: String): Boolean {
        val attention = runCatching { RemoteAttentionPush.parse(payload) }
            .onFailure { error -> Log.w(TAG, "Invalid remote push payload: ${error.message}") }
            .getOrNull() ?: return false

        val notifier = MobileAttentionNotifier(context.applicationContext)
        when (attention.targetType) {
            MobileAttentionActions.TARGET_PATCH -> notifier.showPatchProposal(attention.toPatchProposal())
            else -> notifier.showToolApproval(attention.toToolApproval())
        }
        return true
    }
}

data class RemoteAttentionPush(
    val targetType: String,
    val targetId: String,
    val sessionId: String?,
    val title: String,
    val summary: String,
    val toolName: String,
    val inputPreview: String,
    val requiresAnswer: Boolean,
    val requiresConfirmation: Boolean,
) {
    fun toToolApproval(): PendingToolApproval {
        return PendingToolApproval(
            id = targetId,
            title = title.ifBlank { toolName.ifBlank { "Remote approval" } },
            summary = summary,
            toolName = toolName.ifBlank { "remote_attention" },
            inputPreview = inputPreview,
            requiresAnswer = requiresAnswer,
            requiresConfirmation = requiresConfirmation,
            sessionId = sessionId,
        )
    }

    fun toPatchProposal(): PatchProposal {
        return PatchProposal(
            id = targetId,
            title = title.ifBlank { "Patch proposal" },
            summary = summary,
            path = inputPreview,
            stats = "",
            sessionId = sessionId,
        )
    }

    companion object {
        fun parse(raw: String): RemoteAttentionPush {
            val json = JSONObject(raw)
            val targetType = json.firstString("target_type", "kind", "type")
                .normalizeTargetType()
            val requiresAnswer = json.optBoolean("requires_answer", false) ||
                json.optBoolean("requires_input", false) ||
                json.optString("kind").equals("ask_user", ignoreCase = true)
            val requiresConfirmation = json.optBoolean("requires_confirmation", false) ||
                json.optBoolean("requires_confirm", false)
            return RemoteAttentionPush(
                targetType = targetType,
                targetId = json.firstString("target_id", "approval_id", "patch_id", "id")
                    .ifBlank { UUID.randomUUID().toString() },
                sessionId = json.firstString("session_id").ifBlank { null },
                title = json.firstString("title").take(MAX_FIELD_LENGTH),
                summary = json.firstString("summary", "body", "message").take(MAX_FIELD_LENGTH),
                toolName = json.firstString("tool_name", "tool").take(MAX_FIELD_LENGTH),
                inputPreview = json.firstString("input_preview", "path", "preview").take(MAX_FIELD_LENGTH),
                requiresAnswer = requiresAnswer,
                requiresConfirmation = requiresConfirmation,
            )
        }
    }
}

private fun JSONObject.firstString(vararg keys: String): String {
    for (key in keys) {
        val value = opt(key) ?: continue
        val text = value.toString().trim()
        if (text.isNotBlank() && text != "null") return text
    }
    return ""
}

private fun String.normalizeTargetType(): String {
    return when {
        equals("patch", ignoreCase = true) ||
            equals("patch_proposal", ignoreCase = true) -> MobileAttentionActions.TARGET_PATCH
        else -> MobileAttentionActions.TARGET_TOOL
    }
}

private const val MAX_FIELD_LENGTH = 300
