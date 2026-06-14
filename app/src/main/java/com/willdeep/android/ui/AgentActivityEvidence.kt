package com.willdeep.android.ui

enum class AgentActivitySignal(val reportValue: String) {
    RespondingSession("responding_session"),
    AssistantMessage("assistant_message"),
    AssistantText("assistant_text"),
    PendingTool("pending_tool"),
    PatchProposal("patch_proposal"),
    LiveJob("live_job"),
    WorktreeFile("worktree_file"),
}

data class AgentActivityBaseline(
    val assistantMessageCount: Int,
    val assistantTextLength: Int,
    val pendingToolCount: Int,
    val patchProposalCount: Int,
    val liveJobCount: Int,
    val worktreeFileCount: Int,
) {
    companion object {
        fun capture(state: MobileGatewayUiState): AgentActivityBaseline {
            val assistantMessages = state.conversationMessages.filter { message ->
                message.role == "assistant"
            }
            return AgentActivityBaseline(
                assistantMessageCount = assistantMessages.size,
                assistantTextLength = assistantMessages.sumOf { message -> message.content.length },
                pendingToolCount = state.pendingTools.size,
                patchProposalCount = state.patchProposals.size,
                liveJobCount = state.jobs.count { job -> job.isAlive },
                worktreeFileCount = state.worktree?.fileCount ?: 0,
            )
        }
    }
}

fun MobileGatewayUiState.hasAgentActivityAfter(baseline: AgentActivityBaseline): Boolean {
    return agentActivitySignalAfter(baseline) != null
}

fun MobileGatewayUiState.agentActivitySignalAfter(
    baseline: AgentActivityBaseline,
): AgentActivitySignal? {
    val assistantMessages = conversationMessages.filter { message ->
        message.role == "assistant"
    }
    return when {
        sessions.any { session -> session.isResponding } -> AgentActivitySignal.RespondingSession
        assistantMessages.size > baseline.assistantMessageCount -> AgentActivitySignal.AssistantMessage
        assistantMessages.sumOf { message -> message.content.length } > baseline.assistantTextLength ->
            AgentActivitySignal.AssistantText
        pendingTools.size > baseline.pendingToolCount -> AgentActivitySignal.PendingTool
        patchProposals.size > baseline.patchProposalCount -> AgentActivitySignal.PatchProposal
        jobs.count { job -> job.isAlive } > baseline.liveJobCount -> AgentActivitySignal.LiveJob
        (worktree?.fileCount ?: 0) > baseline.worktreeFileCount -> AgentActivitySignal.WorktreeFile
        else -> null
    }
}
