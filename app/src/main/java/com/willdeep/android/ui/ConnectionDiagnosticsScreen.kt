package com.willdeep.android.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.willdeep.android.R
import java.net.URI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionDiagnosticsScreen(
    state: MobileGatewayUiState,
    onBack: () -> Unit,
    onCheckNow: () -> Unit,
    onRemoteMacSelected: (String) -> Unit,
) {
    var pickerVisible by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val selectedMac = state.pairedMacs.firstOrNull { it.id == state.selectedMacId }
    val responseHealthy = state.status == ConnectionStatus.Connected
    val responseSeconds = state.desktopResponseAgeMillis.coerceAtLeast(0L) / 1_000L
    val usesTailscale = state.baseUrl.isTailscaleEndpoint()
    val tailscaleAvailable = usesTailscale || state.fallbackBaseUrls.any { it.isTailscaleEndpoint() }
    val routeText = stringResource(
        if (usesTailscale) R.string.diagnostics_route_tailscale else R.string.diagnostics_route_lan,
    )
    val desktopStateText = when {
        responseHealthy -> stringResource(R.string.diagnostics_mac_message_healthy)
        state.isTransportConnected -> stringResource(R.string.diagnostics_server_only)
        else -> stringResource(R.string.diagnostics_transport_unavailable)
    }
    val shareText = listOf(
        stringResource(R.string.diagnostics_export_title),
        stringResource(R.string.diagnostics_export_mac, selectedMac?.name ?: state.desktopName),
        stringResource(R.string.diagnostics_export_status, desktopStateText),
        stringResource(
            R.string.diagnostics_export_transport,
            stringResource(
                if (state.isTransportConnected) R.string.diagnostics_available
                else R.string.diagnostics_unavailable,
            ),
        ),
        stringResource(R.string.diagnostics_export_response_age, responseSeconds),
        stringResource(R.string.diagnostics_export_route, routeText),
        stringResource(R.string.diagnostics_export_protocol, state.protocolVersion),
        stringResource(R.string.diagnostics_export_server, state.gatewayServerVersion),
    ).joinToString("\n")

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
                title = { Text(stringResource(R.string.diagnostics_title), fontWeight = FontWeight.Bold) },
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
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.diagnostics_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Card(
                onClick = { pickerVisible = true },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DiagnosticComputerIcon(healthy = responseHealthy)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            selectedMac?.name ?: state.desktopName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            stringResource(R.string.diagnostics_switch_mac),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = stringResource(R.string.diagnostics_switch_mac),
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (responseHealthy) Color(0xFFEAF7E8) else Color(0xFFFFF4DE),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        desktopStateText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (responseHealthy) Color(0xFF25743B) else Color(0xFF8B5E00),
                    )
                    Text(
                        if (responseHealthy) {
                            stringResource(R.string.diagnostics_mac_message_age, responseSeconds)
                        } else {
                            stringResource(R.string.remote_mac_no_response_seconds, responseSeconds)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            DiagnosticDetailsCard(
                state = state,
                routeText = routeText,
                tailscaleAvailable = tailscaleAvailable,
                responseSeconds = responseSeconds,
            )
            DiagnosticFlow(responseHealthy = responseHealthy)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFF4DE),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.diagnostics_recovery_policy),
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7D5500),
                )
            }
            Button(
                onClick = onCheckNow,
                enabled = !state.isCheckingGateway,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (state.isCheckingGateway) R.string.checking_gateway_button
                        else R.string.diagnostics_check_now,
                    )
                )
            }
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, shareText)
                    context.startActivity(
                        Intent.createChooser(intent, context.getString(R.string.diagnostics_export_chooser)),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.diagnostics_export_button))
            }
            Text(
                text = stringResource(R.string.diagnostics_redaction_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 20.dp),
            )
        }
    }

    if (pickerVisible) {
        ModalBottomSheet(onDismissRequest = { pickerVisible = false }) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.remote_mac_selector_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                state.pairedMacs.forEach { mac ->
                    Surface(
                        onClick = {
                            pickerVisible = false
                            onRemoteMacSelected(mac.id)
                        },
                        shape = RoundedCornerShape(15.dp),
                        color = if (mac.id == state.selectedMacId) Color(0xFFF1F8F0)
                            else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            DiagnosticComputerIcon(healthy = mac.id == state.selectedMacId && responseHealthy)
                            Text(mac.name, modifier = Modifier.weight(1f))
                            if (mac.id == state.selectedMacId) {
                                Text(
                                    stringResource(R.string.remote_mac_selected_mark),
                                    color = Color(0xFF2F9E44),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
                TextButton(
                    onClick = { pickerVisible = false },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.done_button))
                }
            }
        }
    }
}

@Composable
private fun DiagnosticDetailsCard(
    state: MobileGatewayUiState,
    routeText: String,
    tailscaleAvailable: Boolean,
    responseSeconds: Long,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
            DiagnosticRow(
                stringResource(R.string.diagnostics_server_channel),
                stringResource(
                    if (state.isTransportConnected) R.string.diagnostics_available
                    else R.string.diagnostics_unavailable,
                ),
            )
            HorizontalDivider()
            DiagnosticRow(stringResource(R.string.diagnostics_route), routeText)
            HorizontalDivider()
            DiagnosticRow(
                stringResource(R.string.diagnostics_latency),
                state.gatewayLatencyMillis?.let {
                    stringResource(R.string.diagnostics_latency_value, it)
                } ?: stringResource(R.string.diagnostics_not_measured),
            )
            HorizontalDivider()
            DiagnosticRow(
                stringResource(R.string.diagnostics_last_ack),
                stringResource(R.string.diagnostics_seconds_ago, responseSeconds),
            )
            HorizontalDivider()
            DiagnosticRow(
                stringResource(R.string.diagnostics_fallback),
                stringResource(
                    if (tailscaleAvailable) R.string.diagnostics_tailscale_available
                    else R.string.diagnostics_tailscale_unavailable,
                ),
            )
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun DiagnosticFlow(responseHealthy: Boolean) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.diagnostics_message_path),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FlowNode(stringResource(R.string.diagnostics_phone), true)
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp),
                )
                FlowNode(stringResource(R.string.diagnostics_server), true)
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp),
                )
                FlowNode(stringResource(R.string.diagnostics_mac), responseHealthy)
            }
        }
    }
}

@Composable
private fun FlowNode(label: String, healthy: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DiagnosticComputerIcon(healthy)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DiagnosticComputerIcon(healthy: Boolean) {
    val color = if (healthy) Color(0xFF2F9E44) else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(color.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_computer),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(21.dp),
        )
    }
}

private fun String.isTailscaleEndpoint(): Boolean {
    val host = runCatching { URI(this).host.orEmpty() }.getOrDefault("")
    return host.startsWith("100.")
}
