package com.willdeep.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.annotation.StringRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.willdeep.android.BuildConfig
import com.willdeep.android.MobileAttentionActionRequest
import com.willdeep.android.R
import com.willdeep.android.mobile.GatewayFile
import com.willdeep.android.mobile.GatewayJob
import com.willdeep.android.mobile.GatewayMessage
import com.willdeep.android.mobile.GatewayMessageActivity
import com.willdeep.android.mobile.GatewayQueuedMessage
import com.willdeep.android.mobile.GatewaySession
import com.willdeep.android.mobile.GatewayWorktree
import com.willdeep.android.mobile.GatewayWorktreeFile
import com.willdeep.android.mobile.PatchProposal
import com.willdeep.android.mobile.PendingToolApproval
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WillDeepApp(
    viewModel: MobileGatewayViewModel = viewModel(),
    sharedMessageText: String = "",
    sharedMessageVersion: Int = 0,
    attentionAction: MobileAttentionActionRequest? = null,
    attentionActionVersion: Int = 0,
    pairingPayloadText: String = "",
    pairingPayloadVersion: Int = 0,
) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var screen by rememberSaveable { mutableStateOf("home") }

    LaunchedEffect(sharedMessageVersion) {
        viewModel.importSharedMessage(sharedMessageText)
        if (sharedMessageVersion > 0 && sharedMessageText.isNotBlank()) {
            screen = "session"
        }
    }
    LaunchedEffect(attentionActionVersion) {
        val request = attentionAction ?: return@LaunchedEffect
        viewModel.handleAttentionAction(request)
        if (attentionActionVersion > 0) {
            screen = "session"
        }
    }
    LaunchedEffect(pairingPayloadVersion) {
        if (pairingPayloadVersion > 0 && pairingPayloadText.isNotBlank()) {
            viewModel.scanAndPair(pairingPayloadText)
            screen = "home"
        }
    }
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.resumeConnectionIfAppropriate()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    when (screen) {
        "scanner" -> ScannerScreen(
            onBack = { screen = "home" },
            onPayloadScanned = { payload ->
                viewModel.scanAndPair(payload)
                screen = "home"
            },
        )
        "session" -> SessionDetailScreen(
            viewModel = viewModel,
            state = state,
            onBack = { screen = "home" },
            onScan = { screen = "scanner" },
        )
        else -> HomeScreen(
            state = state,
            versionName = BuildConfig.VERSION_NAME,
            onScanClick = { screen = "scanner" },
            onCreateSession = { viewModel.openWorkspacePicker() },
            onSelectSession = { id ->
                viewModel.selectSession(id)
                screen = "session"
            },
            onConnect = viewModel::connect,
            onDisconnect = viewModel::disconnect,
            onForget = viewModel::forgetToken,
            onWorkspacePickerDismiss = viewModel::closeWorkspacePicker,
            onWorkspacePickerRetry = viewModel::requestWorkspaces,
            onWorkspaceSelected = { path ->
                if (viewModel.createSession(path)) {
                    screen = "session"
                }
            },
        )
    }
}

