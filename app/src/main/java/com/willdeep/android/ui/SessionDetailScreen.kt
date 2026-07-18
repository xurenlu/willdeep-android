package com.willdeep.android.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

private enum class SessionDetailTab { Conversation, Actions, Changes }

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
        jobs = state.jobs.filter { it.sessionId == sessionId },
        queuedMessages = state.queuedMessages.filter { it.sessionId == sessionId },
        conversationMessages = state.conversationMessages.filter { it.sessionId == sessionId },
        worktree = state.worktree?.takeIf { it.sessionId == null || it.sessionId == sessionId },
    )
    val isResponding = activeSession?.isResponding == true
    var selectedTab by remember { mutableStateOf(SessionDetailTab.Conversation) }
    val listState = rememberLazyListState()
    val conversationMessages = scopedState.conversationMessages.filter { it.hasDisplayableBody() }
    val visibleConversationMessages = conversationMessages.takeLast(24)
    val lastMessage = conversationMessages.lastOrNull()
    val activityItemCount = if (
        isResponding || scopedState.pendingTools.isNotEmpty() || scopedState.jobs.any { it.isAlive }
    ) 1 else 0
    val messageItemCount = visibleConversationMessages.size.coerceAtLeast(1)
    val bottomAnchorIndex = activityItemCount + messageItemCount

    LaunchedEffect(
        sessionId,
        conversationMessages.size,
        lastMessage?.id,
        lastMessage?.content?.length,
        lastMessage?.isStreaming,
        selectedTab,
    ) {
        if (selectedTab == SessionDetailTab.Conversation) {
            listState.animateScrollToItem(bottomAnchorIndex)
        }
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
                                text = if (state.status == ConnectionStatus.Connected) {
                                    stringResource(
                                        R.string.session_detail_mac_received,
                                        state.desktopResponseAgeMillis.coerceAtLeast(0L) / 1_000L,
                                        subtitleText,
                                    )
                                } else {
                                    subtitleText
                                },
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
            if (selectedTab == SessionDetailTab.Conversation) {
                MessageInputBar(
                state = scopedState,
                isResponding = isResponding,
                onMessageChange = viewModel::updateMessage,
                onSend = viewModel::sendMessage,
                onStop = viewModel::stopTurn,
                onAddAttachment = viewModel::addAttachment,
                onRemoveAttachment = viewModel::removeAttachment,
                onApprovalModeChange = viewModel::updateApprovalMode,
                onProviderSelected = viewModel::selectProvider,
                onModelSelected = viewModel::selectModel,
                onSkillToggle = viewModel::toggleSkill,
                onExpertToggle = viewModel::toggleExpert,
                onPluginToggle = viewModel::togglePlugin,
                onToolDecision = viewModel::decideTool,
                onToolAnswerChange = viewModel::updateToolAnswer,
                onToolConfirmationChange = viewModel::updateToolConfirmation,
                onPatchDecision = viewModel::decidePatch,
                onSendQueuedNow = viewModel::sendQueuedNow,
                onRemoveQueued = viewModel::removeQueuedMessage,
                onClearQueue = viewModel::clearQueue,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SessionDetailTabs(
                selected = selectedTab,
                onSelected = { selectedTab = it },
            )
            when (selectedTab) {
                SessionDetailTab.Conversation -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (isResponding || scopedState.pendingTools.isNotEmpty() || scopedState.jobs.any { it.isAlive }) {
                        item(key = "activity") {
                            SessionActivitySummary(
                                responding = isResponding,
                                actionCount = scopedState.pendingTools.size + scopedState.patchProposals.size,
                            )
                        }
                    }
                    if (conversationMessages.isEmpty()) {
                        item {
                            EmptyDetailState(R.string.conversation_empty)
                        }
                    } else {
                        items(visibleConversationMessages, key = { it.id }) { message ->
                            ConversationMessageRow(message)
                        }
                    }
                    item { Spacer(Modifier.height(1.dp)) }
                }
                SessionDetailTab.Actions -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (
                        scopedState.pendingTools.isEmpty() &&
                        scopedState.patchProposals.isEmpty() &&
                        scopedState.jobs.isEmpty() &&
                        scopedState.queuedMessages.isEmpty()
                    ) {
                        item { EmptyDetailState(R.string.session_detail_actions_empty) }
                    } else {
                        item {
                            ApprovalCard(
                                state = scopedState,
                                onToolDecision = viewModel::decideTool,
                                onToolAnswerChange = viewModel::updateToolAnswer,
                                onToolConfirmationChange = viewModel::updateToolConfirmation,
                                onPatchDecision = viewModel::decidePatch,
                            )
                        }
                        item { JobsCard(scopedState, viewModel::killJob) }
                        item {
                            QueueCard(
                                state = scopedState,
                                onSendNow = viewModel::sendQueuedNow,
                                onRemove = viewModel::removeQueuedMessage,
                                onClear = viewModel::clearQueue,
                            )
                        }
                    }
                }
                SessionDetailTab.Changes -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (scopedState.worktree == null || scopedState.worktree.fileCount == 0) {
                        item { EmptyDetailState(R.string.session_detail_changes_empty) }
                    } else {
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
    }
}

@Composable
private fun SessionDetailTabs(
    selected: SessionDetailTab,
    onSelected: (SessionDetailTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SessionDetailTab.entries.forEach { tab ->
            FilterChip(
                selected = selected == tab,
                onClick = { onSelected(tab) },
                label = { Text(stringResource(tab.labelResource())) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SessionActivitySummary(responding: Boolean, actionCount: Int) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF3F6EF),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (responding) Color(0xFF2F9E44) else MaterialTheme.colorScheme.primary),
            )
            Text(
                text = if (responding) {
                    stringResource(R.string.session_detail_activity_running)
                } else {
                    stringResource(R.string.session_detail_activity_actions, actionCount)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun EmptyDetailState(textResource: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(textResource),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(14.dp),
        )
    }
}

private fun SessionDetailTab.labelResource(): Int = when (this) {
    SessionDetailTab.Conversation -> R.string.session_detail_tab_conversation
    SessionDetailTab.Actions -> R.string.session_detail_tab_actions
    SessionDetailTab.Changes -> R.string.session_detail_tab_changes
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
    onApprovalModeChange: (ApprovalMode) -> Unit,
    onProviderSelected: (String) -> Unit,
    onModelSelected: (String) -> Unit,
    onSkillToggle: (String) -> Unit,
    onExpertToggle: (String) -> Unit,
    onPluginToggle: (String) -> Unit,
    onToolDecision: (com.willdeep.android.mobile.PendingToolApproval, Boolean) -> Unit,
    onToolAnswerChange: (String, String) -> Unit,
    onToolConfirmationChange: (String, String) -> Unit,
    onPatchDecision: (com.willdeep.android.mobile.PatchProposal, Boolean) -> Unit,
    onSendQueuedNow: (com.willdeep.android.mobile.GatewayQueuedMessage) -> Unit,
    onRemoveQueued: (com.willdeep.android.mobile.GatewayQueuedMessage) -> Unit,
    onClearQueue: () -> Unit,
) {
    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
    ) { uris -> uris.forEach { onAddAttachment(it) } }

    val isConnected = state.status == ConnectionStatus.Connected
    val canSend = isConnected && (state.messageText.isNotBlank() || state.attachments.isNotEmpty())
    val placeholder = when {
        state.status == ConnectionStatus.AwaitingDesktop -> {
            stringResource(R.string.composer_placeholder_waiting_mac)
        }
        !isConnected -> stringResource(R.string.composer_placeholder_offline)
        isResponding -> stringResource(R.string.composer_placeholder_queue)
        else -> stringResource(R.string.composer_placeholder)
    }
    var activePicker by remember { mutableStateOf<CapabilityPickerKind?>(null) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
        ) {
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
                }
            }
            RunControlStrip(
                state = state,
                onOpenPicker = { activePicker = it },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
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
                        painter = painterResource(R.drawable.ic_add),
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
                            .heightIn(min = 112.dp, max = 180.dp)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.TopStart,
                    ) {
                        if (state.messageText.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.align(Alignment.TopStart),
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
                            maxLines = 5,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth(),
                        )
                    }
                }
                FilledIconButton(
                    onClick = {
                        if (isResponding) onStop() else onSend()
                    },
                    enabled = isResponding || canSend,
                    shape = CircleShape,
                    modifier = Modifier.size(46.dp),
                    colors = if (isResponding) {
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFFD94335),
                            contentColor = Color.White,
                        )
                    } else {
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.outline,
                        )
                    },
                ) {
                    Icon(
                        painter = painterResource(
                            if (isResponding) R.drawable.ic_stop_square else R.drawable.ic_send
                        ),
                        contentDescription = if (isResponding) {
                            stringResource(R.string.stop_turn_button)
                        } else {
                            stringResource(R.string.send_button)
                        },
                        modifier = Modifier.size(if (isResponding) 18.dp else 22.dp),
                    )
                }
            }
            ComposerReviewDock(
                state = state,
                onToolDecision = onToolDecision,
                onToolAnswerChange = onToolAnswerChange,
                onToolConfirmationChange = onToolConfirmationChange,
                onPatchDecision = onPatchDecision,
                onSendQueuedNow = onSendQueuedNow,
                onRemoveQueued = onRemoveQueued,
                onClearQueue = onClearQueue,
            )
        }
    }
    CapabilityPickerSheet(
        kind = activePicker,
        state = state,
        onDismiss = { activePicker = null },
        onApprovalModeChange = onApprovalModeChange,
        onProviderSelected = onProviderSelected,
        onModelSelected = onModelSelected,
        onSkillToggle = onSkillToggle,
        onExpertToggle = onExpertToggle,
        onPluginToggle = onPluginToggle,
    )
}

