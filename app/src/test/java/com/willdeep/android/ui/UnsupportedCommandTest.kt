package com.willdeep.android.ui

import com.willdeep.android.mobile.GatewayEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnsupportedCommandTest {
    @Test
    fun parsesMacGatewayUnsupportedCommand() {
        val event = GatewayEvent.Error("cmd_1", "Unsupported mobile command: capabilities.get.")

        assertEquals("capabilities.get", event.unsupportedCommandType())
    }

    @Test
    fun parsesLegacyCliUnsupportedCommand() {
        val event = GatewayEvent.Error("cmd_2", "unsupported command: capabilities.get")

        assertEquals("capabilities.get", event.unsupportedCommandType())
    }

    @Test
    fun parsesNonOptionalUnsupportedCommand() {
        val event = GatewayEvent.Error("cmd_3", "Unsupported mobile command: tool.decide.")

        assertEquals("tool.decide", event.unsupportedCommandType())
    }

    @Test
    fun ignoresOtherErrors() {
        val event = GatewayEvent.Error("cmd_4", "Session not found: abc.")

        assertNull(event.unsupportedCommandType())
    }

    @Test
    fun optionalCommandsAreProbesOnly() {
        assertTrue("capabilities.get" in OPTIONAL_GATEWAY_COMMANDS)
        assertTrue("push.register" in OPTIONAL_GATEWAY_COMMANDS)
        assertTrue("message.send" !in OPTIONAL_GATEWAY_COMMANDS)
    }
}
