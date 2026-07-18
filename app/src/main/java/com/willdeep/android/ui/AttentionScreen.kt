package com.willdeep.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.willdeep.android.R
import com.willdeep.android.mobile.PatchProposal
import com.willdeep.android.mobile.PendingToolApproval

private enum class AttentionFilter { All, Approvals, Questions, Failures }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttentionScreen(
    state: MobileGatewayUiState,
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit,
    onToolDecision: (PendingToolApproval, Boolean) -> Unit,
    onToolAnswerChange: (String, String) -> Unit,
    onToolConfirmationChange: (String, String) -> Unit,
    onPatchDecision: (PatchProposal, Boolean) -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf(AttentionFilter.All) }
    val tools = when (filter) {
        AttentionFilter.All, AttentionFilter.Approvals -> state.pendingTools
        AttentionFilter.Questions -> state.pendingTools.filter { it.requiresAnswer }
        AttentionFilter.Failures -> emptyList()
    }
    val patches = if (filter == AttentionFilter.All || filter == AttentionFilter.Approvals) {
        state.patchProposals
    } else {
        emptyList()
    }
    val failures = if (filter == AttentionFilter.All || filter == AttentionFilter.Failures) {
        state.commandStatuses.filter { it.state == MobileCommandState.Failed }
    } else {
        emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back_button),
                        )
                    }
                },
                title = {
                    Column {
                        Text(stringResource(R.string.attention_title), fontWeight = FontWeight.Bold)
                        Text(
                            text = stringResource(
                                R.string.attention_pending_count,
                                state.pendingTools.size + state.patchProposals.size,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(AttentionFilter.entries) { item ->
                    FilterChip(
                        selected = filter == item,
                        onClick = { filter = item },
                        label = { Text(stringResource(item.labelResource())) },
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                tools.forEach { tool ->
                    item(key = "tool-${tool.id}") {
                        AttentionApprovalItem(
                            state = state.copy(
                                pendingTools = listOf(tool),
                                patchProposals = emptyList(),
                            ),
                            sessionId = tool.sessionId,
                            onOpenSession = onOpenSession,
                            onToolDecision = onToolDecision,
                            onToolAnswerChange = onToolAnswerChange,
                            onToolConfirmationChange = onToolConfirmationChange,
                            onPatchDecision = onPatchDecision,
                        )
                    }
                }
                patches.forEach { patch ->
                    item(key = "patch-${patch.id}") {
                        AttentionApprovalItem(
                            state = state.copy(
                                pendingTools = emptyList(),
                                patchProposals = listOf(patch),
                            ),
                            sessionId = patch.sessionId,
                            onOpenSession = onOpenSession,
                            onToolDecision = onToolDecision,
                            onToolAnswerChange = onToolAnswerChange,
                            onToolConfirmationChange = onToolConfirmationChange,
                            onPatchDecision = onPatchDecision,
                        )
                    }
                }
                items(failures, key = { "failed-${it.id}" }) { command ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.attention_failure_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Text(command.type, style = MaterialTheme.typography.bodyMedium)
                            if (command.detail.isNotBlank()) {
                                Text(
                                    command.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    }
                }
                if (tools.isEmpty() && patches.isEmpty() && failures.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFFF3F6EF),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.attention_empty),
                                modifier = Modifier.padding(20.dp),
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttentionApprovalItem(
    state: MobileGatewayUiState,
    sessionId: String?,
    onOpenSession: (String) -> Unit,
    onToolDecision: (PendingToolApproval, Boolean) -> Unit,
    onToolAnswerChange: (String, String) -> Unit,
    onToolConfirmationChange: (String, String) -> Unit,
    onPatchDecision: (PatchProposal, Boolean) -> Unit,
) {
    val session = state.sessions.firstOrNull { it.id == sessionId }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session?.title?.ifBlank { session.workspaceName }
                        ?: stringResource(R.string.attention_unknown_session),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                session?.workspaceName?.takeIf { it.isNotBlank() }?.let { workspace ->
                    Text(
                        workspace,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            if (sessionId != null) {
                TextButton(onClick = { onOpenSession(sessionId) }) {
                    Text(stringResource(R.string.attention_open_session))
                }
            }
        }
        ApprovalCard(
            state = state,
            onToolDecision = onToolDecision,
            onToolAnswerChange = onToolAnswerChange,
            onToolConfirmationChange = onToolConfirmationChange,
            onPatchDecision = onPatchDecision,
            showSectionTitle = false,
        )
    }
}

private fun AttentionFilter.labelResource(): Int = when (this) {
    AttentionFilter.All -> R.string.attention_filter_all
    AttentionFilter.Approvals -> R.string.attention_filter_approvals
    AttentionFilter.Questions -> R.string.attention_filter_questions
    AttentionFilter.Failures -> R.string.attention_filter_failures
}
