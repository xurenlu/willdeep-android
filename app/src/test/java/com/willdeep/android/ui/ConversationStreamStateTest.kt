package com.willdeep.android.ui

import com.willdeep.android.mobile.GatewayMessage
import com.willdeep.android.mobile.PatchDiff
import com.willdeep.android.mobile.PatchProposal
import com.willdeep.android.mobile.PendingToolApproval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationStreamStateTest {
    @Test
    fun deltaCreatesStreamingAssistantMessage() {
        val messages = emptyList<GatewayMessage>().appendDelta(
            sessionId = "s1",
            messageId = "m1",
            text = "Editing",
        )

        assertEquals(1, messages.size)
        assertEquals("m1", messages.single().id)
        assertEquals("assistant", messages.single().role)
        assertEquals("Editing", messages.single().content)
        assertTrue(messages.single().isStreaming)
    }

    @Test
    fun doneMarksStreamingMessageComplete() {
        val messages = listOf(
            GatewayMessage(
                id = "m1",
                role = "assistant",
                content = "Editing files",
                createdAt = "",
                sessionId = "s1",
                isStreaming = true,
            )
        ).markMessageDone(sessionId = "s1", messageId = "m1")

        assertFalse(messages.single().isStreaming)
    }

    @Test
    fun snapshotKeepsOnlyAnswerDraftsForCurrentAskUserApprovals() {
        val answers = mapOf(
            "ask_1" to "Use main",
            "ask_removed" to "Old answer",
            "tool_1" to "Not an answer prompt",
        )
        val approvals = listOf(
            PendingToolApproval(
                id = "ask_1",
                title = "Question",
                summary = "Which branch?",
                toolName = "ask_user",
                inputPreview = "",
                requiresAnswer = true,
                sessionId = "s1",
            ),
            PendingToolApproval(
                id = "tool_1",
                title = "Shell",
                summary = "Run tests",
                toolName = "shell",
                inputPreview = "./gradlew test",
                requiresAnswer = false,
                sessionId = "s1",
            ),
        )

        val kept = answers.keepAnswersFor(approvals)

        assertEquals(mapOf("ask_1" to "Use main"), kept)
    }

    @Test
    fun snapshotKeepsOnlyConfirmationDraftsForDangerApprovals() {
        val confirmations = mapOf(
            "danger_1" to "confirm",
            "danger_removed" to "confirm",
            "tool_1" to "nope",
        )
        val approvals = listOf(
            PendingToolApproval(
                id = "danger_1",
                title = "Shell",
                summary = "Danger command",
                toolName = "shell",
                inputPreview = "rm -rf /tmp/example",
                requiresAnswer = false,
                requiresConfirmation = true,
                sessionId = "s1",
            ),
            PendingToolApproval(
                id = "tool_1",
                title = "Shell",
                summary = "Run tests",
                toolName = "shell",
                inputPreview = "./gradlew test",
                requiresAnswer = false,
                requiresConfirmation = false,
                sessionId = "s1",
            ),
        )

        val kept = confirmations.keepConfirmationsFor(approvals)

        assertEquals(mapOf("danger_1" to "confirm"), kept)
    }

    @Test
    fun toolUpdatedRemovalClearsPendingApprovalAnswerAndConfirmationDraft() {
        val state = MobileGatewayUiState(
            pendingTools = listOf(
                PendingToolApproval(
                    id = "tool_1",
                    title = "Shell",
                    summary = "Run tests",
                    toolName = "shell",
                    inputPreview = "./gradlew test",
                    requiresAnswer = false,
                    requiresConfirmation = true,
                    sessionId = "s1",
                ),
                PendingToolApproval(
                    id = "ask_1",
                    title = "Question",
                    summary = "Which branch?",
                    toolName = "ask_user",
                    inputPreview = "",
                    requiresAnswer = true,
                    sessionId = "s1",
                ),
            ),
            toolAnswers = mapOf(
                "tool_1" to "",
                "ask_1" to "Use main",
            ),
            toolConfirmations = mapOf(
                "tool_1" to "confirm",
                "ask_1" to "confirm",
            ),
        )

        val updated = state.removeToolApproval("ask_1")

        assertEquals(listOf("tool_1"), updated.pendingTools.map { it.id })
        assertEquals(mapOf("tool_1" to ""), updated.toolAnswers)
        assertEquals(mapOf("tool_1" to "confirm"), updated.toolConfirmations)
    }

    @Test
    fun patchUpdatedRemovalClearsProposalAndDiff() {
        val state = MobileGatewayUiState(
            patchProposals = listOf(
                PatchProposal(
                    id = "patch_1",
                    title = "Patch one",
                    summary = "First patch",
                    path = "README.md",
                    stats = "+1 -0",
                    sessionId = "s1",
                ),
                PatchProposal(
                    id = "patch_2",
                    title = "Patch two",
                    summary = "Second patch",
                    path = "app.kt",
                    stats = "+2 -1",
                    sessionId = "s1",
                ),
            ),
            patchDiffs = mapOf(
                "patch_1" to PatchDiff(
                    patchId = "patch_1",
                    title = "Patch one",
                    diff = "diff --git a/README.md b/README.md",
                    sessionId = "s1",
                ),
                "patch_2" to PatchDiff(
                    patchId = "patch_2",
                    title = "Patch two",
                    diff = "diff --git a/app.kt b/app.kt",
                    sessionId = "s1",
                ),
            ),
        )

        val updated = state.removePatchProposal("patch_1")

        assertEquals(listOf("patch_2"), updated.patchProposals.map { it.id })
        assertEquals(setOf("patch_2"), updated.patchDiffs.keys)
    }
}