private enum class CapabilityPickerKind {
    Approval,
    Provider,
    Model,
    Skills,
    Experts,
    Plugins,
}

@Composable
private fun RunControlStrip(
    state: MobileGatewayUiState,
    onOpenPicker: (CapabilityPickerKind) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item {
            ControlIconButton(
                iconRes = R.drawable.ic_approval,
                contentDescription = stringResource(state.approvalMode.labelRes()),
                active = true,
                onClick = { onOpenPicker(CapabilityPickerKind.Approval) },
            )
        }
        item {
            ControlIconButton(
                iconRes = R.drawable.ic_provider,
                contentDescription = stringResource(R.string.composer_provider_title),
                active = state.selectedProviderId.isNotBlank(),
                onClick = { onOpenPicker(CapabilityPickerKind.Provider) },
            )
        }
        item {
            ControlIconButton(
                iconRes = R.drawable.ic_model,
                contentDescription = stringResource(R.string.composer_model_title),
                active = state.selectedModelId.isNotBlank(),
                onClick = { onOpenPicker(CapabilityPickerKind.Model) },
            )
        }
        item {
            ControlIconButton(
                iconRes = R.drawable.ic_skill,
                contentDescription = stringResource(R.string.composer_skills_title),
                active = state.selectedSkillIds.isNotEmpty(),
                badgeText = state.selectedSkillIds.countBadge(),
                onClick = { onOpenPicker(CapabilityPickerKind.Skills) },
            )
        }
        item {
            ControlIconButton(
                iconRes = R.drawable.ic_expert,
                contentDescription = stringResource(R.string.composer_expert_title),
                active = state.selectedExpertIds.isNotEmpty(),
                badgeText = state.selectedExpertIds.countBadge(),
                onClick = { onOpenPicker(CapabilityPickerKind.Experts) },
            )
        }
        item {
            ControlIconButton(
                iconRes = R.drawable.ic_plugin,
                contentDescription = stringResource(R.string.composer_plugins_title),
                active = state.selectedPluginIds.isNotEmpty(),
                badgeText = state.selectedPluginIds.countBadge(),
                onClick = { onOpenPicker(CapabilityPickerKind.Plugins) },
            )
        }
    }
}

