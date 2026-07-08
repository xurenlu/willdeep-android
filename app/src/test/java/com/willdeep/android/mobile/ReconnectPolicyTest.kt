package com.willdeep.android.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconnectPolicyTest {
    @Test
    fun delayUsesBoundedBackoff() {
        assertEquals(1_000L, ReconnectPolicy.delayMillisForAttempt(0))
        assertEquals(1_000L, ReconnectPolicy.delayMillisForAttempt(1))
        assertEquals(2_000L, ReconnectPolicy.delayMillisForAttempt(2))
        assertEquals(5_000L, ReconnectPolicy.delayMillisForAttempt(3))
        assertEquals(10_000L, ReconnectPolicy.delayMillisForAttempt(4))
        assertEquals(20_000L, ReconnectPolicy.delayMillisForAttempt(5))
        assertEquals(30_000L, ReconnectPolicy.delayMillisForAttempt(6))
        assertEquals(30_000L, ReconnectPolicy.delayMillisForAttempt(99))
    }

    @Test
    fun retryStopsAtMaxAttempts() {
        assertTrue(ReconnectPolicy.shouldRetry(1))
        assertTrue(ReconnectPolicy.shouldRetry(ReconnectPolicy.MAX_ATTEMPTS - 1))
        assertFalse(ReconnectPolicy.shouldRetry(ReconnectPolicy.MAX_ATTEMPTS))
    }

    @Test
    fun heartbeatExpiresAfterTimeoutWindow() {
        assertFalse(ReconnectPolicy.isHeartbeatExpired(nowMillis = 4_999L, lastEventMillis = 0L))
        assertTrue(ReconnectPolicy.isHeartbeatExpired(nowMillis = 5_000L, lastEventMillis = 0L))
        assertTrue(ReconnectPolicy.isHeartbeatExpired(nowMillis = 80_000L, lastEventMillis = 74_000L))
    }

    @Test
    fun authRejectionDetectsHttpAndGatewayMessages() {
        assertTrue(ReconnectPolicy.isAuthenticationRejected(401, "HTTP 401"))
        assertTrue(ReconnectPolicy.isAuthenticationRejected(403, "HTTP 403"))
        assertTrue(ReconnectPolicy.isAuthenticationRejected(null, "unauthorized"))
        assertTrue(ReconnectPolicy.isAuthenticationRejected(null, "Forbidden token"))
        assertFalse(ReconnectPolicy.isAuthenticationRejected(null, "timeout"))
        assertFalse(ReconnectPolicy.isAuthenticationRejected(500, "server error"))
    }
}