@Composable
private fun SessionCard(
    state: MobileGatewayUiState,
    onRefresh: () -> Unit,
    onCreate: () -> Unit,
    onSelect: (String) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(stringResource(R.string.section_sessions), modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onRefresh, enabled = state.status == ConnectionStatus.Connected) {
                    Text(stringResource(R.string.refresh_sessions_button))
                }
            }
            Button(onClick = onCreate, enabled = state.status == ConnectionStatus.Connected) {
                Text(stringResource(R.string.new_session_button))
            }
            if (state.sessions.isEmpty()) {
                Text(
                    text = stringResource(R.string.session_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.sessions.forEach { session ->
                        SessionRow(
                            session = session,
                            selected = session.id == state.selectedSessionId,
                            onClick = { onSelect(session.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: GatewaySession, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = session.title.ifBlank { session.workspaceName.ifBlank { session.id.take(8) } },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = session.workspaceName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (selected) {
                    AssistChip(onClick = onClick, label = { Text(stringResource(R.string.selected_session)) })
                }
                if (session.isResponding) {
                    AssistChip(onClick = onClick, label = { Text(stringResource(R.string.responding)) })
                }
                AssistChip(
                    onClick = onClick,
                    label = { Text(stringResource(R.string.message_count, session.messageCount)) },
                )
            }
        }
    }
}

@Composable
internal fun ConversationCard(state: MobileGatewayUiState) {
    val displayMessages = state.conversationMessages.filter { it.hasDisplayableBody() }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(stringResource(R.string.section_conversation))
            if (displayMessages.isEmpty()) {
                Text(
                    text = stringResource(R.string.conversation_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            } else {
                displayMessages.takeLast(12).forEach { message ->
                    ConversationMessageRow(message)
                }
            }
        }
    }
}

internal fun GatewayMessage.hasDisplayableBody(): Boolean {
    return content.isNotBlank() || imageUrls.isNotEmpty() || rawContentPreview.isNotBlank()
}

@Composable
internal fun ConversationMessageRow(message: GatewayMessage) {
    val roleLabel = when (message.role) {
        "user" -> stringResource(R.string.message_role_user)
        "assistant" -> stringResource(R.string.message_role_assistant)
        "system" -> stringResource(R.string.message_role_system)
        else -> stringResource(R.string.message_role_other)
    }
    val containerColor = when (message.role) {
        "user" -> MaterialTheme.colorScheme.primaryContainer
        "assistant" -> MaterialTheme.colorScheme.surfaceVariant
        "system" -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = roleLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (message.isStreaming) {
                AssistChip(
                    onClick = {},
                    label = { Text(stringResource(R.string.message_streaming)) },
                )
            }
            if (message.imageUrls.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    message.imageUrls.take(6).forEach { url ->
                        coil.compose.AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(MaterialTheme.shapes.small),
                        )
                    }
                }
            }
            if (message.content.isNotBlank()) {
                MarkdownText(
                    text = message.content,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else if (message.imageUrls.isEmpty()) {
                val placeholder = when {
                    message.activity == GatewayMessageActivity.Thinking -> R.string.message_thinking_content
                    message.activity == GatewayMessageActivity.Tool -> R.string.message_tool_activity_content
                    message.isStreaming -> R.string.message_waiting_visible_content
                    else -> R.string.message_empty_content
                }
                Text(
                    text = stringResource(placeholder),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                if (message.rawContentPreview.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.message_raw_body_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                            Text(
                                text = message.rawContentPreview,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun WorktreeCard(
    state: MobileGatewayUiState,
    onReadFile: (GatewayWorktreeFile, String?) -> Unit,
) {
    val worktree = state.worktree ?: return
    if (worktree.fileCount == 0 && worktree.files.isEmpty()) {
        return
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(stringResource(R.string.section_worktree))
            WorktreeSummary(worktree)
            worktree.files.take(8).forEach { file ->
                WorktreeFileRow(
                    file = file,
                    isConnected = state.status == ConnectionStatus.Connected,
                    onReadFile = { onReadFile(file, worktree.sessionId ?: state.selectedSessionId) },
                )
            }
        }
    }
}

@Composable
private fun WorktreeSummary(worktree: GatewayWorktree) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(
            onClick = {},
            label = { Text(stringResource(R.string.worktree_file_count, worktree.fileCount)) },
        )
        AssistChip(
            onClick = {},
            label = { Text(stringResource(R.string.worktree_added_lines, worktree.totalAddedLines)) },
        )
        AssistChip(
            onClick = {},
            label = { Text(stringResource(R.string.worktree_deleted_lines, worktree.totalDeletedLines)) },
        )
    }
    if (!worktree.repositoryRoot.isNullOrBlank()) {
        Text(
            text = worktree.repositoryRoot,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WorktreeFileRow(
    file: GatewayWorktreeFile,
    isConnected: Boolean,
    onReadFile: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AssistChip(onClick = {}, label = { Text(file.kind.ifBlank { stringResource(R.string.worktree_unknown_kind) }) })
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = file.path,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.worktree_file_stats, file.addedLines, file.deletedLines),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            TextButton(
                onClick = onReadFile,
                enabled = isConnected && file.path.isNotBlank(),
            ) {
                Text(stringResource(R.string.read_changed_file_button))
            }
        }
    }
}

@Composable
private fun FilesCard(
    state: MobileGatewayUiState,
    onPathChange: (String) -> Unit,
    onReadFile: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(stringResource(R.string.section_files))
            OutlinedTextField(
                value = state.filePathText,
                onValueChange = onPathChange,
                label = { Text(stringResource(R.string.file_path_label)) },
                placeholder = { Text(stringResource(R.string.file_path_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onReadFile,
                enabled = state.status == ConnectionStatus.Connected && state.filePathText.isNotBlank(),
            ) {
                Text(stringResource(R.string.read_file_button))
            }
            state.loadedFile?.let { file ->
                FilePreview(file)
            }
        }
    }
}

@Composable
private fun FilePreview(file: GatewayFile) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = file.path,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(stringResource(R.string.file_bytes, file.byteCount)) },
                )
                if (file.truncated) {
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.file_truncated)) },
                    )
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = file.content,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }
    }
}

