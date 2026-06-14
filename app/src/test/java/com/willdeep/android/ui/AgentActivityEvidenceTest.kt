package com.willdeep.android.ui

import com.willdeep.android.mobile.GatewayJob
import com.willdeep.android.mobile.GatewayMessage
import com.willdeep.android.mobile.GatewaySession
import com.willdeep.android.mobile.GatewayWorktree
import com.willdeep.android.mobile.PatchProposal
import com.willdeep.android.mobile.PendingToolApproval
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentActivityEvidenceTest {
    @Test
    fun userMessageEchoDoesNotCountAsAgentActivity() {
        val baselineState = MobileGatewayUiState(
            conversationMessages = listOf(userMessage("user_1", "Please edit README.md")),
        )
        val baseline = AgentActivityBaseline.capture(baselineState)
        val echoedState = baselineState.copy(
            conversationMessages = baselineState.conversationMessages + userMessage(
                id = "user_2",
                content = "Please edit README.md",
            ),
        )

        assertFalse(echoedState.hasAgentActivityAfter(baseline))
    }

    @Test
    fun assistantDeltaCountsAsAgentActivity() {
        val baselineState = MobileGatewayUiState(
            conversationMessages = listOf(assistantMessage("assistant_1", "Thinking")),
        )
        val baseline = AgentActivityBaseline.capture(baselineState)
        val streamingState = baselineState.copy(
            conversationMessages = listOf(assistantMessage("assistant_1", "Thinking\nEditing files")),
        )

        assertTrue(streamingState.hasAgentActivityAfter(baseline))
    }

    @Test
    fun newAssistantMessageCountsAsAgentActivity() {
        val baseline = AgentActivityBaseline.capture(MobileGatewayUiState())
        val state = MobileGatewayUiState(
            conversationMessages = listOf(assistantMessage("assistant_1", "Starting work")),
        )

        assertTrue(state.hasAgentActivityAfter(baseline))
    }

    @Test
    fun respondingSessionCountsAsAgentActivity() {
        val baseline = AgentActivityBaseline.capture(MobileGatewayUiState())
        val state = MobileGatewayUiState(
            sessions = listOf(
                GatewaySession(
                    id = "s1",
                    title = "Coding",
                    workspaceName = "WillDeep",
                    messageCount = 1,
                    isActive = true,
                    isResponding = true,
                )
            ),
        )

        assertTrue(state.hasAgentActivityAfter(baseline))
    }

    @Test
    fun toolPatchJobAndWorktreeSignalsCountAsAgentActivity() {
        val baseline = AgentActivityBaseline.capture(MobileGatewayUiState())

        assertTrue(MobileGatewayUiState(pendingTools = listOf(pendingTool())).hasAgentActivityAfter(baseline))
        assertTrue(MobileGatewayUiState(patchProposals = listOf(patchProposal())).hasAgentActivityAfter(baseline))
        assertTrue(MobileGatewayUiState(jobs = listOf(liveJob())).hasAgentActivityAfter(baseline))
        assertTrue(MobileGatewayUiState(worktree = worktree()).hasAgentActivityAfter(baseline))
    }

    private fun userMessage(id: String, content: String): GatewayMessage {
        return GatewayMessage(
            id = id,
            role = "user",
            content = content,
            createdAt = "",
            sessionId = "s1",
        )
    }

    private fun assistantMessage(id: String, content: String): GatewayMessage {
        return GatewayMessage(
            id = id,
            role = "assistant",
            content = content,
            createdAt = "",
            sessionId = "s1",
            isStreaming = true,
        )
    }

    private fun pendingTool(): PendingToolApproval {
        return PendingToolApproval(
            id = "tool_1",
            title = "Run tests",
            summary = "./gradlew test",
            toolName = "shell",
            inputPreview = "./gradlew test",
            requiresAnswer = false,
            sessionId = "s1",
        )
    }

    private fun patchProposal(): PatchProposal {
        return PatchProposal(
            id = "patch_1",
            title = "Patch",
            summary = "README.md",
            path = "README.md",
            stats = "+1 -0",
            sessionId = "s1",
        )
    }

    private fun liveJob(): GatewayJob {
        return GatewayJob(
            id = "job_1",
            handle = "gradle",
            command = "./gradlew test",
            status = "running",
            isAlive = true,
            pid = 42,
            exitCode = null,
            outputByteCount = 128,
            sessionId = "s1",
        )
    }

    private fun worktree(): GatewayWorktree {
        return GatewayWorktree(
            repositoryRoot = "/workspace",
            fileCount = 1,
            totalAddedLines = 1,
            totalDeletedLines = 0,
            files = emptyList(),
            sessionId = "s1",
        )
    }
}
