package com.willdeep.android.ui

import android.app.Application
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.willdeep.android.R
import com.willdeep.android.mobile.DeviceTokenStore
import com.willdeep.android.mobile.GatewayEnvelope
import com.willdeep.android.mobile.GatewayEvent
import com.willdeep.android.mobile.GatewayFile
import com.willdeep.android.mobile.GatewayHealth
import com.willdeep.android.mobile.GatewayJob
import com.willdeep.android.mobile.GatewayMessage
import com.willdeep.android.mobile.GatewayQueuedMessage
import com.willdeep.android.mobile.GatewaySession
import com.willdeep.android.mobile.GatewayWorkspace
import com.willdeep.android.mobile.GatewayWorktree
import com.willdeep.android.mobile.GatewayWorktreeFile
import com.willdeep.android.mobile.InvalidPairingPayloadException
import com.willdeep.android.mobile.MOBILE_GATEWAY_PROTOCOL_VERSION
import com.willdeep.android.mobile.MobileGatewayClient
import com.willdeep.android.mobile.PatchDiff
import com.willdeep.android.mobile.PatchProposal
import com.willdeep.android.mobile.PairingPayload
import com.willdeep.android.mobile.PendingToolApproval
import com.willdeep.android.mobile.ReconnectPolicy
import com.willdeep.android.mobile.StoredGatewayCredential
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class ConnectionStatus {
    Idle,
    Pairing,
    Connecting,
    Reconnecting,
    Connected,
    Disconnected,
    Error,
}

data class GatewayLogLine(
    val kind: String,
    val text: String,
)

enum class MobileCommandState {
    Pending,
    Accepted,
    Failed,
}

data class ImageAttachment(
    val uri: Uri,
    val mimeType: String?,
    val approxBytes: Long?,
)

data class MobileCommandStatus(
    val id: String,
    val type: String,
    val state: MobileCommandState,
    val detail: String = "",
)

data class MobileGatewayUiState(
    val pairingPayloadText: String = "",
    val deviceName: String = Build.MODEL ?: "Android Device",
    val baseUrl: String = "",
    val fallbackBaseUrls: List<String> = emptyList(),
    val desktopName: String = "",
    val protocolVersion: String = "",
    val gatewayServerVersion: String = "",
    val pairingAllowed: Boolean? = null,
    val isCheckingGateway: Boolean = false,
    val isPaired: Boolean = false,
    val status: ConnectionStatus = ConnectionStatus.Idle,
    val errorMessage: String? = null,
    val reconnectAttempt: Int = 0,
    val reconnectDelayMillis: Long = 0,
    val filePathText: String = "",
    val loadedFile: GatewayFile? = null,
    val sessions: List<GatewaySession> = emptyList(),
    val selectedSessionId: String? = null,
    val messageText: String = "",
    val preferredWorkspacePath: String = "",
    val workspaces: List<GatewayWorkspace> = emptyList(),
    val isLoadingWorkspaces: Boolean = false,
    val workspacePickerVisible: Boolean = false,
    val workspacePickerError: String? = null,
    val recentWorkspacePaths: List<String> = emptyList(),
    val pendingTools: List<PendingToolApproval> = emptyList(),
    val toolAnswers: Map<String, String> = emptyMap(),
    val toolConfirmations: Map<String, String> = emptyMap(),
    val patchProposals: List<PatchProposal> = emptyList(),
    val patchDiffs: Map<String, PatchDiff> = emptyMap(),
    val jobs: List<GatewayJob> = emptyList(),
    val queuedMessages: List<GatewayQueuedMessage> = emptyList(),
    val conversationMessages: List<GatewayMessage> = emptyList(),
    val worktree: GatewayWorktree? = null,
    val commandStatuses: List<MobileCommandStatus> = emptyList(),
    val logLines: List<GatewayLogLine> = emptyList(),
    val attachments: List<ImageAttachment> = emptyList(),
)

internal data class GatewayHealthTarget(
    val baseUrl: String,
    val fallbackBaseUrls: List<String> = emptyList(),
    val desktopName: String,
    val protocolVersion: String,
    val requiresPairingAllowed: Boolean,
)

private data class ReachableGateway(
    val baseUrl: String,
    val health: GatewayHealth,
)

class MobileGatewayViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenStore = DeviceTokenStore(application)
    private val client = MobileGatewayClient()
    private var socketJob: Job? = null
    private var reconnectRequested = false
    private var manuallyDisconnected = false

    private val _state = MutableStateFlow(MobileGatewayUiState())
    val state: StateFlow<MobileGatewayUiState> = _state.asStateFlow()

    init {
        tokenStore.load()?.let { credential ->
            _state.update {
                it.copy(
                    baseUrl = credential.baseUrl,
                    fallbackBaseUrls = credential.fallbackBaseUrls,
                    desktopName = credential.desktopName,
                    protocolVersion = credential.protocolVersion,
                    isPaired = true,
                    status = ConnectionStatus.Disconnected,
                )
            }
            resumeConnectionIfAppropriate()
        }
    }

    fun updatePairingPayload(value: String) {
        _state.update {
            it.copy(
                pairingPayloadText = value,
                gatewayServerVersion = "",
                pairingAllowed = null,
            )
        }
    }

    fun loadPairingPayloadFromQr(value: String) {
        _state.update {
            it.copy(
                pairingPayloadText = value,
                logLines = it.logLines.append(
                    GatewayLogLine(
                        "ack",
                        getApplication<Application>().getString(R.string.qr_scanner_payload_loaded),
                    )
                ),
            )
        }
    }

    fun updateDeviceName(value: String) {
        _state.update { it.copy(deviceName = value) }
    }

    fun updateMessage(value: String) {
        _state.update { it.copy(messageText = value) }
    }

    fun importSharedMessage(value: String) {
        val text = value.trim()
        if (text.isBlank()) return
        _state.update {
            val nextMessage = if (it.messageText.isBlank()) {
                text
            } else {
                it.messageText.trimEnd() + "\n\n" + text
            }
            it.copy(
                messageText = nextMessage,
                logLines = it.logLines.append(
                    GatewayLogLine(
                        "ack",
                        getApplication<Application>().getString(R.string.shared_message_loaded_log),
                    )
                ),
            )
        }
    }

    fun updateFilePath(value: String) {
        _state.update { it.copy(filePathText = value) }
    }

    fun updatePreferredWorkspacePath(value: String) {
        _state.update { it.copy(preferredWorkspacePath = value) }
    }

    fun updateToolAnswer(approvalId: String, value: String) {
        _state.update {
            it.copy(toolAnswers = it.toolAnswers + (approvalId to value))
        }
    }

    fun updateToolConfirmation(approvalId: String, value: String) {
        _state.update {
            it.copy(toolConfirmations = it.toolConfirmations + (approvalId to value))
        }
    }

    fun scanAndPair(payload: String) {
        loadPairingPayloadFromQr(payload)
        pair()
    }

    fun pair() {
        val current = state.value
        viewModelScope.launch {
            runCatching {
                _state.update { it.copy(status = ConnectionStatus.Pairing, errorMessage = null) }
                val payload = parsePairingPayload(current.pairingPayloadText)
                ensurePairingPayloadUsable(payload)
                val reachable = checkFirstReachableHealth(payload.baseUrl, payload.fallbackBaseUrls)
                val health = reachable.health
                updateGatewayHealth(payload, health)
                ensureCompatibleGateway(health)
                val claim = client.claimPairing(payload.copy(baseUrl = reachable.baseUrl), current.deviceName)
                val credential = StoredGatewayCredential(
                    baseUrl = payload.baseUrl,
                    fallbackBaseUrls = payload.fallbackBaseUrls,
                    deviceToken = claim.deviceToken,
                    desktopName = payload.desktopName,
                    protocolVersion = claim.protocolVersion,
                )
                tokenStore.save(credential)
                credential
            }.onSuccess { credential ->
                _state.update {
                    it.copy(
                        baseUrl = credential.baseUrl,
                        fallbackBaseUrls = credential.fallbackBaseUrls,
                        desktopName = credential.desktopName,
                        protocolVersion = credential.protocolVersion,
                        isPaired = true,
                        status = ConnectionStatus.Disconnected,
                        pairingPayloadText = "",
                    )
                }
                connect()
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        status = ConnectionStatus.Error,
                        errorMessage = error.message,
                    )
                }
            }
        }
    }

    fun checkGatewayHealth() {
        viewModelScope.launch {
            runCatching {
                _state.update { it.copy(isCheckingGateway = true, errorMessage = null) }
                state.value.pairingPayloadText
                    .takeIf { it.isNotBlank() }
                    ?.let { ensurePairingPayloadUsable(parsePairingPayload(it)) }
                val target = resolveGatewayHealthTarget(state.value)
                    ?: throw IllegalStateException(
                        getApplication<Application>().getString(R.string.error_gateway_health_target_missing)
                )
                val health = checkFirstReachableHealth(target.baseUrl, target.fallbackBaseUrls).health
                updateGatewayHealth(target, health)
                ensureCompatibleGateway(health, requiresPairingAllowed = target.requiresPairingAllowed)
                health
            }.onSuccess { health ->
                _state.update {
                    it.copy(
                        isCheckingGateway = false,
                        logLines = it.logLines.append(
                            GatewayLogLine(
                                "ack",
                                getApplication<Application>().getString(
                                    R.string.gateway_health_ok_log,
                                    health.serverVersion.ifBlank { health.appVersion },
                                ),
                            )
                        ),
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isCheckingGateway = false,
                        status = ConnectionStatus.Error,
                        errorMessage = error.message,
                    )
                }
            }
        }
    }

    fun connect() {
        val credential = tokenStore.load() ?: return
        manuallyDisconnected = false
        reconnectRequested = true
        socketJob?.cancel()
        _state.update {
            it.copy(
                status = ConnectionStatus.Connecting,
                errorMessage = null,
                baseUrl = credential.baseUrl,
                fallbackBaseUrls = credential.fallbackBaseUrls,
                desktopName = credential.desktopName,
                protocolVersion = credential.protocolVersion,
                isPaired = true,
                reconnectAttempt = 0,
                reconnectDelayMillis = 0,
            )
        }
        socketJob = viewModelScope.launch {
            runConnectionLoop()
        }
    }

    private suspend fun runConnectionLoop() {
        var attempt = 0
        while (reconnectRequested) {
            val credential = tokenStore.load() ?: return
            var terminalRequiresRePair = false
            var terminalMessage: String? = null
            val endpoints = credential.connectionBaseUrls()
            for (endpoint in endpoints) {
                if (!reconnectRequested) return
                _state.update {
                    it.copy(
                        status = if (attempt == 0) ConnectionStatus.Connecting else ConnectionStatus.Reconnecting,
                        errorMessage = null,
                        baseUrl = endpoint,
                        fallbackBaseUrls = credential.fallbackBaseUrls,
                        desktopName = credential.desktopName,
                        protocolVersion = credential.protocolVersion,
                        isPaired = true,
                        reconnectAttempt = attempt,
                        reconnectDelayMillis = if (attempt == 0) 0 else ReconnectPolicy.delayMillisForAttempt(attempt),
                    )
                }

                client.connect(endpoint, credential.deviceToken).collect { event ->
                    when (event) {
                        GatewayEvent.Connected -> {
                            attempt = 0
                            handleGatewayEvent(event)
                        }
                        is GatewayEvent.ConnectionFailed -> {
                            terminalMessage = event.message
                            terminalRequiresRePair = ReconnectPolicy.isAuthenticationRejected(event.httpCode, event.message)
                            handleGatewayEvent(event)
                        }
                        is GatewayEvent.ConnectionClosed -> {
                            terminalMessage = event.reason
                            handleGatewayEvent(event)
                        }
                        else -> handleGatewayEvent(event)
                    }
                }

                if (terminalRequiresRePair || !reconnectRequested) {
                    break
                }
            }

            if (!reconnectRequested) {
                return
            }
            if (terminalRequiresRePair) {
                handleAuthenticationRejected()
                return
            }

            attempt += 1
            if (!ReconnectPolicy.shouldRetry(attempt)) {
                reconnectRequested = false
                _state.update {
                    it.copy(
                        status = ConnectionStatus.Disconnected,
                        errorMessage = getApplication<Application>().getString(R.string.error_reconnect_exhausted),
                        reconnectAttempt = attempt,
                        reconnectDelayMillis = 0,
                    )
                }
                return
            }

            val delayMillis = ReconnectPolicy.delayMillisForAttempt(attempt)
            _state.update {
                it.copy(
                    status = ConnectionStatus.Reconnecting,
                    errorMessage = terminalMessage,
                    reconnectAttempt = attempt,
                    reconnectDelayMillis = delayMillis,
                )
            }
            delay(delayMillis)
        }
    }

    fun disconnect() {
        manuallyDisconnected = true
        reconnectRequested = false
        socketJob?.cancel()
        socketJob = null
        client.disconnect()
        _state.update {
            it.copy(
                status = ConnectionStatus.Disconnected,
                reconnectAttempt = 0,
                reconnectDelayMillis = 0,
            )
        }
    }

    fun forgetToken() {
        manuallyDisconnected = true
        disconnect()
        tokenStore.clear()
        _state.update {
            MobileGatewayUiState(
                deviceName = it.deviceName,
                pairingPayloadText = it.pairingPayloadText,
            )
        }
    }

    fun refreshSessions() {
        send(GatewayEnvelope(type = "session.list"))
    }

    fun resumeConnectionIfAppropriate() {
        if (MobileGatewayConnectionPolicy.shouldAutoResume(
                isPaired = state.value.isPaired,
                status = state.value.status,
                manuallyDisconnected = manuallyDisconnected,
            )
        ) {
            connect()
        }
    }

    fun openWorkspacePicker() {
        if (state.value.status != ConnectionStatus.Connected) {
            connect()
        }
        _state.update {
            it.copy(
                workspacePickerVisible = true,
                workspacePickerError = null,
                isLoadingWorkspaces = true,
            )
        }
        requestWorkspaces()
    }

    fun closeWorkspacePicker() {
        _state.update {
            it.copy(
                workspacePickerVisible = false,
                workspacePickerError = null,
                isLoadingWorkspaces = false,
            )
        }
    }

    fun requestWorkspaces() {
        val sent = send(GatewayEnvelope(type = "workspace.list"))
        if (!sent) {
            _state.update {
                it.copy(
                    isLoadingWorkspaces = false,
                    workspacePickerError = getApplication<Application>().getString(
                        R.string.error_websocket_not_connected,
                    ),
                )
            }
        }
    }

    fun createSession(workspacePath: String): Boolean {
        val path = workspacePath.trim()
        if (path.isEmpty()) {
            _state.update {
                it.copy(
                    workspacePickerError = getApplication<Application>().getString(
                        R.string.error_workspace_required,
                    ),
                )
            }
            return false
        }
        val payload = JSONObject().put("workspace_path", path)
        val sent = send(
            GatewayEnvelope(
                type = "session.create",
                payload = payload,
            )
        )
        if (!sent) return false
        _state.update {
            it.copy(
                preferredWorkspacePath = path,
                recentWorkspacePaths = (listOf(path) + it.recentWorkspacePaths.filter { existing -> existing != path }).take(8),
                workspacePickerVisible = false,
                workspacePickerError = null,
                isLoadingWorkspaces = false,
            )
        }
        return true
    }

    fun selectSession(sessionId: String) {
        _state.update { it.copy(selectedSessionId = sessionId) }
        send(GatewayEnvelope(type = "session.select", sessionId = sessionId))
    }

    fun addAttachment(uri: Uri) {
        val resolver = getApplication<Application>().contentResolver
        val mime = runCatching { resolver.getType(uri) }.getOrNull()
        val size = runCatching {
            resolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
        }.getOrNull()
        _state.update {
            if (it.attachments.any { att -> att.uri == uri }) it
            else it.copy(attachments = it.attachments + ImageAttachment(uri, mime, size))
        }
    }

    fun removeAttachment(uri: Uri) {
        _state.update { it.copy(attachments = it.attachments.filterNot { att -> att.uri == uri }) }
    }

    fun sendMessage() {
        val current = state.value
        val text = current.messageText.trim()
        val attachments = current.attachments
        if (text.isEmpty() && attachments.isEmpty()) return

        val payload = JSONObject().put("text", text)
        val workspacePath = current.preferredWorkspacePath.trim().takeIf { it.isNotEmpty() }
        workspacePath?.let { payload.put("workspace_path", it) }

        if (attachments.isNotEmpty()) {
            val imagesArray = JSONArray()
            val resolver = getApplication<Application>().contentResolver
            attachments.forEach { att ->
                runCatching {
                    val bytes = resolver.openInputStream(att.uri)?.use { it.readBytes() }
                    if (bytes != null && bytes.isNotEmpty()) {
                        val mime = att.mimeType?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
                        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        imagesArray.put(
                            JSONObject()
                                .put("type", "image_url")
                                .put(
                                    "image_url",
                                    JSONObject().put("url", "data:$mime;base64,$b64"),
                                )
                        )
                    }
                }
            }
            if (imagesArray.length() > 0) {
                payload.put("images", imagesArray)
            }
        }

        val activeSession = current.sessions.firstOrNull { it.id == current.selectedSessionId }
        val shouldQueue = activeSession?.isResponding == true && current.selectedSessionId != null

        val envelope = if (shouldQueue) {
            GatewayEnvelope(
                type = "queue.update",
                sessionId = current.selectedSessionId,
                payload = payload.put("action", "add"),
            )
        } else {
            GatewayEnvelope(
                type = "message.send",
                sessionId = current.selectedSessionId.takeIf { workspacePath == null },
                payload = payload,
            )
        }
        val sent = send(envelope)
        if (!sent) return
        val logKind = if (shouldQueue) "mobile" else "mobile"
        val logText = if (shouldQueue) {
            getApplication<Application>().getString(R.string.queue_add_log)
        } else {
            text.ifBlank { "(image)" }
        }
        _state.update {
            it.copy(
                messageText = "",
                attachments = emptyList(),
                logLines = it.logLines.append(GatewayLogLine(logKind, logText)),
            )
        }
    }

    fun stopTurn() {
        send(GatewayEnvelope(type = "turn.stop", sessionId = state.value.selectedSessionId))
    }

    fun queueCurrentMessage() {
        val current = state.value
        val text = current.messageText.trim()
        if (text.isEmpty()) return
        val payload = JSONObject()
            .put("action", "add")
            .put("text", text)
        val sent = send(
            GatewayEnvelope(
                type = "queue.update",
                sessionId = current.selectedSessionId,
                payload = payload,
            )
        )
        if (!sent) return
        _state.update {
            it.copy(
                messageText = "",
                logLines = it.logLines.append(
                    GatewayLogLine(
                        "mobile",
                        getApplication<Application>().getString(R.string.queue_add_log),
                    )
                ),
            )
        }
    }

    fun sendQueuedNow(message: GatewayQueuedMessage) {
        updateQueue(message, "send_now", R.string.queue_send_now_log)
    }

    fun removeQueuedMessage(message: GatewayQueuedMessage) {
        val sent = updateQueue(message, "remove", R.string.queue_remove_log)
        if (!sent) return
        _state.update {
            it.copy(queuedMessages = it.queuedMessages.filterNot { queued -> queued.id == message.id })
        }
    }

    fun clearQueue() {
        val sent = send(
            GatewayEnvelope(
                type = "queue.update",
                sessionId = state.value.selectedSessionId,
                payload = JSONObject().put("action", "clear"),
            )
        )
        if (!sent) return
        _state.update {
            it.copy(
                queuedMessages = emptyList(),
                logLines = it.logLines.append(
                    GatewayLogLine(
                        "mobile",
                        getApplication<Application>().getString(R.string.queue_clear_log),
                    )
                ),
            )
        }
    }

    fun decideTool(approval: PendingToolApproval, approved: Boolean) {
        val decision = if (approved) "approve" else "reject"
        val payload = JSONObject()
            .put("id", approval.id)
            .put("decision", decision)
            .put("approved", approved)
        val answer = state.value.toolAnswers[approval.id].orEmpty().trim()
        if (approved && approval.requiresAnswer) {
            if (answer.isEmpty()) {
                _state.update {
                    it.copy(errorMessage = getApplication<Application>().getString(R.string.error_answer_required))
                }
                return
            }
            payload.put("answer", answer)
        }
        val confirmation = state.value.toolConfirmations[approval.id].orEmpty().trim()
        if (approved && approval.requiresConfirmation) {
            if (!confirmation.equals("confirm", ignoreCase = true)) {
                _state.update {
                    it.copy(errorMessage = getApplication<Application>().getString(R.string.error_confirmation_required))
                }
                return
            }
            payload.put("typed_confirmation", confirmation.lowercase())
        }
        val sent = send(
            GatewayEnvelope(
                type = "tool.decide",
                sessionId = approval.sessionId ?: state.value.selectedSessionId,
                payload = payload,
            )
        )
        if (!sent) return
        _state.update {
            it.copy(
                pendingTools = it.pendingTools.filterNot { item -> item.id == approval.id },
                toolAnswers = it.toolAnswers - approval.id,
                toolConfirmations = it.toolConfirmations - approval.id,
                logLines = it.logLines.append(
                    GatewayLogLine(
                        "mobile",
                        getApplication<Application>().getString(R.string.tool_decision_log, decision),
                    )
                ),
            )
        }
    }

    fun decidePatch(proposal: PatchProposal, approved: Boolean) {
        val decision = if (approved) "approve" else "reject"
        val payload = JSONObject()
            .put("id", proposal.id)
            .put("decision", decision)
            .put("approved", approved)
        val sent = send(
            GatewayEnvelope(
                type = "patch.decide",
                sessionId = proposal.sessionId ?: state.value.selectedSessionId,
                payload = payload,
            )
        )
        if (!sent) return
        _state.update {
            it.copy(
                patchProposals = it.patchProposals.filterNot { item -> item.id == proposal.id },
                patchDiffs = it.patchDiffs - proposal.id,
                logLines = it.logLines.append(
                    GatewayLogLine(
                        "mobile",
                        getApplication<Application>().getString(R.string.patch_decision_log, decision),
                    )
                ),
            )
        }
    }

    fun requestPatchDiff(proposal: PatchProposal) {
        val payload = JSONObject().put("patch_id", proposal.id)
        send(
            GatewayEnvelope(
                type = "diff.get",
                sessionId = proposal.sessionId ?: state.value.selectedSessionId,
                payload = payload,
            )
        )
    }

    fun requestFileRead() {
        val current = state.value
        sendFileRead(current.filePathText, current.selectedSessionId)
    }

    fun requestWorktreeFileRead(file: GatewayWorktreeFile, sessionId: String?) {
        val path = file.path.trim()
        if (path.isEmpty()) return
        _state.update { it.copy(filePathText = path) }
        sendFileRead(path, sessionId ?: state.value.selectedSessionId)
    }

    private fun sendFileRead(pathText: String, sessionId: String?): Boolean {
        val path = pathText.trim()
        if (path.isEmpty()) return false
        val payload = JSONObject()
            .put("path", path)
            .put("max_bytes", 65536)
        return send(
            GatewayEnvelope(
                type = "file.read",
                sessionId = sessionId,
                payload = payload,
            )
        )
    }

    fun killJob(job: GatewayJob) {
        val payload = JSONObject().put("job_id", job.handle.ifBlank { job.id })
        val sent = send(
            GatewayEnvelope(
                type = "job.kill",
                sessionId = job.sessionId ?: state.value.selectedSessionId,
                payload = payload,
            )
        )
        if (!sent) return
        _state.update {
            it.copy(
                logLines = it.logLines.append(
                    GatewayLogLine(
                        "mobile",
                        getApplication<Application>().getString(R.string.job_kill_log, job.handle.ifBlank { job.id }),
                    )
                ),
            )
        }
    }

    private fun updateQueue(message: GatewayQueuedMessage, action: String, logStringId: Int): Boolean {
        val payload = JSONObject()
            .put("action", action)
            .put("message_id", message.id)
        val sent = send(
            GatewayEnvelope(
                type = "queue.update",
                sessionId = message.sessionId ?: state.value.selectedSessionId,
                payload = payload,
            )
        )
        if (!sent) return false
        _state.update {
            it.copy(
                logLines = it.logLines.append(
                    GatewayLogLine(
                        "mobile",
                        getApplication<Application>().getString(logStringId, message.textPreview.ifBlank { message.id }),
                    )
                ),
            )
        }
        return true
    }

    private fun ensureCompatibleGateway(
        health: GatewayHealth,
        requiresPairingAllowed: Boolean = true,
    ) {
        if (health.protocolVersion != MOBILE_GATEWAY_PROTOCOL_VERSION) {
            throw IllegalStateException(
                getApplication<Application>().getString(
                    R.string.error_gateway_protocol_mismatch,
                    health.protocolVersion,
                )
            )
        }
        if (requiresPairingAllowed && !health.pairingAllowed) {
            throw IllegalStateException(
                getApplication<Application>().getString(R.string.error_gateway_pairing_not_allowed)
            )
        }
    }

    private fun ensurePairingPayloadUsable(payload: PairingPayload) {
        if (!payload.hasCompatibleProtocol()) {
            throw IllegalStateException(
                getApplication<Application>().getString(
                    R.string.error_gateway_protocol_mismatch,
                    payload.protocolVersion,
                )
            )
        }
        if (payload.isExpired()) {
            throw IllegalStateException(
                getApplication<Application>().getString(R.string.error_pairing_payload_expired)
            )
        }
    }

    private fun parsePairingPayload(raw: String): PairingPayload {
        return try {
            PairingPayload.parse(raw)
        } catch (_: InvalidPairingPayloadException) {
            throw IllegalStateException(
                getApplication<Application>().getString(R.string.error_pairing_payload_invalid)
            )
        }
    }

    private suspend fun checkFirstReachableHealth(
        baseUrl: String,
        fallbackBaseUrls: List<String>,
    ): ReachableGateway {
        var lastError: Throwable? = null
        com.willdeep.android.mobile.connectionBaseUrls(baseUrl, fallbackBaseUrls).forEach { candidate ->
            runCatching { client.checkHealth(candidate) }
                .onSuccess { health -> return ReachableGateway(candidate, health) }
                .onFailure { lastError = it }
        }
        throw lastError ?: IllegalStateException(
            getApplication<Application>().getString(R.string.error_gateway_health_target_missing)
        )
    }

    private fun updateGatewayHealth(payload: PairingPayload, health: GatewayHealth) {
        updateGatewayHealth(
            GatewayHealthTarget(
                baseUrl = payload.baseUrl,
                fallbackBaseUrls = payload.fallbackBaseUrls,
                desktopName = payload.desktopName,
                protocolVersion = payload.protocolVersion,
                requiresPairingAllowed = true,
            ),
            health,
        )
    }

    private fun updateGatewayHealth(target: GatewayHealthTarget, health: GatewayHealth) {
        _state.update {
            it.copy(
                baseUrl = target.baseUrl,
                fallbackBaseUrls = target.fallbackBaseUrls,
                desktopName = target.desktopName,
                protocolVersion = health.protocolVersion.ifBlank { target.protocolVersion },
                gatewayServerVersion = health.serverVersion.ifBlank { health.appVersion },
                pairingAllowed = health.pairingAllowed,
            )
        }
    }

    override fun onCleared() {
        client.disconnect()
        super.onCleared()
    }

    private fun send(envelope: GatewayEnvelope): Boolean {
        val sent = client.sendCommand(envelope)
        _state.update {
            val commandStatus = if (sent) {
                MobileCommandStatus(
                    id = envelope.id,
                    type = envelope.type,
                    state = MobileCommandState.Pending,
                )
            } else {
                MobileCommandStatus(
                    id = envelope.id,
                    type = envelope.type,
                    state = MobileCommandState.Failed,
                    detail = getApplication<Application>().getString(R.string.error_websocket_not_connected),
                )
            }
            val updated = it.copy(
                commandStatuses = it.commandStatuses.upsertCommandStatus(commandStatus),
            )
            if (!sent) {
                updated.copy(
                    status = ConnectionStatus.Error,
                    errorMessage = getApplication<Application>().getString(R.string.error_websocket_not_connected),
                )
            } else {
                updated
            }
        }
        return sent
    }

    private fun handleGatewayEvent(event: GatewayEvent) {
        when (event) {
            GatewayEvent.Connected -> {
                _state.update {
                    it.copy(
                        status = ConnectionStatus.Connected,
                        errorMessage = null,
                        reconnectAttempt = 0,
                        reconnectDelayMillis = 0,
                    )
                }
            }
            GatewayEvent.Disconnected -> {
                _state.update {
                    if (it.status == ConnectionStatus.Error) {
                        it
                    } else {
                        it.copy(status = ConnectionStatus.Disconnected)
                    }
                }
            }
            is GatewayEvent.ConnectionClosed -> {
                _state.update {
                    it.copy(
                        status = ConnectionStatus.Disconnected,
                        errorMessage = event.reason.ifBlank { null },
                        logLines = it.logLines.append(
                            GatewayLogLine(
                                "error",
                                getApplication<Application>().getString(R.string.websocket_closed_log, event.code),
                            )
                        ),
                    )
                }
            }
            is GatewayEvent.ConnectionFailed -> {
                _state.update {
                    it.copy(
                        errorMessage = event.message,
                        logLines = it.logLines.append(
                            GatewayLogLine(
                                "error",
                                getApplication<Application>().getString(R.string.websocket_failed_log, event.message),
                            )
                        ),
                    )
                }
            }
            is GatewayEvent.Snapshot -> {
                _state.update {
                    it.copy(
                        sessions = event.sessions,
                        selectedSessionId = event.activeSessionId ?: it.selectedSessionId ?: event.sessions.firstOrNull()?.id,
                        pendingTools = event.pendingTools,
                        toolAnswers = it.toolAnswers.keepAnswersFor(event.pendingTools),
                        toolConfirmations = it.toolConfirmations.keepConfirmationsFor(event.pendingTools),
                        patchProposals = event.patchProposals,
                        patchDiffs = it.patchDiffs.filterKeys { patchId ->
                            event.patchProposals.any { proposal -> proposal.id == patchId }
                        },
                        jobs = event.jobs,
                        queuedMessages = event.queuedMessages,
                        conversationMessages = event.messages.filterForSession(
                            event.activeSessionId ?: it.selectedSessionId,
                        ),
                        worktree = event.worktrees.forSession(event.activeSessionId ?: it.selectedSessionId),
                        status = ConnectionStatus.Connected,
                    )
                }
            }
            is GatewayEvent.SessionUpsert -> {
                _state.update {
                    val sessions = (it.sessions.filterNot { session -> session.id == event.session.id } + event.session)
                        .sortedByDescending { session -> session.isActive }
                    it.copy(
                        sessions = sessions,
                        selectedSessionId = event.session.id,
                        status = ConnectionStatus.Connected,
                    )
                }
            }
            is GatewayEvent.Ack -> {
                _state.update {
                    it.copy(
                        selectedSessionId = event.sessionId ?: it.selectedSessionId,
                        commandStatuses = it.commandStatuses.markCommandAccepted(
                            commandId = event.commandId,
                            commandType = event.commandType,
                        ),
                        logLines = it.logLines.append(GatewayLogLine("ack", event.commandType)),
                    )
                }
            }
            is GatewayEvent.Error -> {
                _state.update {
                    it.copy(
                        status = ConnectionStatus.Error,
                        errorMessage = event.message,
                        commandStatuses = it.commandStatuses.markCommandFailed(
                            commandId = event.commandId,
                            message = event.message,
                        ),
                        logLines = it.logLines.append(GatewayLogLine("error", event.message)),
                    )
                }
            }
            is GatewayEvent.MessageDelta -> {
                _state.update {
                    val sessionId = event.sessionId ?: it.selectedSessionId
                    it.copy(
                        conversationMessages = it.conversationMessages.appendDelta(
                            sessionId = sessionId,
                            messageId = event.messageId,
                            text = event.text,
                        ),
                        logLines = it.logLines.append(GatewayLogLine("mac", event.text)),
                    )
                }
            }
            is GatewayEvent.MessageAppend -> {
                _state.update {
                    val sessionId = event.message.sessionId ?: it.selectedSessionId
                    it.copy(
                        conversationMessages = (it.conversationMessages
                            .filterNot { message -> message.id == event.message.id } + event.message)
                            .filterForSession(sessionId),
                    )
                }
            }
            is GatewayEvent.MessageDone -> {
                _state.update {
                    it.copy(
                        conversationMessages = it.conversationMessages.markMessageDone(
                            sessionId = event.sessionId ?: it.selectedSessionId,
                            messageId = event.messageId,
                        ),
                    )
                }
            }
            is GatewayEvent.WorktreeUpdated -> {
                _state.update {
                    it.copy(
                        worktree = event.worktree,
                        logLines = it.logLines.append(
                            GatewayLogLine(
                                "mac",
                                getApplication<Application>().getString(
                                    R.string.worktree_updated_log,
                                    event.worktree.fileCount,
                                ),
                            )
                        ),
                    )
                }
            }
            is GatewayEvent.ToolPending -> {
                _state.update {
                    it.copy(
                        pendingTools = (it.pendingTools.filterNot { item -> item.id == event.approval.id } + event.approval),
                        toolAnswers = if (event.approval.requiresAnswer) {
                            it.toolAnswers + (event.approval.id to it.toolAnswers[event.approval.id].orEmpty())
                        } else {
                            it.toolAnswers - event.approval.id
                        },
                        toolConfirmations = if (event.approval.requiresConfirmation) {
                            it.toolConfirmations + (event.approval.id to it.toolConfirmations[event.approval.id].orEmpty())
                        } else {
                            it.toolConfirmations - event.approval.id
                        },
                        logLines = it.logLines.append(GatewayLogLine("mac", event.approval.title)),
                    )
                }
            }
            is GatewayEvent.ToolUpdated -> {
                _state.update {
                    it.removeToolApproval(event.id)
                }
            }
            is GatewayEvent.PatchUpsert -> {
                _state.update {
                    it.copy(
                        patchProposals = (it.patchProposals.filterNot { item -> item.id == event.proposal.id } + event.proposal),
                        logLines = it.logLines.append(GatewayLogLine("mac", event.proposal.title)),
                    )
                }
            }
            is GatewayEvent.PatchUpdated -> {
                _state.update {
                    it.removePatchProposal(event.id)
                }
            }
            is GatewayEvent.PatchDiffLoaded -> {
                _state.update {
                    it.copy(
                        patchDiffs = it.patchDiffs + (event.diff.patchId to event.diff),
                        commandStatuses = it.commandStatuses.markCommandAccepted(
                            commandId = event.commandId,
                            commandType = "diff.get",
                        ),
                        logLines = it.logLines.append(
                            GatewayLogLine(
                                "ack",
                                getApplication<Application>().getString(R.string.patch_diff_loaded_log, event.diff.title),
                            )
                        ),
                    )
                }
            }
            is GatewayEvent.JobUpdated -> {
                _state.update {
                    it.copy(
                        jobs = (it.jobs.filterNot { job -> job.id == event.job.id } + event.job)
                            .sortedWith(compareByDescending<GatewayJob> { it.isAlive }.thenBy { it.handle }),
                        logLines = it.logLines.append(GatewayLogLine("mac", event.job.handle)),
                    )
                }
            }
            is GatewayEvent.FileLoaded -> {
                _state.update {
                    it.copy(
                        loadedFile = event.file,
                        filePathText = event.file.path.ifBlank { it.filePathText },
                        commandStatuses = it.commandStatuses.markCommandAccepted(
                            commandId = event.commandId,
                            commandType = "file.read",
                        ),
                        logLines = it.logLines.append(
                            GatewayLogLine(
                                "ack",
                                getApplication<Application>().getString(R.string.file_read_loaded_log, event.file.path),
                            )
                        ),
                    )
                }
            }
            is GatewayEvent.WorkspacesUpdated -> {
                _state.update {
                    it.copy(
                        workspaces = event.workspaces,
                        isLoadingWorkspaces = false,
                        workspacePickerError = null,
                    )
                }
            }
            is GatewayEvent.Raw -> Unit
        }
    }

    private fun handleAuthenticationRejected() {
        reconnectRequested = false
        socketJob = null
        manuallyDisconnected = false
        client.disconnect()
        tokenStore.clear()
        _state.update {
            it.copy(
                isPaired = false,
                status = ConnectionStatus.Error,
                errorMessage = getApplication<Application>().getString(R.string.error_device_revoked),
                reconnectAttempt = 0,
                reconnectDelayMillis = 0,
                logLines = it.logLines.append(
                    GatewayLogLine(
                        "error",
                        getApplication<Application>().getString(R.string.error_device_revoked),
                    )
                ),
            )
        }
    }
}