@Composable
internal fun ApprovalCard(
    state: MobileGatewayUiState,
    onToolDecision: (PendingToolApproval, Boolean) -> Unit,
    onToolAnswerChange: (String, String) -> Unit,
    onToolConfirmationChange: (String, String) -> Unit,
    onPatchDecision: (PatchProposal, Boolean) -> Unit,
) {
    if (state.pendingTools.isEmpty() && state.patchProposals.isEmpty()) {
        return
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionTitle(stringResource(R.string.section_approvals))
            state.pendingTools.forEach { approval ->
                ToolApprovalRow(
                    approval = approval,
                    answer = state.toolAnswers[approval.id].orEmpty(),
                    confirmation = state.toolConfirmations[approval.id].orEmpty(),
                    onAnswerChange = { onToolAnswerChange(approval.id, it) },
                    onConfirmationChange = { onToolConfirmationChange(approval.id, it) },
                    onApprove = { onToolDecision(approval, true) },
                    onReject = { onToolDecision(approval, false) },
                )
            }
            state.patchProposals.forEach { proposal ->
                PatchProposalRow(
                    proposal = proposal,
                    onApprove = { onPatchDecision(proposal, true) },
                    onReject = { onPatchDecision(proposal, false) },
                )
            }
        }
    }
}

