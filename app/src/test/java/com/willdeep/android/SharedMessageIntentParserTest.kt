package com.willdeep.android

import org.junit.Assert.assertEquals
import org.junit.Test

class SharedMessageIntentParserTest {
    @Test
    fun extractsSharedTextForTextSendIntent() {
        val text = SharedMessageIntentParser.extractText(
            action = "android.intent.action.SEND",
            mimeType = "text/plain",
            extraText = "  Build a README summary  ",
        )

        assertEquals("Build a README summary", text)
    }

    @Test
    fun rejectsNonTextShareIntents() {
        val text = SharedMessageIntentParser.extractText(
            action = "android.intent.action.SEND",
            mimeType = "image/png",
            extraText = "Build a README summary",
        )

        assertEquals("", text)
    }

    @Test
    fun rejectsNonSendActions() {
        val text = SharedMessageIntentParser.extractText(
            action = "android.intent.action.VIEW",
            mimeType = "text/plain",
            extraText = "Build a README summary",
        )

        assertEquals("", text)
    }
}
