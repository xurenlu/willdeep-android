package com.willdeep.android

enum class MobileAttentionDecision {
    Approve,
    Reject,
}

data class MobileAttentionActionRequest(
    val targetType: String,
    val targetId: String,
    val sessionId: String?,
    val decision: MobileAttentionDecision?,
) {
    fun isDecision(): Boolean = decision != null
}

object MobileAttentionActions {
    const val ACTION_OPEN_ATTENTION = "com.willdeep.android.action.OPEN_ATTENTION"
    const val ACTION_APPROVE_ATTENTION = "com.willdeep.android.action.APPROVE_ATTENTION"
    const val ACTION_REJECT_ATTENTION = "com.willdeep.android.action.REJECT_ATTENTION"

    const val EXTRA_TARGET_TYPE = "com.willdeep.android.extra.TARGET_TYPE"
    const val EXTRA_TARGET_ID = "com.willdeep.android.extra.TARGET_ID"
    const val EXTRA_SESSION_ID = "com.willdeep.android.extra.SESSION_ID"

    const val TARGET_TOOL = "tool"
    const val TARGET_PATCH = "patch"

    fun parse(
        action: String?,
        targetType: String?,
        targetId: String?,
        sessionId: String?,
    ): MobileAttentionActionRequest? {
        val normalizedAction = action?.trim()
        val decision = when (normalizedAction) {
            ACTION_OPEN_ATTENTION -> null
            ACTION_APPROVE_ATTENTION -> MobileAttentionDecision.Approve
            ACTION_REJECT_ATTENTION -> MobileAttentionDecision.Reject
            else -> return null
        }
        val normalizedTargetType = targetType?.trim().orEmpty()
        val normalizedTargetId = targetId?.trim().orEmpty()
        if (normalizedTargetType !in setOf(TARGET_TOOL, TARGET_PATCH) || normalizedTargetId.isBlank()) {
            return null
        }
        return MobileAttentionActionRequest(
            targetType = normalizedTargetType,
            targetId = normalizedTargetId,
            sessionId = sessionId?.trim()?.ifBlank { null },
            decision = decision,
        )
    }
}
