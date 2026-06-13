package com.willdeep.android.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.willdeep.android.R
import com.willdeep.android.mobile.GatewayFile
import com.willdeep.android.mobile.GatewayJob
import com.willdeep.android.mobile.GatewayMessage
import com.willdeep.android.mobile.GatewayQueuedMessage
import com.willdeep.android.mobile.GatewaySession
import com.willdeep.android.mobile.GatewayWorktree
import com.willdeep.android.mobile.GatewayWorktreeFile
import com.willdeep.android.mobile.PatchDiff
import com.willdeep.android.mobile.PatchProposal
import com.willdeep.android.mobile.PendingToolApproval

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WillDeepApp(viewModel: MobileGatewayViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var scannerVisible by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.screen_title))
                        Text(
                            text = stringResource(R.string.screen_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PairingCard(
                    state = state,
                    onPayloadChange = viewModel::updatePairingPayload,
                    onDeviceNameChange = viewModel::updateDeviceName,
                    onPair = viewModel::pair,
                    onScan = { scannerVisible = true },
                    onConnect = viewModel::connect,
                    onDisconnect = viewModel::disconnect,
                    onForget = viewModel::forgetToken,
                )
            }
            if (scannerVisible) {
                item {
                    PairingScannerCard(
                        onPayloadScanned = { payload ->
                            viewModel.loadPairingPayloadFromQr(payload)
                            scannerVisible = false
                        },
                        onClose = { scannerVisible = false },
                    )
                }
            }
            item {
                SessionCard(
                    state = state,
                    onRefresh = viewModel::refreshSessions,
                    onCreate = viewModel::createSession,
                    onSelect = viewModel::selectSession,
                )
            }
            item {
                ConversationCard(state)
            }
            item {
                WorktreeCard(state)
            }
            item {
                FilesCard(
                    state = state,
                    onPathChange = viewModel::updateFilePath,
                    onReadFile = viewModel::requestFileRead,
                )
            }
            item {
                ApprovalCard(
                    state = state,
                    onToolDecision = viewModel::decideTool,
                    onToolAnswerChange = viewModel::updateToolAnswer,
                    onPatchDiffRequest = viewModel::requestPatchDiff,
                    onPatchDecision = viewModel::decidePatch,
                )
            }
            item {
                JobsCard(
                    state = state,
                    onKillJob = viewModel::killJob,
                )
            }
            item {
                ComposerCard(
                    state = state,
                    onMessageChange = viewModel::updateMessage,
                    onSend = viewModel::sendMessage,
                    onQueue = viewModel::queueCurrentMessage,
                    onStop = viewModel::stopTurn,
                )
            }
            item {
                QueueCard(
                    state = state,
                    onSendNow = viewModel::sendQueuedNow,
                    onRemove = viewModel::removeQueuedMessage,
                    onClear = viewModel::clearQueue,
                )
            }
            item {
                EventLogCard(state)
            }
        }
    }
}

