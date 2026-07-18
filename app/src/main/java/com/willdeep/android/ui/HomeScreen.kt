package com.willdeep.android.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.willdeep.android.R
import com.willdeep.android.mobile.GatewaySession
import com.willdeep.android.mobile.GatewayWorkspace
import com.willdeep.android.mobile.PatchProposal
import com.willdeep.android.mobile.PendingToolApproval

private enum class SessionFilter { All, Working, NeedsInput, Completed }

internal const val ALL_WORKSPACES_KEY = "__all_workspaces__"
internal const val NO_WORKSPACE_KEY = "__no_workspace__"
internal const val WORKSPACE_SESSION_PREVIEW_LIMIT = 3
internal const val WORKSPACE_SWITCHER_TEST_TAG = "workspace-switcher"

internal fun workspacePillTestTag(key: String): String = "workspace-pill-$key"

private data class HomeWorkspaceTab(
    val key: String,
    val label: String,
    val subtitle: String,
    val count: Int,
)

internal data class HomeWorkspaceSessionGroup(
    val key: String,
    val label: String,
    val sessions: List<GatewaySession>,
) {
    fun visibleSessions(expanded: Boolean): List<GatewaySession> {
        return if (expanded) sessions else sessions.take(WORKSPACE_SESSION_PREVIEW_LIMIT)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: MobileGatewayUiState,
    versionName: String,
    onScanClick: () -> Unit,
    onCreateSession: () -> Unit,
    onSelectSession: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onForget: () -> Unit,
    onRemoteMacSelected: (String) -> Unit,
    onToolDecision: (PendingToolApproval, Boolean) -> Unit,
    onToolAnswerChange: (String, String) -> Unit,
    onToolConfirmationChange: (String, String) -> Unit,
    onPatchDecision: (PatchProposal, Boolean) -> Unit,
    onWorkspacePickerDismiss: () -> Unit,
    onWorkspacePickerRetry: () -> Unit,
    onWorkspaceSelected: (String) -> Unit,
    onOpenAttention: () -> Unit = {},
    onOpenDiagnostics: () -> Unit = {},
) {
    val isPaired = state.isPaired
    val isConnected = state.status == ConnectionStatus.Connected

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = onScanClick) {
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
        floatingActionButton = {
            if (isPaired) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (isConnected) onCreateSession() else onConnect()
                    },
                    icon = {
                        Icon(painter = painterResource(R.drawable.ic_add), contentDescription = null)
                    },
                    text = { Text(stringResource(R.string.new_session_button)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(6.dp))
            if (!isPaired) {
                NotPairedBody(
                    state = state,
                    onScanClick = onScanClick,
                    modifier = Modifier.weight(1f),
                )
            } else {
                PairedBody(
                    state = state,
                    onScanClick = onScanClick,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect,
                    onForget = onForget,
                    onRemoteMacSelected = onRemoteMacSelected,
                    onToolDecision = onToolDecision,
                    onToolAnswerChange = onToolAnswerChange,
                    onToolConfirmationChange = onToolConfirmationChange,
                    onPatchDecision = onPatchDecision,
                    onSelectSession = onSelectSession,
                    onOpenAttention = onOpenAttention,
                    onOpenDiagnostics = onOpenDiagnostics,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.home_version, versionName),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
    if (state.workspacePickerVisible) {
        WorkspacePickerDialog(
            workspaces = state.workspaces,
            recentPaths = state.recentWorkspacePaths,
            isLoading = state.isLoadingWorkspaces,
            errorMessage = state.workspacePickerError,
            isConnected = isConnected,
            onDismiss = onWorkspacePickerDismiss,
            onRetry = onWorkspacePickerRetry,
            onSelect = onWorkspaceSelected,
        )
    }
}

@Composable
private fun WorkspacePickerDialog(
    workspaces: List<GatewayWorkspace>,
    recentPaths: List<String>,
    isLoading: Boolean,
    errorMessage: String?,
    isConnected: Boolean,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var manualPath by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.workspace_picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.workspace_picker_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                if (!isConnected) {
                    Text(
                        text = stringResource(R.string.workspace_picker_disconnected),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = stringResource(R.string.workspace_picker_loading),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } else if (workspaces.isEmpty()) {
                    Text(
                        text = stringResource(R.string.workspace_picker_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 280.dp),
                    ) {
                        items(workspaces, key = { it.path }) { workspace ->
                            WorkspaceRow(workspace = workspace, onClick = { onSelect(workspace.path) })
                        }
                    }
                }
                if (recentPaths.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.workspace_picker_recent_title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        recentPaths.take(5).forEach { path ->
                            TextButton(onClick = { onSelect(path) }) {
                                Text(
                                    text = path,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.workspace_picker_manual_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = manualPath,
                    onValueChange = { manualPath = it },
                    placeholder = { Text(stringResource(R.string.workspace_picker_manual_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelect(manualPath.trim()) },
                enabled = manualPath.trim().isNotEmpty(),
            ) {
                Text(stringResource(R.string.workspace_picker_create_button))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = onRetry, enabled = isConnected && !isLoading) {
                    Text(stringResource(R.string.workspace_picker_refresh_button))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        },
    )
}

@Composable
private fun WorkspaceRow(workspace: GatewayWorkspace, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = workspace.name.ifBlank { workspace.path.substringAfterLast('/').ifBlank { workspace.path } },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = workspace.path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (workspace.sessionCount > 0) {
                Text(
                    text = stringResource(R.string.workspace_picker_session_count, workspace.sessionCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun NotPairedBody(
    state: MobileGatewayUiState,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showProgress = state.status == ConnectionStatus.Pairing ||
        state.status == ConnectionStatus.Connecting
    val showError = state.status == ConnectionStatus.Error && !state.errorMessage.isNullOrBlank()
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            ScanPromptCard(onScanClick)
        }
        if (showProgress) {
            item { PairingProgressCard(state.status) }
        }
        if (showError) {
            item { PairingErrorCard(message = state.errorMessage.orEmpty(), onScanClick = onScanClick) }
        }
    }
}

@Composable
private fun PairingProgressCard(status: ConnectionStatus) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Text(
                text = if (status == ConnectionStatus.Pairing) {
                    stringResource(R.string.status_pairing)
                } else {
                    stringResource(R.string.status_connecting)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun PairingErrorCard(message: String, onScanClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            FilledTonalButton(onClick = onScanClick) {
                Text(stringResource(R.string.home_scan_to_pair))
            }
        }
    }
}

@Composable
private fun ScanPromptCard(onScanClick: () -> Unit) {
    Card(
        onClick = onScanClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_scan_qr),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_scan_to_pair),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_empty_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PairedBody(
    state: MobileGatewayUiState,
    onScanClick: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onForget: () -> Unit,
    onRemoteMacSelected: (String) -> Unit,
    onToolDecision: (PendingToolApproval, Boolean) -> Unit,
    onToolAnswerChange: (String, String) -> Unit,
    onToolConfirmationChange: (String, String) -> Unit,
    onPatchDecision: (PatchProposal, Boolean) -> Unit,
    onSelectSession: (String) -> Unit,
    onOpenAttention: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedFilter by rememberSaveable { mutableStateOf(SessionFilter.All) }
    var selectedWorkspaceKey by rememberSaveable { mutableStateOf(ALL_WORKSPACES_KEY) }
    var expandedWorkspaceKeys by remember { mutableStateOf(emptySet<String>()) }
    var remoteMacPickerVisible by rememberSaveable { mutableStateOf(false) }
    val allWorkspacesLabel = stringResource(R.string.workspace_all)
    val allWorkspacesSubtitle = stringResource(R.string.workspace_all_subtitle)
    val noWorkspaceLabel = stringResource(R.string.workspace_no_workspace)
    val attentionSessionIds = remember(state.pendingTools, state.patchProposals) {
        (state.pendingTools.mapNotNull { it.sessionId } +
            state.patchProposals.mapNotNull { it.sessionId }).toSet()
    }
    val workspaceTabs = remember(
        state.workspaces,
        state.sessions,
        allWorkspacesLabel,
        allWorkspacesSubtitle,
        noWorkspaceLabel,
    ) {
        workspaceTabs(
            workspaces = state.workspaces,
            sessions = state.sessions,
            allLabel = allWorkspacesLabel,
            allSubtitle = allWorkspacesSubtitle,
            noWorkspaceLabel = noWorkspaceLabel,
        )
    }
    val effectiveWorkspaceKey = selectedWorkspaceKey.takeIf { selected ->
        workspaceTabs.any { it.key == selected }
    } ?: ALL_WORKSPACES_KEY
    val statusFilteredSessions = remember(state.sessions, selectedFilter, attentionSessionIds) {
        state.sessions.filterByStatus(selectedFilter, attentionSessionIds)
    }
    val visibleSessions = remember(statusFilteredSessions, effectiveWorkspaceKey) {
        statusFilteredSessions.filterByWorkspace(effectiveWorkspaceKey)
    }
    val workspaceGroups = remember(visibleSessions, noWorkspaceLabel) {
        groupSessionsByWorkspace(visibleSessions, noWorkspaceLabel)
    }
    val counts = remember(state.sessions, attentionSessionIds) {
        sessionCounts(state.sessions, attentionSessionIds)
    }
    val isConnected = state.status == ConnectionStatus.Connected

    Column(modifier = modifier) {
        RemoteMacSelector(
            state = state,
            onClick = { remoteMacPickerVisible = true },
            onDiagnosticsClick = onOpenDiagnostics,
        )
        if (
            state.status == ConnectionStatus.Reconnecting &&
            state.desktopResponseAgeMillis >= com.willdeep.android.mobile.ReconnectPolicy.HEARTBEAT_TIMEOUT_MILLIS
        ) {
            Spacer(Modifier.height(8.dp))
            MacReconnectNotice(state = state)
        }
        val attentionCount = state.pendingTools.size + state.patchProposals.size
        if (attentionCount > 0) {
            Spacer(Modifier.height(8.dp))
            AttentionSummaryCard(
                approvalCount = state.pendingTools.count { !it.requiresAnswer } + state.patchProposals.size,
                questionCount = state.pendingTools.count { it.requiresAnswer },
                onClick = onOpenAttention,
            )
        }
        Spacer(Modifier.height(8.dp))
        WorkspaceRail(
            tabs = workspaceTabs,
            selectedKey = effectiveWorkspaceKey,
            onSelect = { workspaceKey ->
                selectedWorkspaceKey = workspaceKey
                expandedWorkspaceKeys = emptySet()
            },
        )
        Spacer(Modifier.height(8.dp))
        FilterChipsRow(
            selected = selectedFilter,
            counts = counts,
            onSelect = { selectedFilter = it },
        )
        Spacer(Modifier.height(12.dp))
        if (state.sessions.isEmpty()) {
            EmptySessionsForPaired(
                connected = isConnected,
                onConnect = onConnect,
                onScanClick = onScanClick,
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 96.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (visibleSessions.isEmpty()) {
                    item(key = "empty-filter") {
                        Text(
                            text = stringResource(R.string.home_filter_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 18.dp),
                        )
                    }
                } else {
                    workspaceGroups.forEach { group ->
                        val expanded = group.key in expandedWorkspaceKeys
                        item(key = "workspace-header-${group.key}") {
                            WorkspaceHeader(name = group.label, count = group.sessions.size)
                        }
                        items(
                            items = group.visibleSessions(expanded),
                            key = { session -> "${group.key}:${session.id}" },
                        ) { session ->
                            val sessionState = state.copy(
                                pendingTools = state.pendingTools.filter { it.sessionId == session.id },
                                patchProposals = state.patchProposals.filter { it.sessionId == session.id },
                            )
                            HomeSessionCard(
                                session = session,
                                selected = session.id == state.selectedSessionId,
                                isConnected = isConnected,
                                reviewState = sessionState,
                                onToolDecision = onToolDecision,
                                onToolAnswerChange = onToolAnswerChange,
                                onToolConfirmationChange = onToolConfirmationChange,
                                onPatchDecision = onPatchDecision,
                                onClick = { onSelectSession(session.id) },
                            )
                        }
                        if (group.sessions.size > WORKSPACE_SESSION_PREVIEW_LIMIT) {
                            item(key = "workspace-toggle-${group.key}") {
                                WorkspaceSessionToggle(
                                    expanded = expanded,
                                    hiddenCount = group.sessions.size - WORKSPACE_SESSION_PREVIEW_LIMIT,
                                    onClick = {
                                        expandedWorkspaceKeys = if (expanded) {
                                            expandedWorkspaceKeys - group.key
                                        } else {
                                            expandedWorkspaceKeys + group.key
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (remoteMacPickerVisible) {
        RemoteMacPickerSheet(
            state = state,
            onDismiss = { remoteMacPickerVisible = false },
            onSelect = { id ->
                remoteMacPickerVisible = false
                onRemoteMacSelected(id)
            },
            onAddComputer = {
                remoteMacPickerVisible = false
                onScanClick()
            },
            onRemoveSelected = {
                remoteMacPickerVisible = false
                onForget()
            },
            onConnect = {
                remoteMacPickerVisible = false
                onConnect()
            },
            onDisconnect = {
                remoteMacPickerVisible = false
                onDisconnect()
            },
        )
    }
}

@Composable
private fun RemoteMacSelector(
    state: MobileGatewayUiState,
    onClick: () -> Unit,
    onDiagnosticsClick: () -> Unit,
) {
    val selectorDescription = stringResource(R.string.remote_mac_selector_title)
    val selectedMac = state.pairedMacs.firstOrNull { it.id == state.selectedMacId }
    val statusColor = when (state.status) {
        ConnectionStatus.Connected -> Color(0xFF2F9E44)
        ConnectionStatus.Reconnecting -> Color(0xFFD89B2B)
        ConnectionStatus.Error -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = selectorDescription },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_computer),
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(21.dp),
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(statusColor),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedMac?.name ?: state.desktopName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = selectedMacStatusText(state),
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onDiagnosticsClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = stringResource(R.string.diagnostics_open),
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun AttentionSummaryCard(
    approvalCount: Int,
    questionCount: Int,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFF4DE),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_approval),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.attention_summary_title, approvalCount + questionCount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.attention_summary_detail, approvalCount, questionCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun selectedMacStatusText(state: MobileGatewayUiState): String {
    val responseSeconds = (state.desktopResponseAgeMillis / 1_000L).coerceAtLeast(0L)
    return when (state.status) {
        ConnectionStatus.Connected -> stringResource(
            R.string.remote_mac_responded_seconds,
            responseSeconds,
        )
        ConnectionStatus.AwaitingDesktop -> stringResource(R.string.remote_mac_waiting_response)
        ConnectionStatus.Reconnecting -> stringResource(
            R.string.remote_mac_reconnecting_seconds,
            responseSeconds,
        )
        ConnectionStatus.Connecting,
        ConnectionStatus.Pairing -> stringResource(R.string.remote_mac_opening_channel)
        ConnectionStatus.Error -> state.errorMessage?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.status_disconnected)
        ConnectionStatus.Idle,
        ConnectionStatus.Disconnected -> lastResponseText(state.lastDesktopResponseAtEpochMillis)
    }
}

@Composable
private fun MacReconnectNotice(state: MobileGatewayUiState) {
    val seconds = (state.desktopResponseAgeMillis / 1_000L).coerceAtLeast(20L)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFF7E7),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = stringResource(R.string.remote_mac_reconnect_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8B5E00),
            )
            Text(
                text = stringResource(R.string.remote_mac_no_response_seconds, seconds),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = if (state.isTransportConnected) {
                    stringResource(R.string.remote_mac_transport_waiting)
                } else {
                    stringResource(R.string.remote_mac_transport_reconnecting)
                },
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF8B5E00),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoteMacPickerSheet(
    state: MobileGatewayUiState,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onAddComputer: () -> Unit,
    onRemoveSelected: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.remote_mac_selector_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            state.pairedMacs.forEach { mac ->
                val selected = mac.id == state.selectedMacId
                Surface(
                    onClick = { onSelect(mac.id) },
                    shape = RoundedCornerShape(15.dp),
                    color = if (selected) Color(0xFFF1F8F0) else MaterialTheme.colorScheme.surface,
                    tonalElevation = if (selected) 1.dp else 0.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_computer),
                            contentDescription = null,
                            tint = if (selected && state.status == ConnectionStatus.Connected) {
                                Color(0xFF2F9E44)
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            modifier = Modifier.size(22.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mac.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (selected) {
                                    selectedMacStatusText(state)
                                } else {
                                    lastResponseText(mac.lastDesktopResponseAtEpochMillis)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        if (selected) {
                            Text(
                                text = stringResource(R.string.remote_mac_selected_mark),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF2F9E44),
                            )
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = onAddComputer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.remote_mac_add_computer))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onRemoveSelected) {
                    Text(stringResource(R.string.remote_mac_remove_selected))
                }
                TextButton(
                    onClick = if (state.isTransportConnected) onDisconnect else onConnect,
                ) {
                    Text(
                        if (state.isTransportConnected) {
                            stringResource(R.string.disconnect_button)
                        } else {
                            stringResource(R.string.connect_button)
                        }
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun lastResponseText(epochMillis: Long): String {
    if (epochMillis <= 0L) return stringResource(R.string.remote_mac_never_responded)
    val ageMillis = (System.currentTimeMillis() - epochMillis).coerceAtLeast(0L)
    val seconds = ageMillis / 1_000L
    return when {
        seconds < 60L -> stringResource(R.string.remote_mac_last_seen_seconds, seconds)
        seconds < 3_600L -> stringResource(R.string.remote_mac_last_seen_minutes, seconds / 60L)
        seconds < 86_400L -> stringResource(R.string.remote_mac_last_seen_hours, seconds / 3_600L)
        else -> stringResource(R.string.remote_mac_last_seen_days, seconds / 86_400L)
    }
}

@Composable
private fun HomeStatusWorkspaceRow(
    state: MobileGatewayUiState,
    tabs: List<HomeWorkspaceTab>,
    selectedKey: String,
    onWorkspaceSelect: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onForget: () -> Unit,
) {
    var connectionExpanded by rememberSaveable { mutableStateOf(false) }
    var workspaceExpanded by rememberSaveable { mutableStateOf(false) }
    val status = state.status
    val isConnected = status == ConnectionStatus.Connected
    val isReconnecting = status == ConnectionStatus.Reconnecting
    val isConnecting = status == ConnectionStatus.Connecting
    val selectedTab = tabs.firstOrNull { it.key == selectedKey } ?: tabs.firstOrNull()
    val dotColor = when {
        isConnected -> Color(0xFF1FBF75)
        isReconnecting || isConnecting -> Color(0xFFFFB020)
        status == ConnectionStatus.Error -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }
    val connectionTitle = state.desktopName.ifBlank {
        stringResource(R.string.connection_status_title)
    }
    val connectionSubtitle = when {
        isConnected -> stringResource(R.string.status_connected)
        isReconnecting -> stringResource(R.string.status_reconnecting_short)
        isConnecting -> stringResource(R.string.status_connecting)
        status == ConnectionStatus.Error -> state.errorMessage?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.status_disconnected)
        else -> stringResource(R.string.status_disconnected)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                onClick = { connectionExpanded = !connectionExpanded },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.weight(1.15f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = connectionTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = connectionSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Surface(
                onClick = {
                    workspaceExpanded = !workspaceExpanded
                    connectionExpanded = false
                },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.weight(0.85f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = (selectedTab?.label ?: stringResource(R.string.workspace_title)).take(1),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedTab?.label ?: stringResource(R.string.workspace_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (workspaceExpanded) {
                                stringResource(R.string.workspace_collapse)
                            } else {
                                stringResource(R.string.workspace_expand)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        if (connectionExpanded) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isConnected || isReconnecting) {
                    TextButton(onClick = onDisconnect) {
                        Text(stringResource(R.string.disconnect_button))
                    }
                } else {
                    TextButton(onClick = onConnect) {
                        Text(stringResource(R.string.connect_button))
                    }
                }
                TextButton(onClick = onForget) {
                    Text(stringResource(R.string.forget_button))
                }
            }
        }
        if (workspaceExpanded) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 8.dp),
            ) {
                items(tabs, key = { it.key }) { tab ->
                    WorkspacePill(
                        tab = tab,
                        selected = tab.key == selectedKey,
                        onClick = {
                            onWorkspaceSelect(tab.key)
                            workspaceExpanded = false
                            connectionExpanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkspaceRail(
    tabs: List<HomeWorkspaceTab>,
    selectedKey: String,
    onSelect: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedTab = tabs.firstOrNull { it.key == selectedKey } ?: tabs.firstOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            onClick = { expanded = !expanded },
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            modifier = Modifier.testTag(WORKSPACE_SWITCHER_TEST_TAG),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = stringResource(R.string.workspace_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = selectedTab?.label ?: stringResource(R.string.workspace_all),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "·",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = stringResource(
                        if (expanded) R.string.workspace_collapse else R.string.workspace_expand
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (expanded) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 8.dp),
            ) {
                items(tabs, key = { it.key }) { tab ->
                    WorkspacePill(
                        tab = tab,
                        selected = tab.key == selectedKey,
                        onClick = {
                            onSelect(tab.key)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkspacePill(
    tab: HomeWorkspaceTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        tonalElevation = if (selected) 0.dp else 1.dp,
        modifier = Modifier.testTag(workspacePillTestTag(tab.key)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = tab.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = tab.count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                } else {
                    MaterialTheme.colorScheme.outline
                },
            )
        }
    }
}

@Composable
private fun WorkspaceHeader(name: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun WorkspaceSessionToggle(
    expanded: Boolean,
    hiddenCount: Int,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = if (expanded) {
                stringResource(R.string.workspace_sessions_collapse)
            } else {
                stringResource(R.string.workspace_sessions_expand, hiddenCount)
            },
        )
    }
}

@Composable
private fun ConnectionSummaryRow(
    state: MobileGatewayUiState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onForget: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val status = state.status
    val isConnected = status == ConnectionStatus.Connected
    val isReconnecting = status == ConnectionStatus.Reconnecting
    val isConnecting = status == ConnectionStatus.Connecting
    val dotColor = when {
        isConnected -> Color(0xFF1FBF75)
        isReconnecting || isConnecting -> Color(0xFFFFB020)
        status == ConnectionStatus.Error -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }
    val title = if (state.desktopName.isNotBlank()) {
        stringResource(R.string.paired_to, state.desktopName)
    } else {
        stringResource(R.string.connection_status_title)
    }
    val subtitle = when {
        isConnected -> stringResource(R.string.status_connected)
        isReconnecting -> stringResource(R.string.status_reconnecting_short)
        isConnecting -> stringResource(R.string.status_connecting)
        status == ConnectionStatus.Error -> stringResource(R.string.status_error, state.errorMessage.orEmpty())
        else -> stringResource(R.string.status_disconnected)
    }
    Surface(
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = if (expanded) {
                        stringResource(R.string.connection_collapse)
                    } else {
                        stringResource(R.string.connection_expand)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isConnected || isReconnecting) {
                        TextButton(onClick = onDisconnect) {
                            Text(stringResource(R.string.disconnect_button))
                        }
                    } else {
                        TextButton(onClick = onConnect) {
                            Text(stringResource(R.string.connect_button))
                        }
                    }
                    TextButton(onClick = onForget) {
                        Text(stringResource(R.string.forget_button))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySessionsForPaired(
    connected: Boolean,
    onConnect: () -> Unit,
    onScanClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
    ) {
        Text(
            text = if (connected) stringResource(R.string.session_empty)
            else stringResource(R.string.status_disconnected),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (!connected) {
            FilledTonalButton(onClick = onConnect) {
                Text(stringResource(R.string.connect_button))
            }
        }
        TextButton(onClick = onScanClick) {
            Text(stringResource(R.string.home_scan_to_pair))
        }
    }
}

private fun List<GatewaySession>.filterByStatus(
    filter: SessionFilter,
    attentionSessionIds: Set<String>,
): List<GatewaySession> = when (filter) {
    SessionFilter.All -> this
    SessionFilter.Working -> filter { it.isResponding }
    SessionFilter.NeedsInput -> filter { it.id in attentionSessionIds }
    SessionFilter.Completed -> filter { !it.isActive && !it.isResponding && it.id !in attentionSessionIds }
}

private fun List<GatewaySession>.filterByWorkspace(workspaceKey: String): List<GatewaySession> {
    if (workspaceKey == ALL_WORKSPACES_KEY) return this
    return filter { session -> session.workspaceKey() == workspaceKey }
}

private fun workspaceTabs(
    workspaces: List<GatewayWorkspace>,
    sessions: List<GatewaySession>,
    allLabel: String,
    allSubtitle: String,
    noWorkspaceLabel: String,
): List<HomeWorkspaceTab> {
    val sessionCountsByWorkspace = sessions
        .groupingBy(GatewaySession::workspaceKey)
        .eachCount()
    val tabs = mutableListOf(
        HomeWorkspaceTab(
            key = ALL_WORKSPACES_KEY,
            label = allLabel,
            subtitle = allSubtitle,
            count = sessions.size,
        )
    )
    workspaces.forEach { workspace ->
        val label = workspace.name.ifBlank { workspace.path.substringAfterLast('/').ifBlank { workspace.path } }
        if (label.isNotBlank() && tabs.none { it.key == label }) {
            tabs += HomeWorkspaceTab(
                key = label,
                label = label,
                subtitle = workspace.path,
                count = sessionCountsByWorkspace[label] ?: workspace.sessionCount,
            )
        }
    }
    sessionCountsByWorkspace
        .filterKeys { it != NO_WORKSPACE_KEY }
        .toSortedMap()
        .forEach { (workspaceName, count) ->
            if (tabs.none { it.key == workspaceName }) {
                tabs += HomeWorkspaceTab(
                    key = workspaceName,
                    label = workspaceName,
                    subtitle = workspaceName,
                    count = count,
                )
            }
        }
    sessionCountsByWorkspace[NO_WORKSPACE_KEY]
        ?.takeIf { count -> count > 0 }
        ?.let { count ->
            tabs += HomeWorkspaceTab(
                key = NO_WORKSPACE_KEY,
                label = noWorkspaceLabel,
                subtitle = noWorkspaceLabel,
                count = count,
            )
        }
    return tabs
}

internal fun groupSessionsByWorkspace(
    sessions: List<GatewaySession>,
    noWorkspaceLabel: String,
): List<HomeWorkspaceSessionGroup> {
    val groups = linkedMapOf<String, MutableList<GatewaySession>>()
    sessions.forEach { session ->
        groups.getOrPut(session.workspaceKey()) { mutableListOf() } += session
    }
    return groups.map { (key, groupedSessions) ->
        HomeWorkspaceSessionGroup(
            key = key,
            label = if (key == NO_WORKSPACE_KEY) noWorkspaceLabel else key,
            // The gateway session list is already ordered newest-first; keep that order here.
            sessions = groupedSessions,
        )
    }
}

private fun GatewaySession.workspaceKey(): String {
    return workspaceName.trim().ifBlank { NO_WORKSPACE_KEY }
}

private fun sessionCounts(
    sessions: List<GatewaySession>,
    attentionSessionIds: Set<String>,
): Map<SessionFilter, Int> = mapOf(
    SessionFilter.All to sessions.size,
    SessionFilter.Working to sessions.count { it.isResponding },
    SessionFilter.NeedsInput to sessions.count { it.id in attentionSessionIds },
    SessionFilter.Completed to sessions.count {
        !it.isActive && !it.isResponding && it.id !in attentionSessionIds
    },
)

@Composable
private fun FilterChipsRow(
    selected: SessionFilter,
    counts: Map<SessionFilter, Int>,
    onSelect: (SessionFilter) -> Unit,
) {
    val filterItems = listOf(
        SessionFilter.All to stringResource(R.string.home_filter_all),
        SessionFilter.Working to stringResource(R.string.home_filter_working),
        SessionFilter.NeedsInput to stringResource(R.string.home_filter_needs_input),
        SessionFilter.Completed to stringResource(R.string.home_filter_completed),
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(filterItems, key = { it.first.name }) { (key, label) ->
            val count = counts[key] ?: 0
            FilterChip(
                selected = key == selected,
                onClick = { onSelect(key) },
                shape = RoundedCornerShape(50),
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label)
                        if (count > 0) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                ),
                border = null,
            )
        }
    }
}

@Composable
private fun HomeSessionCard(
    session: GatewaySession,
    selected: Boolean,
    isConnected: Boolean,
    reviewState: MobileGatewayUiState,
    onToolDecision: (PendingToolApproval, Boolean) -> Unit,
    onToolAnswerChange: (String, String) -> Unit,
    onToolConfirmationChange: (String, String) -> Unit,
    onPatchDecision: (PatchProposal, Boolean) -> Unit,
    onClick: () -> Unit,
) {
    val hasReview = reviewState.pendingTools.isNotEmpty() || reviewState.patchProposals.isNotEmpty()
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Surface(
                onClick = onClick,
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = session.title.trim().firstOrNull()?.uppercase()
                                ?: session.workspaceName.trim().firstOrNull()?.uppercase()
                                ?: stringResource(R.string.home_session_initial_fallback),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        if (hasReview) {
                            Box(
                                Modifier
                                    .size(9.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (session.isResponding) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                text = session.title.ifBlank {
                                    session.workspaceName.ifBlank { session.id.take(8) }
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = if (session.messageCount > 0) {
                                    stringResource(R.string.message_count, session.messageCount)
                                } else {
                                    stringResource(R.string.home_session_no_message_count)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isConnected) Color(0xFF2F9E44)
                                        else MaterialTheme.colorScheme.outline
                                    ),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = when {
                                    hasReview -> stringResource(R.string.home_session_waiting_input)
                                    session.isResponding -> stringResource(R.string.home_session_running)
                                    isConnected -> stringResource(R.string.home_session_completed)
                                    else -> stringResource(R.string.home_session_disconnected)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                            if (session.workspaceName.isNotBlank()) {
                                Text(
                                    text = stringResource(
                                        R.string.home_session_workspace_suffix,
                                        session.workspaceName,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            if (hasReview) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                ) {
                    ApprovalCard(
                        state = reviewState,
                        onToolDecision = onToolDecision,
                        onToolAnswerChange = onToolAnswerChange,
                        onToolConfirmationChange = onToolConfirmationChange,
                        onPatchDecision = onPatchDecision,
                        showSectionTitle = false,
                    )
                }
            }
        }
    }
}
