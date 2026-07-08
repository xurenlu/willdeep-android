package com.willdeep.android.push

import com.willdeep.android.MobileAttentionActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileRemotePushHandlerTest {
    @Test
    fun parsesAskUserPayloadAsToolAttentionRequiringAnswer() {
        val push = RemoteAttentionPush.parse(
            """
            {
              "kind": "ask_user",
              "target_id": "approval-1",
              "session_id": "session-1",
              "title": "Need input",
              "summary": "Please continue in session"
            }
            """.trimIndent()
        )

        assertEquals(MobileAttentionActions.TARGET_TOOL, push.targetType)
        assertEquals("approval-1", push.targetId)
        assertEquals("session-1", push.sessionId)
        assertTrue(push.requiresAnswer)
    }

    @Test
    fun parsesPatchPayload() {
        val push = RemoteAttentionPush.parse(
            """
            {
              "target_type": "patch",
              "patch_id": "patch-1",
              "session_id": "session-1",
              "title": "Review patch",
              "path": "app/src/main/java/Main.kt"
            }
            """.trimIndent()
        )

        val proposal = push.toPatchProposal()
        assertEquals(MobileAttentionActions.TARGET_PATCH, push.targetType)
        assertEquals("patch-1", proposal.id)
        assertEquals("app/src/main/java/Main.kt", proposal.path)
    }
}
