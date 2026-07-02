package com.willdeep.android.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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
    val isResponding = activeSession?.isResponding == true
    val listState = rememberLazyListState()

    LaunchedEffect(scopedState.conversationMessages.size, scopedState.queuedMessages.size) {
        val target = listState.layoutInfo.totalItemsCount - 1
        if (target >= 0) listState.animateScrollToItem(target)
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
        bottomBar = {
            MessageInputBar(
                state = state,
                isResponding = isResponding,
                onMessageChange = viewModel::updateMessage,
                onSend = viewModel::sendMessage,
                onStop = viewModel::stopTurn,
                onAddAttachment = viewModel::addAttachment,
                onRemoveAttachment = viewModel::removeAttachment,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
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
        }
    }
}

@Composable
private fun MessageInputBar(
    state: MobileGatewayUiState,
    isResponding: Boolean,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onAddAttachment: (Uri) -> Unit,
    onRemoveAttachment: (Uri) -> Unit,
) {
    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
    ) { uris -> uris.forEach { onAddAttachment(it) } }

    val isConnected = state.status == ConnectionStatus.Connected
    val canSend = isConnected && (state.messageText.isNotBlank() || state.attachments.isNotEmpty())
    val placeholder = when {
        !isConnected -> stringResource(R.string.composer_placeholder_offline)
        isResponding -> stringResource(R.string.composer_placeholder_queue)
        else -> stringResource(R.string.composer_placeholder)
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding(),
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            if (state.attachments.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.attachments.take(6).forEach { att ->
                        Box(modifier = Modifier.size(64.dp)) {
                            AsyncImage(
                                model = att.uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                            )
                            IconButton(
                                onClick = { onRemoveAttachment(att.uri) },
                                modifier = Modifier
                                    .size(22.dp)
                                    .align(Alignment.TopEnd),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = stringResource(R.string.attachment_remove),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .padding(2.dp),
                                )
                            }
                        }
                    }
                }
            }
            if (isResponding) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.composer_responding_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = onStop,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.stop_turn_button),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = {
                        pickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = isConnected,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_image),
                        contentDescription = stringResource(R.string.composer_add_image),
                        tint = if (isConnected) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    )
                }
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .heightIn(min = 44.dp, max = 200.dp)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (state.messageText.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                        BasicTextField(
                            value = state.messageText,
                            onValueChange = onMessageChange,
                            textStyle = LocalTextStyle.current.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                            maxLines = 8,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                FilledIconButton(
                    onClick = onSend,
                    enabled = canSend,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp),
                    colors = if (isResponding) {
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        )
                    } else {
                        IconButtonDefaults.filledIconButtonColors()
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_send),
                        contentDescription = if (isResponding) {
                            stringResource(R.string.composer_queue_button)
                        } else {
                            stringResource(R.string.send_button)
                        },
                    )
                }
            }
        }
    }
}
