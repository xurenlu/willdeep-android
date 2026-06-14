package com.willdeep.android

object SharedMessageIntentParser {
    private const val ACTION_PROCESS_TEXT = "android.intent.action.PROCESS_TEXT"
    private const val ACTION_SEND = "android.intent.action.SEND"

    fun extractText(
        action: String?,
        mimeType: String?,
        extraText: String? = null,
        extraSubject: String? = null,
        extraProcessText: String? = null,
    ): String {
        if (!mimeType.isTextMimeType()) return ""
        if (action == ACTION_PROCESS_TEXT) {
            return extraProcessText?.trim().orEmpty()
        }
        if (action != ACTION_SEND) return ""
        val subject = extraSubject?.trim().orEmpty()
        val text = extraText?.trim().orEmpty()
        return when {
            subject.isBlank() -> text
            text.isBlank() -> subject
            subject == text -> text
            else -> "$subject\n\n$text"
        }
    }

    private fun String?.isTextMimeType(): Boolean {
        return this
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase()
            ?.startsWith("text/") == true
    }
}
