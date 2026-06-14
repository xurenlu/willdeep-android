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
    fun combinesSharedSubjectAndText() {
        val text = SharedMessageIntentParser.extractText(
            action = "android.intent.action.SEND",
            mimeType = "text/plain; charset=utf-8",
            extraSubject = "GitHub Issue 42",
            extraText = "https://example.test/issues/42",
        )

        assertEquals("GitHub Issue 42\n\nhttps://example.test/issues/42", text)
    }

    @Test
    fun usesSharedTitleWhenSubjectIsMissing() {
        val text = SharedMessageIntentParser.extractText(
            action = "android.intent.action.SEND",
            mimeType = "text/plain",
            extraTitle = "Android crash report",
            extraText = "https://example.test/crashes/7",
        )

        assertEquals("Android crash report\n\nhttps://example.test/crashes/7", text)
    }

    @Test
    fun prefersSubjectOverSharedTitle() {
        val text = SharedMessageIntentParser.extractText(
            action = "android.intent.action.SEND",
            mimeType = "text/plain",
            extraSubject = "Issue subject",
            extraTitle = "Chooser title",
            extraText = "https://example.test/issues/9",
        )

        assertEquals("Issue subject\n\nhttps://example.test/issues/9", text)
    }

    @Test
    fun keepsSubjectWhenSharedTextIsMissing() {
        val text = SharedMessageIntentParser.extractText(
            action = "android.intent.action.SEND",
            mimeType = "text/plain",
            extraSubject = "Build mobile approval UI",
            extraText = " ",
        )

        assertEquals("Build mobile approval UI", text)
    }

    @Test
    fun avoidsDuplicatingMatchingSubjectAndText() {
        val text = SharedMessageIntentParser.extractText(
            action = "android.intent.action.SEND",
            mimeType = "text/plain",
            extraSubject = "Build mobile approval UI",
            extraText = "Build mobile approval UI",
        )

        assertEquals("Build mobile approval UI", text)
    }

    @Test
    fun extractsSelectedTextFromProcessTextIntent() {
        val text = SharedMessageIntentParser.extractText(
            action = "android.intent.action.PROCESS_TEXT",
            mimeType = "text/plain",
            extraText = "ignored share body",
            extraProcessText = "  Refactor the selected Kotlin class  ",
        )

        assertEquals("Refactor the selected Kotlin class", text)
    }

    @Test
    fun rejectsProcessTextIntentWithoutTextMimeType() {
        val text = SharedMessageIntentParser.extractText(
            action = "android.intent.action.PROCESS_TEXT",
            mimeType = "image/png",
            extraProcessText = "Refactor the selected Kotlin class",
        )

        assertEquals("", text)
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
