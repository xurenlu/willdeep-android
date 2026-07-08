package com.willdeep.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MobileAttentionActionTest {
    @Test
    fun parsesOpenActionWithoutDecision() {
        val request = MobileAttentionActions.parse(
            action = MobileAttentionActions.ACTION_OPEN_ATTENTION,
            targetType = MobileAttentionActions.TARGET_TOOL,
            targetId = " approval-1 ",
            sessionId = " session-1 ",
        )

        assertEquals(
            MobileAttentionActionRequest(
                targetType = MobileAttentionActions.TARGET_TOOL,
                targetId = "approval-1",
                sessionId = "session-1",
                decision = null,
            ),
            request,
        )
    }

    @Test
    fun parsesApproveAndRejectActions() {
        val approve = MobileAttentionActions.parse(
            action = MobileAttentionActions.ACTION_APPROVE_ATTENTION,
            targetType = MobileAttentionActions.TARGET_PATCH,
            targetId = "patch-1",
            sessionId = null,
        )
        val reject = MobileAttentionActions.parse(
            action = MobileAttentionActions.ACTION_REJECT_ATTENTION,
            targetType = MobileAttentionActions.TARGET_PATCH,
            targetId = "patch-1",
            sessionId = "",
        )

        assertEquals(MobileAttentionDecision.Approve, approve?.decision)
        assertEquals(MobileAttentionDecision.Reject, reject?.decision)
        assertEquals(null, reject?.sessionId)
    }

    @Test
    fun rejectsUnknownActionsAndTargets() {
        assertNull(
            MobileAttentionActions.parse(
                action = "android.intent.action.VIEW",
                targetType = MobileAttentionActions.TARGET_TOOL,
                targetId = "approval-1",
                sessionId = null,
            )
        )
        assertNull(
            MobileAttentionActions.parse(
                action = MobileAttentionActions.ACTION_OPEN_ATTENTION,
                targetType = "session",
                targetId = "approval-1",
                sessionId = null,
            )
        )
        assertNull(
            MobileAttentionActions.parse(
                action = MobileAttentionActions.ACTION_APPROVE_ATTENTION,
                targetType = MobileAttentionActions.TARGET_TOOL,
                targetId = " ",
                sessionId = null,
            )
        )
    }
}
