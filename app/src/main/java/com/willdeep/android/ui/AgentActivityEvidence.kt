package com.willdeep.android.ui

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
    val assistantMessages = conversationMessages.filter { message ->
        message.role == "assistant"
    }
    return sessions.any { session -> session.isResponding } ||
        assistantMessages.size > baseline.assistantMessageCount ||
        assistantMessages.sumOf { message -> message.content.length } > baseline.assistantTextLength ||
        pendingTools.size > baseline.pendingToolCount ||
        patchProposals.size > baseline.patchProposalCount ||
        jobs.count { job -> job.isAlive } > baseline.liveJobCount ||
        (worktree?.fileCount ?: 0) > baseline.worktreeFileCount
}
