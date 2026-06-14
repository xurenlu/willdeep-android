package com.willdeep.android.ui

import com.willdeep.android.mobile.GatewayWorktreeFile

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
    val worktreeFilePaths: Set<String>,
    val worktreeFileSignatures: Set<String>,
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
                worktreeFilePaths = state.worktree?.files
                    ?.map { file -> normalizeActivityPath(file.path) }
                    ?.toSet()
                    ?: emptySet(),
                worktreeFileSignatures = state.worktree?.files
                    ?.map { file -> file.activitySignature() }
                    ?.toSet()
                    ?: emptySet(),
            )
        }
    }
}

fun MobileGatewayUiState.hasAgentActivityAfter(baseline: AgentActivityBaseline): Boolean {
    return agentActivitySignalAfter(baseline) != null
}

fun MobileGatewayUiState.hasCodeActivityAfter(baseline: AgentActivityBaseline): Boolean {
    return codeActivitySignalAfter(baseline) != null
}

fun MobileGatewayUiState.codeActivitySignalAfter(
    baseline: AgentActivityBaseline,
): AgentActivitySignal? {
    return when {
        pendingTools.size > baseline.pendingToolCount -> AgentActivitySignal.PendingTool
        patchProposals.size > baseline.patchProposalCount -> AgentActivitySignal.PatchProposal
        jobs.count { job -> job.isAlive } > baseline.liveJobCount -> AgentActivitySignal.LiveJob
        (worktree?.fileCount ?: 0) > baseline.worktreeFileCount -> AgentActivitySignal.WorktreeFile
        else -> null
    }
}

fun MobileGatewayUiState.hasTargetFileActivityAfter(
    baseline: AgentActivityBaseline,
    targetFile: String,
): Boolean {
    return targetFileActivitySignalAfter(baseline, targetFile) != null
}

fun MobileGatewayUiState.targetFileActivitySignalAfter(
    baseline: AgentActivityBaseline,
    targetFile: String,
): AgentActivitySignal? {
    val normalizedTarget = normalizeActivityPath(targetFile)
    if (normalizedTarget.isEmpty()) return null

    return when {
        patchProposals
            .drop(baseline.patchProposalCount)
            .any { proposal -> proposal.path.matchesActivityPath(normalizedTarget) } ->
            AgentActivitySignal.PatchProposal
        worktree?.files
            ?.filter { file ->
                val normalizedPath = normalizeActivityPath(file.path)
                normalizedPath !in baseline.worktreeFilePaths ||
                    file.activitySignature() !in baseline.worktreeFileSignatures
            }
            ?.any { file -> file.path.matchesActivityPath(normalizedTarget) } == true ->
            AgentActivitySignal.WorktreeFile
        else -> null
    }
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

private fun String.matchesActivityPath(normalizedTarget: String): Boolean {
    val normalizedPath = normalizeActivityPath(this)
    return normalizedPath == normalizedTarget || normalizedPath.endsWith("/$normalizedTarget")
}

private fun GatewayWorktreeFile.activitySignature(): String {
    return listOf(
        normalizeActivityPath(path),
        kind,
        addedLines.toString(),
        deletedLines.toString(),
    ).joinToString("|")
}

private fun normalizeActivityPath(path: String): String {
    return path.trim()
        .replace('\\', '/')
        .removePrefix("./")
        .trim('/')
}
