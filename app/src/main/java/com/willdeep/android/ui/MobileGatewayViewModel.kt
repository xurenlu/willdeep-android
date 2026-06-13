package com.willdeep.android.ui

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.willdeep.android.R
import com.willdeep.android.mobile.DeviceTokenStore
import com.willdeep.android.mobile.GatewayEnvelope
import com.willdeep.android.mobile.GatewayEvent
import com.willdeep.android.mobile.GatewaySession
import com.willdeep.android.mobile.MobileGatewayClient
import com.willdeep.android.mobile.PairingPayload
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
    val sessions: List<GatewaySession> = emptyList(),
    val selectedSessionId: String? = null,
    val messageText: String = "",
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

    fun updateDeviceName(value: String) {
        _state.update { it.copy(deviceName = value) }
    }

    fun updateMessage(value: String) {
        _state.update { it.copy(messageText = value) }
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
                    it.copy(logLines = it.logLines.append(GatewayLogLine("mac", event.text)))
                }
            }
            is GatewayEvent.Raw -> Unit
        }
    }
}

private fun List<GatewayLogLine>.append(line: GatewayLogLine): List<GatewayLogLine> {
    return (this + line).takeLast(80)
}
