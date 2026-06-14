package com.willdeep.android.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MobileCommandStatusTest {
    @Test
    fun upsertKeepsLatestTwentyCommands() {
        val statuses = (1..25).fold(emptyList<MobileCommandStatus>()) { current, index ->
            current.upsertCommandStatus(
                MobileCommandStatus(
                    id = "cmd_$index",
                    type = "message.send",
                    state = MobileCommandState.Pending,
                )
            )
        }

        assertEquals(20, statuses.size)
        assertEquals("cmd_6", statuses.first().id)
        assertEquals("cmd_25", statuses.last().id)
    }

    @Test
    fun ackMarksCommandById() {
        val statuses = listOf(
            MobileCommandStatus("cmd_1", "message.send", MobileCommandState.Pending),
            MobileCommandStatus("cmd_2", "file.read", MobileCommandState.Pending),
        ).markCommandAccepted(commandId = "cmd_2", commandType = "file.read")

        assertEquals(MobileCommandState.Pending, statuses.first().state)
        assertEquals(MobileCommandState.Accepted, statuses.last().state)
    }

    @Test
    fun ackFallsBackToPendingCommandType() {
        val statuses = listOf(
            MobileCommandStatus("cmd_1", "message.send", MobileCommandState.Pending),
            MobileCommandStatus("cmd_2", "file.read", MobileCommandState.Pending),
        ).markCommandAccepted(commandId = null, commandType = "message.send")

        assertEquals(MobileCommandState.Accepted, statuses.first().state)
        assertEquals(MobileCommandState.Pending, statuses.last().state)
    }

    @Test
    fun errorFallsBackToLatestPendingCommand() {
        val statuses = listOf(
            MobileCommandStatus("cmd_1", "message.send", MobileCommandState.Pending),
            MobileCommandStatus("cmd_2", "file.read", MobileCommandState.Pending),
        ).markCommandFailed(commandId = null, message = "desktop rejected command")

        assertEquals(MobileCommandState.Pending, statuses.first().state)
        assertEquals(MobileCommandState.Failed, statuses.last().state)
        assertEquals("desktop rejected command", statuses.last().detail)
    }
}
