package com.willdeep.android.mobile

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import com.willdeep.android.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class MobileGatewayClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build(),
) {
    private var webSocket: WebSocket? = null

    suspend fun checkHealth(baseUrl: String): GatewayHealth {
        val request = Request.Builder()
            .url(baseUrl.endpoint("mobile/health"))
            .get()
            .header("X-App-Version", BuildConfig.VERSION_NAME)
            .build()
        return withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute().use { response ->
                parseGatewayHealth(
                    raw = response.requireBody(),
                    appVersionHeader = response.header("X-App-Version").orEmpty(),
                    serverVersionHeader = response.header("X-Server-Version").orEmpty(),
                )
            }
        }
    }

    suspend fun claimPairing(payload: PairingPayload, deviceName: String): PairingClaim {
        val body = JSONObject()
            .put("pairing_token", payload.pairingToken)
            .put("device_name", deviceName.ifBlank { "Android Device" })
            .toString()
            .toRequestBody(JSON)
        val request = Request.Builder()
            .url(payload.baseUrl.endpoint("mobile/pair/claim"))
            .post(body)
            .header("X-App-Version", BuildConfig.VERSION_NAME)
            .build()
        return withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute().use { response ->
                parsePairingClaim(response.requireBody())
            }
        }
    }

    fun connect(baseUrl: String, deviceToken: String, relayRoom: String? = null): Flow<GatewayEvent> = callbackFlow {
        val path = relayRoom
            ?.trim()
            ?.trim('/')
            ?.takeIf { it.isNotBlank() }
            ?.let { "ws/broadcast/$it" }
            ?: "mobile/ws"
        val request = Request.Builder()
            .url(baseUrl.wsEndpoint(path))
            .header("Authorization", "Bearer $deviceToken")
            .header("X-App-Version", BuildConfig.VERSION_NAME)
            .build()
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                this@MobileGatewayClient.webSocket = webSocket
                trySend(GatewayEvent.Connected)
                webSocket.send(GatewayEnvelope(type = "session.list").toJsonString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching { Log.d("GatewayWs", "recv: " + text.take(2000)) }
                runCatching { parseGatewayEvent(text) }
                    .onSuccess { trySend(it) }
                    .onFailure {
                        runCatching { Log.e("GatewayWs", "parse failed: ${it.message}", it) }
                        trySend(GatewayEvent.Error(null, it.message ?: "Invalid gateway event"))
                    }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                trySend(GatewayEvent.ConnectionFailed(t.message ?: "WebSocket failed", response?.code))
                close()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                trySend(GatewayEvent.ConnectionClosed(code, reason))
                close()
            }
        }
        val socket = httpClient.newWebSocket(request, listener)
        webSocket = socket
        awaitClose {
            socket.close(1000, "Android client closed")
            if (webSocket === socket) {
                webSocket = null
            }
        }
    }

    fun sendCommand(envelope: GatewayEnvelope): Boolean {
        return webSocket?.send(envelope.toJsonString()) == true
    }

    fun disconnect() {
        webSocket?.close(1000, "Android client disconnected")
        webSocket = null
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

private fun parseGatewayHealth(
    raw: String,
    appVersionHeader: String,
    serverVersionHeader: String,
): GatewayHealth {
    val data = JSONObject(raw).asApiData()
    val versionValue = data.optJSONObject("version")?.optString("version").orEmpty()
        .ifBlank { data.optString("version") }
    return GatewayHealth(
        status = data.optString("status", "ok"),
        appVersion = appVersionHeader.ifBlank { versionValue },
        serverVersion = serverVersionHeader.ifBlank { versionValue },
        protocolVersion = data.optString("protocol_version", MOBILE_GATEWAY_PROTOCOL_VERSION),
        pairingAllowed = data.optBoolean("pairing_allowed", false),
    )
}

private fun Response.requireBody(): String {
    val text = body?.string().orEmpty()
    if (!isSuccessful) {
        val message = runCatching { JSONObject(text).optString("error") }.getOrNull()
        throw IOException(message?.ifBlank { "HTTP $code" } ?: "HTTP $code")
    }
    return text
}

private fun String.endpoint(path: String): String {
    return trimEnd('/') + "/" + path.trimStart('/')
}

private fun String.wsEndpoint(path: String): String {
    val http = endpoint(path)
    return when {
        http.startsWith("https://") -> "wss://" + http.removePrefix("https://")
        http.startsWith("http://") -> "ws://" + http.removePrefix("http://")
        else -> http
    }
}
