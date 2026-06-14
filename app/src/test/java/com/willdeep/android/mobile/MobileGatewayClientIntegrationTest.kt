package com.willdeep.android.mobile

import com.willdeep.android.BuildConfig
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.experimental.xor

class MobileGatewayClientIntegrationTest {
    @Test
    fun claimPairingSendsVersionHeaderAndParsesDeviceToken() = runBlocking {
        TestGateway().use { gateway ->
            val client = MobileGatewayClient()

            val claim = client.claimPairing(gateway.pairingPayload(), "Pixel Test")

            assertEquals("device_mock", claim.device.id)
            assertEquals("Pixel Test", claim.device.name)
            assertEquals(gateway.deviceToken, claim.deviceToken)
            assertEquals(MOBILE_GATEWAY_PROTOCOL_VERSION, claim.protocolVersion)
            assertEquals(BuildConfig.VERSION_NAME, gateway.pairClaimHeaders.single().getValue("x-app-version"))
        }
    }

    @Test
    fun checkHealthSendsVersionHeaderAndParsesGatewayStatus() = runBlocking {
        TestGateway().use { gateway ->
            val client = MobileGatewayClient()

            val health = client.checkHealth(gateway.baseUrl)

            assertEquals("ok", health.status)
            assertEquals(BuildConfig.VERSION_NAME, health.appVersion)
            assertEquals(BuildConfig.VERSION_NAME, health.serverVersion)
            assertEquals(MOBILE_GATEWAY_PROTOCOL_VERSION, health.protocolVersion)
            assertTrue(health.pairingAllowed)
            assertEquals(BuildConfig.VERSION_NAME, gateway.healthHeaders.single().getValue("x-app-version"))
        }
    }

    @Test
    fun connectAuthenticatesAndReceivesSnapshotAndSessionListAck() = runBlocking {
        TestGateway().use { gateway ->
            val client = MobileGatewayClient()

            val events = withTimeout(5_000) {
                client.connect(gateway.baseUrl, gateway.deviceToken).take(3).toList()
            }

            assertTrue(events[0] is GatewayEvent.Connected)
            val snapshot = events.filterIsInstance<GatewayEvent.Snapshot>().single()
            val ack = events.filterIsInstance<GatewayEvent.Ack>().single()
            assertEquals("s1", snapshot.activeSessionId)
            assertEquals("Mock coding session", snapshot.sessions.single().title)
            assertEquals("app/src/main/java/com/willdeep/android/ui/WillDeepApp.kt", snapshot.worktrees.single().files.single().path)
            assertEquals("session.list", ack.commandType)
            assertTrue(gateway.sessionListLatch.await(2, TimeUnit.SECONDS))
            assertEquals("Bearer ${gateway.deviceToken}", gateway.websocketHeaders.single().getValue("authorization"))
            assertEquals(BuildConfig.VERSION_NAME, gateway.websocketHeaders.single().getValue("x-app-version"))
        }
    }
}

private class TestGateway : Closeable {
    val deviceToken = "device_test_token"
    val baseUrl: String
    val healthHeaders = CopyOnWriteArrayList<Map<String, String>>()
    val pairClaimHeaders = CopyOnWriteArrayList<Map<String, String>>()
    val websocketHeaders = CopyOnWriteArrayList<Map<String, String>>()
    val sessionListLatch = CountDownLatch(1)

    private val server = ServerSocket(0)
    private val threads = CopyOnWriteArrayList<Thread>()
    private var running = true

    init {
        baseUrl = "http://127.0.0.1:${server.localPort}"
        threads += thread(start = true, name = "test-mobile-gateway") {
            while (running) {
                val socket = runCatching { server.accept() }.getOrNull() ?: break
                threads += thread(start = true, name = "test-mobile-gateway-client") {
                    socket.use { handle(it) }
                }
            }
        }
    }

    fun pairingPayload(): PairingPayload {
        return PairingPayload(
            baseUrl = baseUrl,
            pairingToken = "pair_test_token",
            protocolVersion = MOBILE_GATEWAY_PROTOCOL_VERSION,
            desktopName = "Mock Mac",
            expiresAt = Instant.now().plusSeconds(120).toString(),
        )
    }

    override fun close() {
        running = false
        runCatching { server.close() }
        threads.forEach { it.join(1_000) }
    }

