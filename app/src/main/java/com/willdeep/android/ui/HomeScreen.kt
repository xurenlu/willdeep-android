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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.willdeep.android.R
import com.willdeep.android.mobile.GatewaySession
import com.willdeep.android.mobile.GatewayWorkspace

private enum class SessionFilter { All, Working, NeedsInput, Completed }

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
    onWorkspacePickerDismiss: () -> Unit,
    onWorkspacePickerRetry: () -> Unit,
    onWorkspaceSelected: (String) -> Unit,
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
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_title),
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(16.dp))
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
                    onSelectSession = onSelectSession,
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
    onSelectSession: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filterKey by rememberSaveable { mutableStateOf(SessionFilter.All.name) }
    val filter = remember(filterKey) { SessionFilter.valueOf(filterKey) }
    val counts = remember(state.sessions) { sessionCounts(state.sessions) }
    val visibleSessions = remember(state.sessions, filter) { filterSessions(state.sessions, filter) }
    val isConnected = state.status == ConnectionStatus.Connected ||
        state.status == ConnectionStatus.Reconnecting
    val unknownGroup = stringResource(R.string.workspace_unknown_group)
    val grouped = remember(visibleSessions, unknownGroup) {
        visibleSessions
            .groupBy { it.workspaceName.trim().ifBlank { unknownGroup } }
            .toSortedMap()
    }

    Column(modifier = modifier) {
        ConnectionSummaryRow(
            state = state,
            onConnect = onConnect,
            onDisconnect = onDisconnect,
            onForget = onForget,
        )
        Spacer(Modifier.height(16.dp))
        FilterChipsRow(filter, counts) { filterKey = it.name }
        Spacer(Modifier.height(12.dp))
        if (state.sessions.isEmpty()) {
            EmptySessionsForPaired(
                connected = isConnected,
                onConnect = onConnect,
                onScanClick = onScanClick,
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 96.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (visibleSessions.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.session_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    }
                } else {
                    grouped.forEach { (group, sessions) ->
                        item(key = "header-$group") {
                            WorkspaceHeader(name = group, count = sessions.size)
                        }
                        items(sessions, key = { it.id }) { session ->
                            HomeSessionCard(
                                session = session,
                                selected = session.id == state.selectedSessionId,
                                isConnected = isConnected,
                                onClick = { onSelectSession(session.id) },
                            )
                        }
                        item(key = "spacer-$group") {
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
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
private fun ConnectionSummaryRow(
    state: MobileGatewayUiState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onForget: () -> Unit,
) {
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
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (state.desktopName.isNotBlank()) {
                        stringResource(R.string.paired_to, state.desktopName)
                    } else {
                        stringResource(R.string.status_connected)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = when {
                    isConnected -> stringResource(R.string.status_connected)
                    isReconnecting -> stringResource(R.string.status_reconnecting_short)
                    isConnecting -> stringResource(R.string.status_connecting)
                    status == ConnectionStatus.Error ->
                        stringResource(R.string.status_error, state.errorMessage.orEmpty())
                    else -> stringResource(R.string.status_disconnected)
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
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

private fun filterSessions(
    sessions: List<GatewaySession>,
    filter: SessionFilter,
): List<GatewaySession> = when (filter) {
    SessionFilter.All -> sessions
    SessionFilter.Working -> sessions.filter { it.isResponding }
    SessionFilter.NeedsInput -> sessions.filter { it.isActive && !it.isResponding }
    SessionFilter.Completed -> sessions.filter { !it.isActive && !it.isResponding }
}

private fun sessionCounts(sessions: List<GatewaySession>): Map<SessionFilter, Int> = mapOf(
    SessionFilter.All to sessions.size,
    SessionFilter.Working to sessions.count { it.isResponding },
    SessionFilter.NeedsInput to sessions.count { it.isActive && !it.isResponding },
    SessionFilter.Completed to sessions.count { !it.isActive && !it.isResponding },
)

@Composable
private fun FilterChipsRow(
    selected: SessionFilter,
    counts: Map<SessionFilter, Int>,
    onSelect: (SessionFilter) -> Unit,
) {
    val items = listOf(
        SessionFilter.All to stringResource(R.string.home_filter_all),
        SessionFilter.Working to stringResource(R.string.home_filter_working),
        SessionFilter.NeedsInput to stringResource(R.string.home_filter_needs_input),
        SessionFilter.Completed to stringResource(R.string.home_filter_completed),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items.forEach { (key, label) ->
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
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
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
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = session.title.trim().firstOrNull()?.uppercase()
                        ?: session.workspaceName.trim().firstOrNull()?.uppercase()
                        ?: "·",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
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
                            "—"
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
                                if (isConnected) Color(0xFF1FBF75)
                                else MaterialTheme.colorScheme.outline
                            ),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isConnected) {
                            stringResource(R.string.home_session_connected)
                        } else {
                            stringResource(R.string.home_session_disconnected)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    if (session.workspaceName.isNotBlank()) {
                        Text(
                            text = "  ·  ${session.workspaceName}",
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
}