@Composable
private fun ControlIconButton(
    iconRes: Int,
    contentDescription: String,
    active: Boolean,
    badgeText: String? = null,
    onClick: () -> Unit,
) {
    Box(modifier = Modifier.size(40.dp)) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (active) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
            },
            modifier = Modifier
                .size(38.dp)
                .align(Alignment.Center),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = contentDescription,
                    tint = if (active) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (active) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        badgeText?.let { text ->
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CapabilityPickerSheet(
    kind: CapabilityPickerKind?,
    state: MobileGatewayUiState,
    onDismiss: () -> Unit,
    onApprovalModeChange: (ApprovalMode) -> Unit,
    onProviderSelected: (String) -> Unit,
    onModelSelected: (String) -> Unit,
    onSkillToggle: (String) -> Unit,
    onExpertToggle: (String) -> Unit,
    onPluginToggle: (String) -> Unit,
) {
    if (kind == null) return
    val approvalOptions = ApprovalMode.entries.map { mode ->
        MobileCapabilityOption(id = mode.name, title = stringResource(mode.labelRes()))
    }
    val title = when (kind) {
        CapabilityPickerKind.Approval -> stringResource(R.string.composer_controls_title)
        CapabilityPickerKind.Provider -> stringResource(R.string.composer_provider_title)
        CapabilityPickerKind.Model -> stringResource(R.string.composer_model_title)
        CapabilityPickerKind.Skills -> stringResource(R.string.composer_skills_title)
        CapabilityPickerKind.Experts -> stringResource(R.string.composer_expert_title)
        CapabilityPickerKind.Plugins -> stringResource(R.string.composer_plugins_title)
    }
    val options = when (kind) {
        CapabilityPickerKind.Approval -> approvalOptions
        CapabilityPickerKind.Provider -> state.providerOptions
        CapabilityPickerKind.Model -> state.modelOptions
        CapabilityPickerKind.Skills -> state.skillOptions
        CapabilityPickerKind.Experts -> state.expertOptions
        CapabilityPickerKind.Plugins -> state.pluginOptions
    }
    val selectedIds = when (kind) {
        CapabilityPickerKind.Approval -> setOf(state.approvalMode.name)
        CapabilityPickerKind.Provider -> setOf(state.selectedProviderId)
        CapabilityPickerKind.Model -> setOf(state.selectedModelId)
        CapabilityPickerKind.Skills -> state.selectedSkillIds
        CapabilityPickerKind.Experts -> state.selectedExpertIds
        CapabilityPickerKind.Plugins -> state.selectedPluginIds
    }
    val multiSelect = kind == CapabilityPickerKind.Skills ||
        kind == CapabilityPickerKind.Experts ||
        kind == CapabilityPickerKind.Plugins
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 6.dp,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.done_button))
                }
            }
            if (options.isEmpty()) {
                Text(
                    text = stringResource(R.string.composer_capability_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 6.dp),
                    modifier = Modifier.heightIn(max = 430.dp),
                ) {
                    items(options, key = { it.id }) { option ->
                        CapabilityOptionRow(
                            option = option,
                            selected = option.id in selectedIds,
                            multiSelect = multiSelect,
                            onClick = {
                                when (kind) {
                                    CapabilityPickerKind.Approval -> {
                                        ApprovalMode.entries.firstOrNull { it.name == option.id }
                                            ?.let(onApprovalModeChange)
                                        onDismiss()
                                    }
                                    CapabilityPickerKind.Provider -> {
                                        onProviderSelected(option.id)
                                        onDismiss()
                                    }
                                    CapabilityPickerKind.Model -> {
                                        onModelSelected(option.id)
                                        onDismiss()
                                    }
                                    CapabilityPickerKind.Skills -> onSkillToggle(option.id)
                                    CapabilityPickerKind.Experts -> onExpertToggle(option.id)
                                    CapabilityPickerKind.Plugins -> onPluginToggle(option.id)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CapabilityOptionRow(
    option: MobileCapabilityOption,
    selected: Boolean,
    multiSelect: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                option.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (multiSelect) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = null,
                )
            } else {
                RadioButton(
                    selected = selected,
                    onClick = null,
                )
            }
        }
    }
}

@Composable
private fun ComposerReviewDock(
    state: MobileGatewayUiState,
    onToolDecision: (com.willdeep.android.mobile.PendingToolApproval, Boolean) -> Unit,
    onToolAnswerChange: (String, String) -> Unit,
    onToolConfirmationChange: (String, String) -> Unit,
    onPatchDecision: (com.willdeep.android.mobile.PatchProposal, Boolean) -> Unit,
    onSendQueuedNow: (com.willdeep.android.mobile.GatewayQueuedMessage) -> Unit,
    onRemoveQueued: (com.willdeep.android.mobile.GatewayQueuedMessage) -> Unit,
    onClearQueue: () -> Unit,
) {
    val hasApprovals = state.pendingTools.isNotEmpty() || state.patchProposals.isNotEmpty()
    val hasQueue = state.queuedMessages.isNotEmpty()
    if (!hasApprovals && !hasQueue) {
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.composer_review_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
        if (hasApprovals) {
            ApprovalCard(
                state = state,
                onToolDecision = onToolDecision,
                onToolAnswerChange = onToolAnswerChange,
                onToolConfirmationChange = onToolConfirmationChange,
                onPatchDecision = onPatchDecision,
            )
        }
        if (hasQueue) {
            QueueCard(
                state = state,
                onSendNow = onSendQueuedNow,
                onRemove = onRemoveQueued,
                onClear = onClearQueue,
            )
        }
    }
}

private fun ApprovalMode.labelRes(): Int = when (this) {
    ApprovalMode.AskEveryTime -> R.string.approval_mode_ask_every_time
    ApprovalMode.SmartApproval -> R.string.approval_mode_smart
    ApprovalMode.WorkspaceWritable -> R.string.approval_mode_workspace_writable
}

private fun Set<String>.countBadge(): String? {
    return size.takeIf { it > 0 }?.coerceAtMost(9)?.toString()
}
