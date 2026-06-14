package com.willdeep.android

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.willdeep.android.mobile.DeviceTokenStore
import com.willdeep.android.ui.MobileGatewayViewModel
import com.willdeep.android.ui.WillDeepApp
import com.willdeep.android.ui.theme.WillDeepTheme
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

@RunWith(AndroidJUnit4::class)
class MobileGatewayComposeInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val appContext: Application
        get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application

    private lateinit var gateway: InstrumentedGatewayMock

    @Before
    fun setUp() {
        DeviceTokenStore(appContext).clear()
        gateway = InstrumentedGatewayMock()
    }

    @After
    fun tearDown() {
        gateway.close()
        DeviceTokenStore(appContext).clear()
    }

    @Test
    fun pairingFlowUsesGatewayHealthAndClaimEndpoints() {
        val viewModel = MobileGatewayViewModel(appContext)
        composeRule.setContent {
            WillDeepTheme {
                WillDeepApp(viewModel)
            }
        }

        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val payload = JSONObject()
            .put("base_url", gateway.baseUrl)
            .put("pairing_token", gateway.pairingToken)
            .put("protocol_version", "mobile-gateway.v1")
            .put("desktop_name", gateway.desktopName)
            .put("expires_at", "2026-06-14T12:02:00Z")
            .toString()

        composeRule.onNodeWithText(targetContext.getString(R.string.screen_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(targetContext.getString(R.string.pairing_payload_label))
            .performTextReplacement(payload)
        composeRule.onNodeWithText(targetContext.getString(R.string.device_name_label))
            .performTextReplacement("Pixel Instrumented")

        composeRule.onNodeWithText(targetContext.getString(R.string.check_gateway_button))
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            gateway.seenPaths.contains("/mobile/health")
        }
        composeRule.onNodeWithText(targetContext.getString(R.string.gateway_server_version, gateway.serverVersion))
            .assertIsDisplayed()

        composeRule.onNodeWithText(targetContext.getString(R.string.pair_button))
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            gateway.seenPaths.contains("/mobile/pair/claim")
        }
        composeRule.onNodeWithText(targetContext.getString(R.string.paired_to, gateway.desktopName))
            .assertIsDisplayed()
    }
}

private class InstrumentedGatewayMock : AutoCloseable {
    val pairingToken = "pair_instrumented"
    val desktopName = "Instrumented Mac"
    val serverVersion = "1.67.0-rc1"
    val seenPaths = CopyOnWriteArrayList<String>()

    private val ready = CountDownLatch(1)
    private val server = ServerSocket(0)
    private var running = true
    private val worker = thread(name = "instrumented-mobile-gateway-mock") {
        ready.countDown()
        while (running) {
            runCatching {
                server.accept().use(::handle)
            }
        }
    }

    val baseUrl: String
        get() {
            ready.await(2, TimeUnit.SECONDS)
            return "http://127.0.0.1:${server.localPort}"
        }

    override fun close() {
        running = false
        runCatching { server.close() }
        worker.join(1_000)
    }

    private fun handle(socket: Socket) {
        val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        val requestLine = reader.readLine().orEmpty()
        if (requestLine.isBlank()) return
        val parts = requestLine.split(" ")
        val method = parts.getOrNull(0).orEmpty()
        val path = parts.getOrNull(1).orEmpty()
        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break
            val separator = line.indexOf(':')
            if (separator > 0) {
                headers[line.substring(0, separator).trim().lowercase()] = line.substring(separator + 1).trim()
            }
        }
        val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
        val body = CharArray(contentLength)
        if (contentLength > 0) {
            reader.read(body, 0, contentLength)
        }

        seenPaths += path
        when {
            method == "GET" && path == "/mobile/health" -> writeJson(socket, 200, healthBody())
            method == "POST" && path == "/mobile/pair/claim" -> writeJson(socket, 200, pairBody(String(body)))
            else -> writeJson(socket, 404, JSONObject().put("ok", false).put("error", "not found"))
        }
    }

    private fun healthBody(): JSONObject {
        return JSONObject()
            .put("ok", true)
            .put(
                "data",
                JSONObject()
                    .put("status", "ok")
                    .put("version", serverVersion)
                    .put("protocol_version", "mobile-gateway.v1")
                    .put("pairing_allowed", true),
            )
    }

    private fun pairBody(rawBody: String): JSONObject {
        val body = JSONObject(rawBody)
        if (body.optString("pairing_token") != pairingToken) {
            return JSONObject().put("ok", false).put("error", "invalid pairing token")
        }
        return JSONObject()
            .put("ok", true)
            .put(
                "data",
                JSONObject()
                    .put(
                        "device",
                        JSONObject()
                            .put("id", "device_instrumented")
                            .put("name", body.optString("device_name", "Android"))
                            .put("created_at", "2026-06-14T12:00:00Z"),
                    )
                    .put("device_token", "device_token_instrumented")
                    .put("protocol_version", "mobile-gateway.v1"),
            )
    }

    private fun writeJson(socket: Socket, status: Int, json: JSONObject) {
        val body = json.toString().toByteArray(StandardCharsets.UTF_8)
        val statusText = if (status == 200) "OK" else "Error"
        val response = buildString {
            append("HTTP/1.1 $status $statusText\r\n")
            append("Content-Type: application/json\r\n")
            append("Content-Length: ${body.size}\r\n")
            append("Connection: close\r\n")
            append("X-App-Version: $serverVersion\r\n")
            append("X-Server-Version: $serverVersion\r\n")
            append("\r\n")
        }.toByteArray(StandardCharsets.UTF_8)
        socket.getOutputStream().write(response)
        socket.getOutputStream().write(body)
        socket.getOutputStream().flush()
    }
}
