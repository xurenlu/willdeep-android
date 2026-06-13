package com.willdeep.android.ui

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.willdeep.android.R
import com.willdeep.android.mobile.DeviceTokenStore
import com.willdeep.android.mobile.GatewayEnvelope
import com.willdeep.android.mobile.GatewayEvent
import com.willdeep.android.mobile.GatewayFile
import com.willdeep.android.mobile.GatewayJob
import com.willdeep.android.mobile.GatewayMessage
import com.willdeep.android.mobile.GatewayQueuedMessage
import com.willdeep.android.mobile.GatewaySession
import com.willdeep.android.mobile.MobileGatewayClient
import com.willdeep.android.mobile.PatchDiff
import com.willdeep.android.mobile.PatchProposal
import com.willdeep.android.mobile.PairingPayload
import com.willdeep.android.mobile.PendingToolApproval
import com.willdeep.android.mobile.StoredGatewayCredential
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class ConnectionStatus {
    Idle,
    Pairing,
    Connecting,
    Connected,
    Disconnected,
    Error,
}

data class GatewayLogLine(
    val kind: String,
    val text: String,
)

data class MobileGatewayUiState(
    val pairingPayloadText: String = "",
    val deviceName: String = Build.MODEL ?: "Android Device",
    val baseUrl: String = "",
    val desktopName: String = "",
    val protocolVersion: String = "",
    val isPaired: Boolean = false,
    val status: ConnectionStatus = ConnectionStatus.Idle,
    val errorMessage: String? = null,
    val filePathText: String = "",
    val loadedFile: GatewayFile? = null,
    val sessions: List<GatewaySession> = emptyList(),
    val selectedSessionId: String? = null,
    val messageText: String = "",
    val pendingTools: List<PendingToolApproval> = emptyList(),
    val toolAnswers: Map<String, String> = emptyMap(),
    val patchProposals: List<PatchProposal> = emptyList(),
    val patchDiffs: Map<String, PatchDiff> = emptyMap(),
    val jobs: List<GatewayJob> = emptyList(),
    val queuedMessages: List<GatewayQueuedMessage> = emptyList(),
    val conversationMessages: List<GatewayMessage> = emptyList(),
    val logLines: List<GatewayLogLine> = emptyList(),
)

class MobileGatewayViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenStore = DeviceTokenStore(application)
    private val client = MobileGatewayClient()
    private var socketJob: Job? = null

    private val _state = MutableStateFlow(MobileGatewayUiState())
    val state: StateFlow<MobileGatewayUiState> = _state.asStateFlow()

    init {
        tokenStore.load()?.let { credential ->
            _state.update {
                it.copy(
                    baseUrl = credential.baseUrl,
                    desktopName = credential.desktopName,
                    protocolVersion = credential.protocolVersion,
                    isPaired = true,
                    status = ConnectionStatus.Disconnected,
                )
            }
        }
    }

    fun updatePairingPayload(value: String) {
        _state.update { it.copy(pairingPayloadText = value) }
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

    fun updateFilePath(value: String) {
        _state.update { it.copy(filePathText = value) }
    }

    fun updateToolAnswer(approvalId: String, value: String) {
        _state.update {
            it.copy(toolAnswers = it.toolAnswers + (approvalId to value))
        }
    }

    fun pair() {
        val current = state.value
        viewModelScope.launch {
            runCatching {
                _state.update { it.copy(status = ConnectionStatus.Pairing, errorMessage = null) }
                val payload = PairingPayload.parse(current.pairingPayloadText)
                val claim = client.claimPairing(payload, current.deviceName)
                val credential = StoredGatewayCredential(
                    baseUrl = payload.baseUrl,
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

    fun connect() {
        val credential = tokenStore.load() ?: return
        socketJob?.cancel()
        _state.update {
            it.copy(
                status = ConnectionStatus.Connecting,
                errorMessage = null,
                baseUrl = credential.baseUrl,
                desktopName = credential.desktopName,
                protocolVersion = credential.protocolVersion,
                isPaired = true,
            )
        }
        socketJob = viewModelScope.launch {
            client.connect(credential.baseUrl, credential.deviceToken).collect { event ->
                handleGatewayEvent(event)
            }
        }
    }

    fun disconnect() {
        socketJob?.cancel()
        socketJob = null
        client.disconnect()
        _state.update { it.copy(status = ConnectionStatus.Disconnected) }
    }

    fun forgetToken() {
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

    fun createSession() {
        send(GatewayEnvelope(type = "session.create"))
    }

    fun selectSession(sessionId: String) {
        _state.update { it.copy(selectedSessionId = sessionId) }
        send(GatewayEnvelope(type = "session.select", sessionId = sessionId))
    }

    fun sendMessage() {
        val current = state.value
        val text = current.messageText.trim()
        if (text.isEmpty()) return
        val payload = JSONObject().put("text", text)
        send(
            GatewayEnvelope(
                type = "message.send",
                sessionId = current.selectedSessionId,
                payload = payload,
            )
        )
        _state.update {
            it.copy(
                messageText = "",
                logLines = it.logLines.append(GatewayLogLine("mobile", text)),
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
        send(
            GatewayEnvelope(
                type = "queue.update",
                sessionId = current.selectedSessionId,
                payload = payload,
            )
        )
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
        updateQueue(message, "remove", R.string.queue_remove_log)
        _state.update {
            it.copy(queuedMessages = it.queuedMessages.filterNot { queued -> queued.id == message.id })
        }
    }

    fun clearQueue() {
        send(
            GatewayEnvelope(
                type = "queue.update",
                sessionId = state.value.selectedSessionId,
                payload = JSONObject().put("action", "clear"),
            )
        )
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
        send(
            GatewayEnvelope(
                type = "tool.decide",
                sessionId = approval.sessionId ?: state.value.selectedSessionId,
                payload = payload,
            )
        )
        _state.update {
            it.copy(
                pendingTools = it.pendingTools.filterNot { item -> item.id == approval.id },
                toolAnswers = it.toolAnswers - approval.id,
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
        send(
            GatewayEnvelope(
                type = "patch.decide",
                sessionId = proposal.sessionId ?: state.value.selectedSessionId,
                payload = payload,
            )
        )
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
        val path = current.filePathText.trim()
        if (path.isEmpty()) return
        val payload = JSONObject()
            .put("path", path)
            .put("max_bytes", 65536)
        send(
            GatewayEnvelope(
                type = "file.read",
                sessionId = current.selectedSessionId,
                payload = payload,
            )
        )
    }

    fun killJob(job: GatewayJob) {
        val payload = JSONObject().put("job_id", job.handle.ifBlank { job.id })
        send(
            GatewayEnvelope(
                type = "job.kill",
                sessionId = job.sessionId ?: state.value.selectedSessionId,
                payload = payload,
            )
        )
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

    private fun updateQueue(message: GatewayQueuedMessage, action: String, logStringId: Int) {
        val payload = JSONObject()
            .put("action", action)
            .put("message_id", message.id)
        send(
            GatewayEnvelope(
                type = "queue.update",
                sessionId = message.sessionId ?: state.value.selectedSessionId,
                payload = payload,
            )
        )
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
    }

    override fun onCleared() {
        client.disconnect()
        super.onCleared()
    }

    private fun send(envelope: GatewayEnvelope) {
        val sent = client.sendCommand(envelope)
        if (!sent) {
            _state.update {
                it.copy(
                    status = ConnectionStatus.Error,
                    errorMessage = getApplication<Application>().getString(R.string.error_websocket_not_connected),
                )
            }
        }
    }

    private fun handleGatewayEvent(event: GatewayEvent) {
        when (event) {
            GatewayEvent.Connected -> {
                _state.update { it.copy(status = ConnectionStatus.Connected, errorMessage = null) }
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
            is GatewayEvent.Snapshot -> {
                _state.update {
                    it.copy(
                        sessions = event.sessions,
                        selectedSessionId = event.activeSessionId ?: it.selectedSessionId ?: event.sessions.firstOrNull()?.id,
                        jobs = event.jobs,
                        queuedMessages = event.queuedMessages,
                        conversationMessages = event.messages.filterForSession(
                            event.activeSessionId ?: it.selectedSessionId,
                        ),
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
                        logLines = it.logLines.append(GatewayLogLine("ack", event.commandType)),
                    )
                }
            }
            is GatewayEvent.Error -> {
                _state.update {
                    it.copy(
                        status = ConnectionStatus.Error,
                        errorMessage = event.message,
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
            is GatewayEvent.MessageDone -> Unit
            is GatewayEvent.ToolPending -> {
                _state.update {
                    it.copy(
                        pendingTools = (it.pendingTools.filterNot { item -> item.id == event.approval.id } + event.approval),
                        toolAnswers = if (event.approval.requiresAnswer) {
                            it.toolAnswers + (event.approval.id to it.toolAnswers[event.approval.id].orEmpty())
                        } else {
                            it.toolAnswers - event.approval.id
                        },
                        logLines = it.logLines.append(GatewayLogLine("mac", event.approval.title)),
                    )
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
            is GatewayEvent.PatchDiffLoaded -> {
                _state.update {
                    it.copy(
                        patchDiffs = it.patchDiffs + (event.diff.patchId to event.diff),
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
                        logLines = it.logLines.append(
                            GatewayLogLine(
                                "ack",
                                getApplication<Application>().getString(R.string.file_read_loaded_log, event.file.path),
                            )
                        ),
                    )
                }
            }
            is GatewayEvent.Raw -> Unit
        }
    }
}

private fun List<GatewayLogLine>.append(line: GatewayLogLine): List<GatewayLogLine> {
    return (this + line).takeLast(80)
}

private fun List<GatewayMessage>.filterForSession(sessionId: String?): List<GatewayMessage> {
    if (sessionId.isNullOrBlank()) {
        return takeLast(80)
    }
    return filter { it.sessionId == null || it.sessionId == sessionId }.takeLast(80)
}

private fun List<GatewayMessage>.appendDelta(
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
                message.copy(content = message.content + text)
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
    )).filterForSession(sessionId)
}