private fun List<GatewayLogLine>.append(line: GatewayLogLine): List<GatewayLogLine> {
    return (this + line).takeLast(80)
}

internal fun List<MobileCommandStatus>.upsertCommandStatus(
    status: MobileCommandStatus,
): List<MobileCommandStatus> {
    return (filterNot { it.id == status.id } + status).takeLast(20)
}

internal fun List<MobileCommandStatus>.markCommandAccepted(
    commandId: String?,
    commandType: String,
): List<MobileCommandStatus> {
    val index = indexForCommand(commandId, commandType)
    if (index < 0) {
        return upsertCommandStatus(
            MobileCommandStatus(
                id = commandId ?: "ack-$commandType-${size + 1}",
                type = commandType,
                state = MobileCommandState.Accepted,
            )
        )
    }
    return mapIndexed { currentIndex, status ->
        if (currentIndex == index) {
            status.copy(
                type = commandType.ifBlank { status.type },
                state = MobileCommandState.Accepted,
                detail = "",
            )
        } else {
            status
        }
    }
}

internal fun List<MobileCommandStatus>.markCommandFailed(
    commandId: String?,
    message: String,
): List<MobileCommandStatus> {
    val index = indexForCommand(commandId, commandType = "")
    if (index < 0) {
        return upsertCommandStatus(
            MobileCommandStatus(
                id = commandId ?: "error-${size + 1}",
                type = "command",
                state = MobileCommandState.Failed,
                detail = message,
            )
        )
    }
    return mapIndexed { currentIndex, status ->
        if (currentIndex == index) {
            status.copy(state = MobileCommandState.Failed, detail = message)
        } else {
            status
        }
    }
}