    private fun handle(socket: Socket) {
        val request = socket.getInputStream().readRequest()
        when (request.method to request.path) {
            "GET" to "/mobile/health" -> handleHealth(socket.getOutputStream(), request)
            "POST" to "/mobile/pair/claim" -> handlePairClaim(socket.getOutputStream(), request)
            "GET" to "/mobile/ws" -> handleWebSocket(socket, request)
            else -> socket.getOutputStream().writeJsonResponse(404, JSONObject().put("ok", false).put("error", "not found"))
        }
    }

    private fun handleHealth(output: OutputStream, request: HttpRequest) {
        healthHeaders += request.headers
        output.writeJsonResponse(
            200,
            JSONObject()
                .put("ok", true)
                .put(
                    "data",
                    JSONObject()
                        .put("status", "ok")
                        .put("version", BuildConfig.VERSION_NAME)
                        .put("protocol_version", MOBILE_GATEWAY_PROTOCOL_VERSION)
                        .put("pairing_allowed", true),
                ),
        )
    }

    private fun handlePairClaim(output: OutputStream, request: HttpRequest) {
        pairClaimHeaders += request.headers
        val body = JSONObject(request.body)
        val status = if (body.optString("pairing_token") == "pair_test_token") 200 else 401
        val response = if (status == 200) {
            JSONObject()
                .put("ok", true)
                .put(
                    "data",
                    JSONObject()
                        .put(
                            "device",
                            JSONObject()
                                .put("id", "device_mock")
                                .put("name", body.optString("device_name"))
                                .put("created_at", Instant.now().toString()),
                        )
                        .put("device_token", deviceToken)
                        .put("protocol_version", MOBILE_GATEWAY_PROTOCOL_VERSION),
                )
        } else {
            JSONObject().put("ok", false).put("error", "invalid pairing token")
        }
        output.writeJsonResponse(status, response)
    }

    private fun handleWebSocket(socket: Socket, request: HttpRequest) {
        websocketHeaders += request.headers
        val output = socket.getOutputStream()
        if (request.headers["authorization"] != "Bearer $deviceToken") {
            output.writeJsonResponse(401, JSONObject().put("ok", false).put("error", "unauthorized"))
            return
        }
        val accept = websocketAccept(request.headers.getValue("sec-websocket-key"))
        output.writeTextResponse(
            status = "101 Switching Protocols",
            headers = mapOf(
                "Upgrade" to "websocket",
                "Connection" to "Upgrade",
                "Sec-WebSocket-Accept" to accept,
            ),
        )
        val peer = ServerPeer(socket.getInputStream(), output)
        peer.writeJson(snapshotEnvelope())
        val command = peer.readJson()
        if (command.optString("type") == "session.list") {
            sessionListLatch.countDown()
            peer.writeJson(
                envelope(
                    type = "ack",
                    id = command.optString("id"),
                    sessionId = command.optString("session_id"),
                    payload = JSONObject().put("type", "session.list"),
                )
            )
        }
    }

    private fun snapshotEnvelope(): JSONObject {
        return envelope(
            type = "state.snapshot",
            payload = JSONObject()
                .put("active_session_id", "s1")
                .put(
                    "sessions",
                    org.json.JSONArray()
                        .put(
                            JSONObject()
                                .put("id", "s1")
                                .put("title", "Mock coding session")
                                .put("workspace_name", "Xedit")
                                .put("message_count", 1)
                                .put("is_active", true)
                                .put("is_responding", false),
                        ),
                )
                .put(
                    "worktree_changes",
                    org.json.JSONArray()
                        .put(
                            JSONObject()
                                .put("repository_root", "/Users/rocky/Sites/Xedit")
                                .put("file_count", 1)
                                .put("total_added_lines", 4)
                                .put("total_deleted_lines", 0)
                                .put("session_id", "s1")
                                .put(
                                    "files",
                                    org.json.JSONArray()
                                        .put(
                                            JSONObject()
                                                .put("path", "app/src/main/java/com/willdeep/android/ui/WillDeepApp.kt")
                                                .put("kind", "M")
                                                .put("added_lines", 4)
                                                .put("deleted_lines", 0),
                                        ),
                                ),
                        ),
                ),
        )
    }
}

private data class HttpRequest(
    val method: String,
    val path: String,
    val headers: Map<String, String>,
    val body: String,
)

