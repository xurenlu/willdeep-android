package com.willdeep.android.ui

import com.willdeep.android.mobile.GatewayMessage
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
}