private fun List<MobileCommandStatus>.indexForCommand(
    commandId: String?,
    commandType: String,
): Int {
    if (!commandId.isNullOrBlank()) {
        val idIndex = indexOfLast { it.id == commandId }
        if (idIndex >= 0) return idIndex
    }
    if (commandType.isNotBlank()) {
        return indexOfLast { it.type == commandType && it.state == MobileCommandState.Pending }
    }
    return indexOfLast { it.state == MobileCommandState.Pending }
}

internal fun resolveGatewayHealthTarget(state: MobileGatewayUiState): GatewayHealthTarget? {
    if (state.pairingPayloadText.isNotBlank()) {
        val payload = PairingPayload.parse(state.pairingPayloadText)
        return GatewayHealthTarget(
            baseUrl = payload.baseUrl,
            fallbackBaseUrls = payload.fallbackBaseUrls,
            desktopName = payload.desktopName,
            protocolVersion = payload.protocolVersion,
            requiresPairingAllowed = true,
        )
    }
    if (state.baseUrl.isBlank()) {
        return null
    }
    return GatewayHealthTarget(
        baseUrl = state.baseUrl,
        fallbackBaseUrls = state.fallbackBaseUrls,
        desktopName = state.desktopName,
        protocolVersion = state.protocolVersion,
        requiresPairingAllowed = false,
    )
}