@Composable
private fun ToolApprovalRow(
    approval: PendingToolApproval,
    answer: String,
    confirmation: String,
    onAnswerChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.tool_approval_title, approval.title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (approval.summary.isNotBlank()) {
                Text(
                    text = approval.summary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val previewLines = approval.humanPreviewLines()
            if (previewLines.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    previewLines.forEach { line ->
                        ApprovalPreviewItemRow(line)
                    }
                }
            } else if (approval.inputPreview.isNotBlank() && !approval.inputPreview.isJsonLike()) {
                Text(
                    text = stringResource(R.string.approval_preview, approval.inputPreview),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (approval.requiresAnswer) {
                OutlinedTextField(
                    value = answer,
                    onValueChange = onAnswerChange,
                    label = { Text(stringResource(R.string.tool_answer_label)) },
                    placeholder = { Text(stringResource(R.string.tool_answer_placeholder)) },
                    minLines = 1,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (approval.requiresConfirmation) {
                Text(
                    text = stringResource(R.string.tool_confirmation_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = onConfirmationChange,
                    label = { Text(stringResource(R.string.tool_confirmation_label)) },
                    placeholder = { Text(stringResource(R.string.tool_confirmation_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            DecisionButtons(
                approveEnabled = (!approval.requiresAnswer || answer.isNotBlank()) &&
                    (!approval.requiresConfirmation || confirmation.trim().equals("confirm", ignoreCase = true)),
                onApprove = onApprove,
                onReject = onReject,
            )
        }
    }
}

@Composable
private fun ApprovalPreviewItemRow(line: ApprovalPreviewItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(line.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = line.value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

private data class ApprovalPreviewItem(
    @StringRes val labelRes: Int,
    val value: String,
)

private fun PendingToolApproval.humanPreviewLines(): List<ApprovalPreviewItem> {
    val outer = inputPreview.parseJsonObject() ?: return emptyList()
    val payload = outer.optString("input_json").parseJsonObject()
        ?: outer.optJSONObject("input")
        ?: outer
    val lines = mutableListOf<ApprovalPreviewItem>()
    payload.firstString("model").takeIf { it.isNotBlank() }?.let {
        lines += ApprovalPreviewItem(R.string.approval_detail_model, it)
    }
    payload.firstString("prompt", "input", "text").takeIf { it.isNotBlank() }?.let {
        lines += ApprovalPreviewItem(R.string.approval_detail_prompt, it)
    }
    payload.firstString("n", "count", "quantity").takeIf { it.isNotBlank() }?.let {
        lines += ApprovalPreviewItem(R.string.approval_detail_count, it)
    }
    payload.firstString("size", "resolution").takeIf { it.isNotBlank() }?.let {
        lines += ApprovalPreviewItem(R.string.approval_detail_size, it)
    }
    payload.firstString("quality").takeIf { it.isNotBlank() }?.let {
        lines += ApprovalPreviewItem(R.string.approval_detail_quality, it)
    }
    outer.firstString("endpoint_suffix", "endpoint").takeIf { it.isNotBlank() }?.let {
        lines += ApprovalPreviewItem(R.string.approval_detail_endpoint, it)
    }
    return lines
}

private fun String.parseJsonObject(): JSONObject? {
    val trimmed = trim()
    if (!trimmed.startsWith("{")) return null
    return runCatching { JSONObject(trimmed) }.getOrNull()
}

private fun String.isJsonLike(): Boolean {
    val trimmed = trim()
    return trimmed.startsWith("{") || trimmed.startsWith("[")
}

private fun JSONObject.firstString(vararg keys: String): String {
    return keys.firstNotNullOfOrNull { key ->
        when {
            !has(key) -> null
            opt(key) == JSONObject.NULL -> null
            else -> opt(key)?.toString()?.trim()?.takeIf(String::isNotBlank)
        }
    }.orEmpty()
}

@Composable
private fun PatchProposalRow(
    proposal: PatchProposal,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.patch_proposal_title, proposal.title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (proposal.path.isNotBlank()) {
                Text(
                    text = proposal.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (proposal.summary.isNotBlank()) {
                Text(
                    text = proposal.summary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (proposal.stats.isNotBlank()) {
                Text(
                    text = stringResource(R.string.patch_stats, proposal.stats),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            DecisionButtons(onApprove = onApprove, onReject = onReject)
        }
    }
}

@Composable
private fun DecisionButtons(
    approveEnabled: Boolean = true,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Button(
            onClick = onApprove,
            enabled = approveEnabled,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(stringResource(R.string.approve_button))
        }
        OutlinedButton(
            onClick = onReject,
            modifier = Modifier.height(42.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(stringResource(R.string.reject_button))
        }
    }
}

@Composable
private fun JobsCard(
    state: MobileGatewayUiState,
    onKillJob: (GatewayJob) -> Unit,
) {
    if (state.jobs.isEmpty()) {
        return
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(stringResource(R.string.section_jobs))
            state.jobs.forEach { job ->
                JobRow(job = job, onKill = { onKillJob(job) })
            }
        }
    }
}

@Composable
private fun JobRow(job: GatewayJob, onKill: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = job.handle.ifBlank { job.id.take(8) },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AssistChip(
                    onClick = {},
                    label = { Text(job.status) },
                )
            }
            if (job.command.isNotBlank()) {
                Text(
                    text = job.command,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(stringResource(R.string.job_pid, job.pid)) },
                )
                AssistChip(
                    onClick = {},
                    label = { Text(stringResource(R.string.job_output_bytes, job.outputByteCount)) },
                )
                if (job.exitCode != null) {
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.job_exit_code, job.exitCode)) },
                    )
                }
            }
            if (job.isAlive) {
                OutlinedButton(onClick = onKill) {
                    Text(stringResource(R.string.kill_job_button))
                }
            }
        }
    }
}

@Composable
internal fun QueueCard(
    state: MobileGatewayUiState,
    onSendNow: (GatewayQueuedMessage) -> Unit,
    onRemove: (GatewayQueuedMessage) -> Unit,
    onClear: () -> Unit,
) {
    if (state.queuedMessages.isEmpty()) {
        return
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(stringResource(R.string.section_queue), modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = onClear,
                    enabled = state.status == ConnectionStatus.Connected,
                ) {
                    Text(stringResource(R.string.clear_queue_button))
                }
            }
            state.queuedMessages.forEach { message ->
                QueuedMessageRow(
                    message = message,
                    connected = state.status == ConnectionStatus.Connected,
                    onSendNow = { onSendNow(message) },
                    onRemove = { onRemove(message) },
                )
            }
        }
    }
}

@Composable
private fun QueuedMessageRow(
    message: GatewayQueuedMessage,
    connected: Boolean,
    onSendNow: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = message.textPreview.ifBlank { message.id.take(8) },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (message.imageCount > 0) {
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.queue_images, message.imageCount)) },
                    )
                }
                if (message.textAttachmentCount > 0) {
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.queue_attachments, message.textAttachmentCount)) },
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSendNow, enabled = connected) {
                    Text(stringResource(R.string.send_now_button))
                }
                OutlinedButton(onClick = onRemove, enabled = connected) {
                    Text(stringResource(R.string.remove_queue_button))
                }
            }
        }
    }
}

