package com.willdeep.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.willdeep.android.R
import com.willdeep.android.mobile.GatewaySession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    viewModel: MobileGatewayViewModel,
    state: MobileGatewayUiState,
    onBack: () -> Unit,
    onScan: () -> Unit,
) {
    val activeSession: GatewaySession? = state.sessions.firstOrNull { it.id == state.selectedSessionId }
    val titleText = activeSession?.title?.ifBlank {
        activeSession.workspaceName.ifBlank { activeSession.id.take(8) }
    } ?: stringResource(R.string.session_detail_title)
    val subtitleText = activeSession?.workspaceName?.ifBlank { null }
    val sessionId = state.selectedSessionId
    val scopedState = state.copy(
        pendingTools = state.pendingTools.filter { it.sessionId == sessionId },
        patchProposals = state.patchProposals.filter { it.sessionId == sessionId },
        queuedMessages = state.queuedMessages.filter { it.sessionId == sessionId },
        conversationMessages = state.conversationMessages.filter { it.sessionId == sessionId },
        worktree = state.worktree?.takeIf { it.sessionId == null || it.sessionId == sessionId },
    )

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
                        Text(
                            text = titleText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (subtitleText != null) {
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onScan) {
                        Icon(
                            painter = painterResource(R.drawable.ic_scan_qr),
                            contentDescription = stringResource(R.string.scan_qr_button),
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (scopedState.pendingTools.isNotEmpty() || scopedState.patchProposals.isNotEmpty()) {
                item {
                    ApprovalCard(
                        state = scopedState,
                        onToolDecision = viewModel::decideTool,
                        onToolAnswerChange = viewModel::updateToolAnswer,
                        onToolConfirmationChange = viewModel::updateToolConfirmation,
                        onPatchDiffRequest = viewModel::requestPatchDiff,
                        onPatchDecision = viewModel::decidePatch,
                    )
                }
            }
            item {
                ConversationCard(scopedState)
            }
            if (scopedState.queuedMessages.isNotEmpty()) {
                item {
                    QueueCard(
                        state = scopedState,
                        onSendNow = viewModel::sendQueuedNow,
                        onRemove = viewModel::removeQueuedMessage,
                        onClear = viewModel::clearQueue,
                    )
                }
            }
            if (scopedState.worktree != null) {
                item {
                    WorktreeCard(
                        state = scopedState,
                        onReadFile = viewModel::requestWorktreeFileRead,
                    )
                }
            }
            item {
                ComposerCard(
                    state = state,
                    isResponding = activeSession?.isResponding == true,
                    onMessageChange = viewModel::updateMessage,
                    onSend = viewModel::sendMessage,
                    onStop = viewModel::stopTurn,
                    onAddAttachment = viewModel::addAttachment,
                    onRemoveAttachment = viewModel::removeAttachment,
                )
            }
        }
    }
}
