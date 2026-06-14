package com.willdeep.android.ui

import com.willdeep.android.mobile.GatewayJob
import com.willdeep.android.mobile.GatewayMessage
import com.willdeep.android.mobile.GatewaySession
import com.willdeep.android.mobile.GatewayWorktree
import com.willdeep.android.mobile.GatewayWorktreeFile
import com.willdeep.android.mobile.PatchProposal
import com.willdeep.android.mobile.PendingToolApproval
import org.junit.Assert.assertEquals
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
        assertEquals(null, echoedState.agentActivitySignalAfter(baseline))
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
        assertEquals(
            AgentActivitySignal.AssistantText,
            streamingState.agentActivitySignalAfter(baseline),
        )
        assertFalse(streamingState.hasCodeActivityAfter(baseline))
        assertEquals(null, streamingState.codeActivitySignalAfter(baseline))
    }

    @Test
    fun newAssistantMessageCountsAsAgentActivity() {
        val baseline = AgentActivityBaseline.capture(MobileGatewayUiState())
        val state = MobileGatewayUiState(
            conversationMessages = listOf(assistantMessage("assistant_1", "Starting work")),
        )

        assertTrue(state.hasAgentActivityAfter(baseline))
        assertEquals(AgentActivitySignal.AssistantMessage, state.agentActivitySignalAfter(baseline))
        assertFalse(state.hasCodeActivityAfter(baseline))
        assertEquals(null, state.codeActivitySignalAfter(baseline))
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
        assertEquals(AgentActivitySignal.RespondingSession, state.agentActivitySignalAfter(baseline))
    }

    @Test
    fun toolPatchJobAndWorktreeSignalsCountAsAgentActivity() {
        val baseline = AgentActivityBaseline.capture(MobileGatewayUiState())

        assertEquals(
            AgentActivitySignal.PendingTool,
            MobileGatewayUiState(pendingTools = listOf(pendingTool())).agentActivitySignalAfter(baseline),
        )
        assertEquals(
            AgentActivitySignal.PatchProposal,
            MobileGatewayUiState(patchProposals = listOf(patchProposal())).agentActivitySignalAfter(baseline),
        )
        assertEquals(
            AgentActivitySignal.LiveJob,
            MobileGatewayUiState(jobs = listOf(liveJob())).agentActivitySignalAfter(baseline),
        )
        assertEquals(
            AgentActivitySignal.WorktreeFile,
            MobileGatewayUiState(
                worktree = worktree(files = listOf(worktreeFile("README.md"))),
            ).agentActivitySignalAfter(baseline),
        )
    }

    @Test
    fun codeActivityOnlyIncludesToolPatchJobAndWorktreeSignals() {
        val baseline = AgentActivityBaseline.capture(MobileGatewayUiState())

        assertEquals(
            AgentActivitySignal.PendingTool,
            MobileGatewayUiState(pendingTools = listOf(pendingTool())).codeActivitySignalAfter(baseline),
        )
        assertEquals(
            AgentActivitySignal.PatchProposal,
            MobileGatewayUiState(patchProposals = listOf(patchProposal())).codeActivitySignalAfter(baseline),
        )
        assertEquals(
            AgentActivitySignal.LiveJob,
            MobileGatewayUiState(jobs = listOf(liveJob())).codeActivitySignalAfter(baseline),
        )
        assertEquals(
            AgentActivitySignal.WorktreeFile,
            MobileGatewayUiState(
                worktree = worktree(files = listOf(worktreeFile("README.md"))),
            ).codeActivitySignalAfter(baseline),
        )
        assertEquals(
            null,
            MobileGatewayUiState(
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
            ).codeActivitySignalAfter(baseline),
        )
        assertEquals(
            null,
            MobileGatewayUiState(
                conversationMessages = listOf(assistantMessage("assistant_1", "Starting work")),
            ).codeActivitySignalAfter(baseline),
        )
    }

    @Test
    fun codeActivityStillFindsToolSignalWhenSessionIsResponding() {
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
            pendingTools = listOf(pendingTool()),
        )

        assertEquals(AgentActivitySignal.RespondingSession, state.agentActivitySignalAfter(baseline))
        assertTrue(state.hasCodeActivityAfter(baseline))
        assertEquals(AgentActivitySignal.PendingTool, state.codeActivitySignalAfter(baseline))
    }

    @Test
    fun targetFileActivityMatchesNewPatchOrWorktreeFile() {
        val baseline = AgentActivityBaseline.capture(MobileGatewayUiState())

        assertEquals(
            AgentActivitySignal.PatchProposal,
            MobileGatewayUiState(
                patchProposals = listOf(
                    patchProposal(path = "WILLDEEP_ANDROID_LIVE_ACCEPTANCE.md"),
                ),
            ).targetFileActivitySignalAfter(baseline, "WILLDEEP_ANDROID_LIVE_ACCEPTANCE.md"),
        )
        assertEquals(
            AgentActivitySignal.WorktreeFile,
            MobileGatewayUiState(
                worktree = worktree(
                    files = listOf(worktreeFile("notes/WILLDEEP_ANDROID_LIVE_ACCEPTANCE.md")),
                ),
            ).targetFileActivitySignalAfter(baseline, "WILLDEEP_ANDROID_LIVE_ACCEPTANCE.md"),
        )
        assertTrue(
            MobileGatewayUiState(
                worktree = worktree(
                    files = listOf(worktreeFile("./WILLDEEP_ANDROID_LIVE_ACCEPTANCE.md")),
                ),
            ).hasTargetFileActivityAfter(baseline, "WILLDEEP_ANDROID_LIVE_ACCEPTANCE.md"),
        )
    }

    @Test
    fun targetFileActivityIgnoresExistingWorktreeFileAndOtherPaths() {
        val baselineState = MobileGatewayUiState(
            worktree = worktree(
                files = listOf(worktreeFile("WILLDEEP_ANDROID_LIVE_ACCEPTANCE.md")),
            ),
        )
        val baseline = AgentActivityBaseline.capture(baselineState)

        assertEquals(
            null,
            baselineState.targetFileActivitySignalAfter(baseline, "WILLDEEP_ANDROID_LIVE_ACCEPTANCE.md"),
        )
        assertEquals(
            null,
            MobileGatewayUiState(
                patchProposals = listOf(patchProposal(path = "README.md")),
                worktree = worktree(files = listOf(worktreeFile("README.md"))),
            ).targetFileActivitySignalAfter(baseline, "WILLDEEP_ANDROID_LIVE_ACCEPTANCE.md"),
        )
    }

    @Test
    fun targetFileActivityCountsExistingWorktreeFileWhenStatsChange() {
        val baselineState = MobileGatewayUiState(
            worktree = worktree(
                files = listOf(
                    worktreeFile(
                        path = "WILLDEEP_ANDROID_LIVE_ACCEPTANCE.md",
                        addedLines = 1,
                    ),
                ),
            ),
        )
        val baseline = AgentActivityBaseline.capture(baselineState)
        val updatedState = baselineState.copy(
            worktree = worktree(
                files = listOf(
                    worktreeFile(
                        path = "WILLDEEP_ANDROID_LIVE_ACCEPTANCE.md",
                        addedLines = 2,
                    ),
                ),
            ),
        )

        assertEquals(
            AgentActivitySignal.WorktreeFile,
            updatedState.targetFileActivitySignalAfter(baseline, "WILLDEEP_ANDROID_LIVE_ACCEPTANCE.md"),
        )
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

    private fun patchProposal(path: String = "README.md"): PatchProposal {
        return PatchProposal(
            id = "patch_1",
            title = "Patch",
            summary = path,
            path = path,
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

    private fun worktree(files: List<GatewayWorktreeFile> = emptyList()): GatewayWorktree {
        return GatewayWorktree(
            repositoryRoot = "/workspace",
            fileCount = files.size,
            totalAddedLines = files.sumOf { file -> file.addedLines },
            totalDeletedLines = files.sumOf { file -> file.deletedLines },
            files = files,
            sessionId = "s1",
        )
    }

    private fun worktreeFile(
        path: String,
        kind: String = "modified",
        addedLines: Int = 1,
        deletedLines: Int = 0,
    ): GatewayWorktreeFile {
        return GatewayWorktreeFile(
            path = path,
            kind = kind,
            addedLines = addedLines,
            deletedLines = deletedLines,
        )
    }
}
