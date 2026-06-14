package com.willdeep.android

object SharedMessageIntentParser {
    private const val ACTION_SEND = "android.intent.action.SEND"

    fun extractText(
        action: String?,
        mimeType: String?,
        extraText: String?,
        extraSubject: String? = null,
    ): String {
        if (action != ACTION_SEND) return ""
        if (!mimeType.isTextMimeType()) return ""
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
