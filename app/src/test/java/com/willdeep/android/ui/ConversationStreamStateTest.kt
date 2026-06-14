package com.willdeep.android.ui

import com.willdeep.android.mobile.GatewayMessage
import com.willdeep.android.mobile.PendingToolApproval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationStreamStateTest {
    @Test
    fun deltaCreatesStreamingAssistantMessage() {
        val messages = emptyList<GatewayMessage>().appendDelta(
            sessionId = "s1",
            messageId = "m1",
            text = "Editing",
        )

        assertEquals(1, messages.size)
        assertEquals("m1", messages.single().id)
        assertEquals("assistant", messages.single().role)
        assertEquals("Editing", messages.single().content)
        assertTrue(messages.single().isStreaming)
    }

    @Test
    fun doneMarksStreamingMessageComplete() {
        val messages = listOf(
            GatewayMessage(
                id = "m1",
                role = "assistant",
                content = "Editing files",
                createdAt = "",
                sessionId = "s1",
                isStreaming = true,
            )
        ).markMessageDone(sessionId = "s1", messageId = "m1")

        assertFalse(messages.single().isStreaming)
    }

    @Test
    fun snapshotKeepsOnlyAnswerDraftsForCurrentAskUserApprovals() {
        val answers = mapOf(
            "ask_1" to "Use main",
            "ask_removed" to "Old answer",
            "tool_1" to "Not an answer prompt",
        )
        val approvals = listOf(
            PendingToolApproval(
                id = "ask_1",
                title = "Question",
                summary = "Which branch?",
                toolName = "ask_user",
                inputPreview = "",
                requiresAnswer = true,
                sessionId = "s1",
            ),
            PendingToolApproval(
                id = "tool_1",
                title = "Shell",
                summary = "Run tests",
                toolName = "shell",
                inputPreview = "./gradlew test",
                requiresAnswer = false,
                sessionId = "s1",
            ),
        )

        val kept = answers.keepAnswersFor(approvals)

        assertEquals(mapOf("ask_1" to "Use main"), kept)
    }
}
