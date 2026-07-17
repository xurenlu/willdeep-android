package com.willdeep.android.ui

import com.willdeep.android.mobile.GatewayEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopAppResponsePolicyTest {
    @Test
    fun transportOpenAloneDoesNotProveMacAppIsOnline() {
        assertFalse(GatewayEvent.Connected.isDesktopHeartbeatEvent())
        assertFalse(GatewayEvent.Raw("session.list").isDesktopHeartbeatEvent())
        assertFalse(GatewayEvent.ConnectionClosed(1000, "closed").isDesktopHeartbeatEvent())
    }

    @Test
    fun macSnapshotAndCommandAcknowledgementProveAppResponse() {
        assertTrue(emptySnapshot().isDesktopHeartbeatEvent())
        assertTrue(
            GatewayEvent.Ack(
                commandId = "command-1",
                commandType = "session.list",
                sessionId = null,
            ).isDesktopHeartbeatEvent(),
        )
    }

    private fun emptySnapshot(): GatewayEvent.Snapshot {
        return GatewayEvent.Snapshot(
            sessions = emptyList(),
            activeSessionId = null,
            pendingTools = emptyList(),
            patchProposals = emptyList(),
            jobs = emptyList(),
            queuedMessages = emptyList(),
            messages = emptyList(),
            worktrees = emptyList(),
        )
    }
}