private class ServerPeer(
    private val input: InputStream,
    private val output: OutputStream,
) {
    fun writeJson(json: JSONObject) {
        output.writeWebSocketText(json.toString())
    }

    fun readJson(): JSONObject {
        return JSONObject(input.readWebSocketText())
    }
}

private fun InputStream.readRequest(): HttpRequest {
    val headerBytes = mutableListOf<Byte>()
    while (true) {
        val next = read()
        require(next >= 0) { "unexpected EOF while reading HTTP request" }
        headerBytes += next.toByte()
        if (headerBytes.takeLast(4) == listOf('\r'.code.toByte(), '\n'.code.toByte(), '\r'.code.toByte(), '\n'.code.toByte())) {
            break
        }
    }
    val headerText = headerBytes.toByteArray().toString(Charsets.UTF_8)
    val lines = headerText.split("\r\n").filter { it.isNotBlank() }
    val requestLine = lines.first().split(" ")
    val headers = lines.drop(1).associate { line ->
        val parts = line.split(":", limit = 2)
        parts[0].lowercase() to parts.getOrElse(1) { "" }.trim()
    }
    val bodyLength = headers["content-length"]?.toIntOrNull() ?: 0
    val body = if (bodyLength > 0) {
        readNBytes(bodyLength).toString(Charsets.UTF_8)
    } else {
        ""
    }
    return HttpRequest(
        method = requestLine[0],
        path = requestLine[1],
        headers = headers,
        body = body,
    )
}

private fun OutputStream.writeJsonResponse(status: Int, body: JSONObject) {
    writeTextResponse(
        status = "$status ${statusText(status)}",
        headers = mapOf(
            "Content-Type" to "application/json",
            "X-App-Version" to BuildConfig.VERSION_NAME,
            "X-Server-Version" to BuildConfig.VERSION_NAME,
        ),
        body = body.toString(),
    )
}

private fun OutputStream.writeTextResponse(
    status: String,
    headers: Map<String, String>,
    body: String = "",
) {
    val allHeaders = mapOf(
        "Content-Length" to body.toByteArray(Charsets.UTF_8).size.toString(),
    ) + headers
    val response = buildString {
        append("HTTP/1.1 ").append(status).append("\r\n")
        allHeaders.forEach { (name, value) -> append(name).append(": ").append(value).append("\r\n") }
        append("\r\n")
        append(body)
    }
    write(response.toByteArray(Charsets.UTF_8))
    flush()
}

private fun InputStream.readWebSocketText(): String {
    val first = readNBytes(2)
    require(first.size == 2) { "missing websocket header" }
    var length = first[1].toInt() and 0x7f
    if (length == 126) {
        length = readNBytes(2).fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xff) }
    }
    require(length != 127) { "large websocket frames are not needed in this test" }
    val masked = (first[1].toInt() and 0x80) != 0
    val mask = if (masked) readNBytes(4) else byteArrayOf()
    val payload = readNBytes(length)
    val unmasked = if (masked) {
        payload.mapIndexed { index, byte -> byte xor mask[index % 4] }.toByteArray()
    } else {
        payload
    }
    return unmasked.toString(Charsets.UTF_8)
}

private fun OutputStream.writeWebSocketText(text: String) {
    val payload = text.toByteArray(Charsets.UTF_8)
    val header = mutableListOf<Byte>(0x81.toByte())
    when {
        payload.size < 126 -> header += payload.size.toByte()
        payload.size <= 65_535 -> {
            header += 126.toByte()
            header += ((payload.size shr 8) and 0xff).toByte()
            header += (payload.size and 0xff).toByte()
        }
        else -> error("large websocket frames are not needed in this test")
    }
    write(header.toByteArray())
    write(payload)
    flush()
}

private fun envelope(
    type: String,
    id: String = UUID.randomUUID().toString(),
    sessionId: String = "",
    payload: JSONObject = JSONObject(),
): JSONObject {
    val json = JSONObject()
        .put("id", id)
        .put("type", type)
        .put("payload", payload)
    if (sessionId.isNotBlank()) {
        json.put("session_id", sessionId)
    }
    return json
}

private fun websocketAccept(key: String): String {
    val digest = MessageDigest.getInstance("SHA-1")
        .digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").toByteArray(Charsets.US_ASCII))
    return Base64.getEncoder().encodeToString(digest)
}

private fun statusText(status: Int): String {
    return when (status) {
        200 -> "OK"
        401 -> "Unauthorized"
        404 -> "Not Found"
        else -> "HTTP"
    }
}