internal fun List<GatewayMessage>.filterForSession(sessionId: String?): List<GatewayMessage> {
    if (sessionId.isNullOrBlank()) {
        return takeLast(80)
    }
    return filter { it.sessionId == null || it.sessionId == sessionId }.takeLast(80)
}

internal fun List<GatewayMessage>.appendDelta(
    sessionId: String?,
    messageId: String?,
    text: String,
): List<GatewayMessage> {
    if (text.isBlank()) {
        return this
    }
    val targetIndex = when {
        !messageId.isNullOrBlank() -> indexOfLast { it.id == messageId }
        else -> indexOfLast { it.role == "assistant" && (sessionId == null || it.sessionId == sessionId) }
    }
    if (targetIndex >= 0) {
        return mapIndexed { index, message ->
            if (index == targetIndex) {
                message.copy(content = message.content + text, isStreaming = true)
            } else {
                message
            }
        }.filterForSession(sessionId)
    }
    return (this + GatewayMessage(
        id = messageId ?: "stream-${System.nanoTime()}",
        role = "assistant",
        content = text,
        createdAt = "",
        sessionId = sessionId,
        isStreaming = true,
    )).filterForSession(sessionId)
}

internal fun List<GatewayMessage>.markMessageDone(
    sessionId: String?,
    messageId: String?,
): List<GatewayMessage> {
    val targetIndex = when {
        !messageId.isNullOrBlank() -> indexOfLast { it.id == messageId }
        else -> indexOfLast { it.role == "assistant" && (sessionId == null || it.sessionId == sessionId) }
    }
    if (targetIndex < 0) {
        return filterForSession(sessionId)
    }
    return mapIndexed { index, message ->
        if (index == targetIndex) {
            message.copy(isStreaming = false)
        } else {
            message
        }
    }.filterForSession(sessionId)
}