@Composable
private fun PairingCard(
    state: MobileGatewayUiState,
    onPayloadChange: (String) -> Unit,
    onDeviceNameChange: (String) -> Unit,
    onPair: () -> Unit,
    onScan: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onForget: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(stringResource(R.string.section_pairing))
            Text(
                text = if (state.isPaired && state.desktopName.isNotBlank()) {
                    stringResource(R.string.paired_to, state.desktopName)
                } else {
                    stringResource(R.string.not_paired)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            StatusLine(state)
            if (state.baseUrl.isNotBlank()) {
                Text(
                    text = stringResource(R.string.gateway_url, state.baseUrl),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (state.protocolVersion.isNotBlank()) {
                Text(
                    text = stringResource(R.string.protocol_version, state.protocolVersion),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedTextField(
                value = state.pairingPayloadText,
                onValueChange = onPayloadChange,
                label = { Text(stringResource(R.string.pairing_payload_label)) },
                placeholder = { Text(stringResource(R.string.pairing_payload_placeholder)) },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.deviceName,
                onValueChange = onDeviceNameChange,
                label = { Text(stringResource(R.string.device_name_label)) },
                placeholder = { Text(stringResource(R.string.device_name_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPair,
                    enabled = state.pairingPayloadText.isNotBlank() && state.status != ConnectionStatus.Pairing,
                ) {
                    Text(stringResource(R.string.pair_button))
                }
                OutlinedButton(onClick = onScan) {
                    Text(stringResource(R.string.scan_qr_button))
                }
                if (state.status == ConnectionStatus.Connected || state.status == ConnectionStatus.Reconnecting) {
                    OutlinedButton(onClick = onDisconnect) {
                        Text(stringResource(R.string.disconnect_button))
                    }
                } else {
                    OutlinedButton(onClick = onConnect, enabled = state.isPaired) {
                        Text(stringResource(R.string.connect_button))
                    }
                }
                TextButton(onClick = onForget, enabled = state.isPaired) {
                    Text(stringResource(R.string.forget_button))
                }
            }
            Text(
                text = stringResource(R.string.pairing_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun PairingScannerCard(
    onPayloadScanned: (String) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(stringResource(R.string.qr_scanner_title), modifier = Modifier.weight(1f))
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.close_scanner_button))
                }
            }
            Text(
                text = stringResource(R.string.qr_scanner_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            if (hasCameraPermission) {
                PairingQrScanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    onPayloadScanned = onPayloadScanned,
                )
            } else {
                Text(
                    text = stringResource(R.string.camera_permission_required),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                    Text(stringResource(R.string.camera_permission_button))
                }
            }
        }
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
private fun ConversationCard(state: MobileGatewayUiState) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(stringResource(R.string.section_conversation))
            if (state.conversationMessages.isEmpty()) {
                Text(
                    text = stringResource(R.string.conversation_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            } else {
                state.conversationMessages.takeLast(12).forEach { message ->
                    ConversationMessageRow(message)
                }
            }
        }
    }
}

@Composable
private fun ConversationMessageRow(message: GatewayMessage) {
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
            Text(
                text = message.content.ifBlank { stringResource(R.string.message_empty_content) },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun WorktreeCard(state: MobileGatewayUiState) {
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
                WorktreeFileRow(file)
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
private fun WorktreeFileRow(file: GatewayWorktreeFile) {
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
private fun ApprovalCard(
    state: MobileGatewayUiState,
    onToolDecision: (PendingToolApproval, Boolean) -> Unit,
    onToolAnswerChange: (String, String) -> Unit,
    onPatchDiffRequest: (PatchProposal) -> Unit,
    onPatchDecision: (PatchProposal, Boolean) -> Unit,
) {
    if (state.pendingTools.isEmpty() && state.patchProposals.isEmpty()) {
        return
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(stringResource(R.string.section_approvals))
            state.pendingTools.forEach { approval ->
                ToolApprovalRow(
                    approval = approval,
                    answer = state.toolAnswers[approval.id].orEmpty(),
                    onAnswerChange = { onToolAnswerChange(approval.id, it) },
                    onApprove = { onToolDecision(approval, true) },
                    onReject = { onToolDecision(approval, false) },
                )
            }
            state.patchProposals.forEach { proposal ->
                PatchProposalRow(
                    proposal = proposal,
                    diff = state.patchDiffs[proposal.id],
                    onViewDiff = { onPatchDiffRequest(proposal) },
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
    onAnswerChange: (String) -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
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
                text = stringResource(R.string.tool_approval_title, approval.title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (approval.summary.isNotBlank()) {
                Text(text = approval.summary, style = MaterialTheme.typography.bodySmall)
            }
            if (approval.inputPreview.isNotBlank()) {
                Text(
                    text = stringResource(R.string.approval_preview, approval.inputPreview),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (approval.requiresAnswer) {
                OutlinedTextField(
                    value = answer,
                    onValueChange = onAnswerChange,
                    label = { Text(stringResource(R.string.tool_answer_label)) },
                    placeholder = { Text(stringResource(R.string.tool_answer_placeholder)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            DecisionButtons(
                approveEnabled = !approval.requiresAnswer || answer.isNotBlank(),
                onApprove = onApprove,
                onReject = onReject,
            )
        }
    }
}

@Composable
private fun PatchProposalRow(
    proposal: PatchProposal,
    diff: PatchDiff?,
    onViewDiff: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
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
                text = stringResource(R.string.patch_proposal_title, proposal.title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (proposal.path.isNotBlank()) {
                Text(
                    text = proposal.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (proposal.summary.isNotBlank()) {
                Text(text = proposal.summary, style = MaterialTheme.typography.bodySmall)
            }
            if (proposal.stats.isNotBlank()) {
                Text(
                    text = stringResource(R.string.patch_stats, proposal.stats),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            OutlinedButton(onClick = onViewDiff) {
                Text(stringResource(R.string.view_diff_button))
            }
            if (diff != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = diff.diff,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp),
                    )
                }
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
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onApprove, enabled = approveEnabled) {
            Text(stringResource(R.string.approve_button))
        }
        OutlinedButton(onClick = onReject) {
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
private fun ComposerCard(
    state: MobileGatewayUiState,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onQueue: () -> Unit,
    onStop: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(stringResource(R.string.section_composer))
            OutlinedTextField(
                value = state.messageText,
                onValueChange = onMessageChange,
                label = { Text(stringResource(R.string.message_label)) },
                placeholder = { Text(stringResource(R.string.message_placeholder)) },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSend,
                    enabled = state.status == ConnectionStatus.Connected && state.messageText.isNotBlank(),
                ) {
                    Text(stringResource(R.string.send_button))
                }
                OutlinedButton(
                    onClick = onQueue,
                    enabled = state.status == ConnectionStatus.Connected && state.messageText.isNotBlank(),
                ) {
                    Text(stringResource(R.string.queue_button))
                }
                OutlinedButton(
                    onClick = onStop,
                    enabled = state.status == ConnectionStatus.Connected,
                ) {
                    Text(stringResource(R.string.stop_turn_button))
                }
            }
        }
    }
}

@Composable
private fun QueueCard(
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
private fun StatusLine(state: MobileGatewayUiState) {
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
