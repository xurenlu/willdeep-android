package com.willdeep.android.mobile

object ReconnectPolicy {
    const val MAX_ATTEMPTS = 6
    const val HEARTBEAT_INTERVAL_MILLIS = 1_000L
    const val HEARTBEAT_TIMEOUT_MILLIS = 5_000L

    fun delayMillisForAttempt(attempt: Int): Long {
        val clamped = attempt.coerceIn(1, MAX_ATTEMPTS)
        return when (clamped) {
            1 -> 1_000L
            2 -> 2_000L
            3 -> 5_000L
            4 -> 10_000L
            5 -> 20_000L
            else -> 30_000L
        }
    }

    fun isHeartbeatExpired(nowMillis: Long, lastEventMillis: Long): Boolean {
        return nowMillis - lastEventMillis >= HEARTBEAT_TIMEOUT_MILLIS
    }

    fun shouldRetry(attempt: Int): Boolean {
        return attempt < MAX_ATTEMPTS
    }

    fun isAuthenticationRejected(httpCode: Int?, message: String): Boolean {
        if (httpCode == 401 || httpCode == 403) {
            return true
        }
        val lower = message.lowercase()
        return lower.contains("unauthorized") || lower.contains("forbidden")
    }
}
