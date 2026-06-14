package com.willdeep.android

object SharedMessageIntentParser {
    private const val ACTION_SEND = "android.intent.action.SEND"

    fun extractText(action: String?, mimeType: String?, extraText: String?): String {
        if (action != ACTION_SEND) return ""
        if (!mimeType.isTextMimeType()) return ""
        return extraText?.trim().orEmpty()
    }

    private fun String?.isTextMimeType(): Boolean {
        return this
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase()
            ?.startsWith("text/") == true
    }
}