internal fun Map<String, String>.keepAnswersFor(approvals: List<PendingToolApproval>): Map<String, String> {
    return approvals
        .filter { it.requiresAnswer }
        .associate { approval -> approval.id to this[approval.id].orEmpty() }
}

internal fun Map<String, String>.keepConfirmationsFor(approvals: List<PendingToolApproval>): Map<String, String> {
    return approvals
        .filter { it.requiresConfirmation }
        .associate { approval -> approval.id to this[approval.id].orEmpty() }
}

internal fun MobileGatewayUiState.removeToolApproval(approvalId: String): MobileGatewayUiState {
    return copy(
        pendingTools = pendingTools.filterNot { item -> item.id == approvalId },
        toolAnswers = toolAnswers - approvalId,
        toolConfirmations = toolConfirmations - approvalId,
    )
}

internal fun MobileGatewayUiState.removePatchProposal(patchId: String): MobileGatewayUiState {
    return copy(
        patchProposals = patchProposals.filterNot { item -> item.id == patchId },
        patchDiffs = patchDiffs - patchId,
    )
}

private fun List<GatewayWorktree>.forSession(sessionId: String?): GatewayWorktree? {
    if (isEmpty()) return null
    if (sessionId.isNullOrBlank()) return firstOrNull()
    return firstOrNull { it.sessionId == sessionId } ?: firstOrNull()
}