@Composable
private fun EventLogCard(state: MobileGatewayUiState) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionTitle(stringResource(R.string.section_connection))
            if (state.isPaired) {
                Text(
                    text = stringResource(R.string.token_stored),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            SectionTitle(stringResource(R.string.section_recent_commands))
            if (state.commandStatuses.isEmpty()) {
                Text(
                    text = stringResource(R.string.command_status_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            } else {
                state.commandStatuses.takeLast(6).forEach { command ->
                    CommandStatusRow(command)
                }
            }
            SectionTitle(stringResource(R.string.section_event_log))
            state.logLines.takeLast(12).forEach { line ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = when (line.kind) {
                            "mobile" -> stringResource(R.string.log_mobile, line.text)
                            "mac" -> stringResource(R.string.log_mac, line.text)
                            "ack" -> stringResource(R.string.log_ack, line.text)
                            "error" -> stringResource(R.string.log_error, line.text)
                            else -> line.text
                        },
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun CommandStatusRow(command: MobileCommandStatus) {
    val statusText = when (command.state) {
        MobileCommandState.Pending -> stringResource(R.string.command_status_pending)
        MobileCommandState.Accepted -> stringResource(R.string.command_status_accepted)
        MobileCommandState.Failed -> stringResource(R.string.command_status_failed)
    }
    Surface(
        color = when (command.state) {
            MobileCommandState.Pending -> MaterialTheme.colorScheme.secondaryContainer
            MobileCommandState.Accepted -> MaterialTheme.colorScheme.primaryContainer
            MobileCommandState.Failed -> MaterialTheme.colorScheme.errorContainer
        },
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.command_status_line, command.type, statusText),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (command.detail.isNotBlank()) {
                Text(
                    text = command.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
internal fun StatusLine(state: MobileGatewayUiState) {
    val text = when (state.status) {
        ConnectionStatus.Idle -> stringResource(R.string.status_idle)
        ConnectionStatus.Pairing -> stringResource(R.string.status_pairing)
        ConnectionStatus.Connecting -> stringResource(R.string.status_connecting)
        ConnectionStatus.Reconnecting -> stringResource(
            R.string.status_reconnecting,
            state.reconnectAttempt,
            (state.reconnectDelayMillis / 1000L).coerceAtLeast(1L),
        )
        ConnectionStatus.Connected -> stringResource(R.string.status_connected)
        ConnectionStatus.Disconnected -> stringResource(R.string.status_disconnected)
        ConnectionStatus.Error -> stringResource(R.string.status_error, state.errorMessage.orEmpty())
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (state.status == ConnectionStatus.Error) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        },
    )
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}
