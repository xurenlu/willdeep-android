package com.willdeep.android

import android.app.Application
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.willdeep.android.mobile.DeviceTokenStore
import com.willdeep.android.ui.AgentActivityBaseline
import com.willdeep.android.ui.ConnectionStatus
import com.willdeep.android.ui.HomeScreen
import com.willdeep.android.ui.MobileCommandState
import com.willdeep.android.ui.MobileGatewayViewModel
import com.willdeep.android.ui.MobileGatewayUiState
import com.willdeep.android.ui.RemoteMacSummary
import com.willdeep.android.ui.WORKSPACE_SWITCHER_TEST_TAG
import com.willdeep.android.ui.WillDeepApp
import com.willdeep.android.ui.agentActivitySignalAfter
import com.willdeep.android.ui.codeActivitySignalAfter
import com.willdeep.android.ui.hasAgentActivityAfter
import com.willdeep.android.ui.hasCodeActivityAfter
import com.willdeep.android.ui.hasTargetFileActivityAfter
import com.willdeep.android.ui.targetFileActivitySignalAfter
import com.willdeep.android.ui.workspacePillTestTag
import com.willdeep.android.ui.theme.WillDeepTheme
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

private const val LIVE_SMOKE_LOG_TAG = "WillDeepLiveSmoke"

@RunWith(AndroidJUnit4::class)
class MobileGatewayComposeInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val appContext: Application
        get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application

    private var gateway: InstrumentedGatewayMock? = null

    @Before
    fun setUp() {
        DeviceTokenStore(appContext).clear()
        gateway = InstrumentedGatewayMock()
    }

    @After
    fun tearDown() {
        gateway?.close()
        gateway = null
        DeviceTokenStore(appContext).clear()
    }

    @Test
    fun pairingFlowConnectsWebSocketAndDisplaysSnapshot() {
        val gateway = requireNotNull(gateway)
        val viewModel = MobileGatewayViewModel(appContext)
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val payload = JSONObject()
            .put("base_url", gateway.baseUrl)
            .put("pairing_token", gateway.pairingToken)
            .put("protocol_version", "mobile-gateway.v1")
            .put("desktop_name", gateway.desktopName)
            .put("expires_at", Instant.now().plusSeconds(120).toString())
            .toString()

        composeRule.setContent {
            WillDeepTheme {
                WillDeepApp(
                    viewModel = viewModel,
                    pairingPayloadText = payload,
                    pairingPayloadVersion = 1,
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            gateway.seenPaths.contains("/mobile/health")
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            gateway.seenPaths.contains("/mobile/pair/claim")
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            gateway.seenPaths.contains("/mobile/ws")
        }
        composeRule.onNodeWithText(gateway.desktopName)
            .assertIsDisplayed()
        composeRule.onNodeWithText(gateway.sessionTitle)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        val taskText = "Update the Android gateway UI"
        composeRule.onNode(hasSetTextAction())
            .performTextReplacement(taskText)
        composeRule.onNodeWithContentDescription(targetContext.getString(R.string.send_button))
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            gateway.seenCommands.contains("message.send")
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.state.value.conversationMessages.any { message ->
                message.content.contains(gateway.assistantDelta)
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.state.value.pendingTools.any { it.id == gateway.toolApprovalId }
        }
        composeRule.onNodeWithContentDescription(targetContext.getString(R.string.back_button))
            .performClick()
        composeRule.onNodeWithText(gateway.sessionTitle)
            .assertIsDisplayed()
        composeRule.onNodeWithText(targetContext.getString(R.string.tool_approval_title, gateway.toolApprovalTitle))
            .assertIsDisplayed()
        composeRule.onNodeWithText(targetContext.getString(R.string.tool_confirmation_label))
            .performTextReplacement("confirm")
        composeRule.onNodeWithText(targetContext.getString(R.string.approve_button))
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            gateway.approvedToolIds.contains(gateway.toolApprovalId)
        }
        composeRule.onNodeWithText(targetContext.getString(R.string.patch_proposal_title, gateway.patchTitle))
            .assertIsDisplayed()
        composeRule.onNodeWithText(targetContext.getString(R.string.approve_button))
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            gateway.approvedPatchIds.contains(gateway.patchId)
        }
    }

    @Test
    fun sharedMessageTextLoadsIntoComposerAndAppendsLaterImports() {
        val viewModel = MobileGatewayViewModel(appContext)
        val sharedMessageText = mutableStateOf("Refactor the selected Kotlin class")
        val sharedMessageVersion = mutableStateOf(1)

        composeRule.setContent {
            WillDeepTheme {
                WillDeepApp(
                    viewModel = viewModel,
                    sharedMessageText = sharedMessageText.value,
                    sharedMessageVersion = sharedMessageVersion.value,
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.state.value.messageText == "Refactor the selected Kotlin class"
        }
        composeRule.onNodeWithText("Refactor the selected Kotlin class")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.runOnIdle {
            sharedMessageText.value = "Add parser tests for selected text"
            sharedMessageVersion.value = 2
        }
        val combined = "Refactor the selected Kotlin class\n\nAdd parser tests for selected text"
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.state.value.messageText == combined
        }
        composeRule.onNodeWithText(combined)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun remoteMacSelectorShowsAllComputersAndStaleResponseNotice() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val state = MobileGatewayUiState(
            desktopName = "Primary MacBook Pro",
            isPaired = true,
            pairedMacs = listOf(
                RemoteMacSummary(
                    id = "macbook",
                    name = "Primary MacBook Pro",
                    lastDesktopResponseAtEpochMillis = System.currentTimeMillis() - 20_000,
                    isSelected = true,
                ),
                RemoteMacSummary(
                    id = "studio-mini",
                    name = "Studio Mac mini",
                    lastDesktopResponseAtEpochMillis = System.currentTimeMillis() - 120_000,
                ),
            ),
            selectedMacId = "macbook",
            isTransportConnected = true,
            desktopResponseAgeMillis = 20_000,
            status = ConnectionStatus.Reconnecting,
        )

        composeRule.setContent {
            WillDeepTheme {
                HomeScreen(
                    state = state,
                    versionName = "test",
                    onScanClick = {},
                    onCreateSession = {},
                    onSelectSession = {},
                    onConnect = {},
                    onDisconnect = {},
                    onForget = {},
                    onRemoteMacSelected = {},
                    onToolDecision = { _, _ -> },
                    onToolAnswerChange = { _, _ -> },
                    onToolConfirmationChange = { _, _ -> },
                    onPatchDecision = { _, _ -> },
                    onWorkspacePickerDismiss = {},
                    onWorkspacePickerRetry = {},
                    onWorkspaceSelected = {},
                )
            }
        }

        composeRule.onNodeWithText(targetContext.getString(R.string.remote_mac_reconnect_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText("Primary MacBook Pro")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            targetContext.getString(R.string.remote_mac_selector_title)
        ).performClick()
        composeRule.onNodeWithText("Studio Mac mini")
            .assertIsDisplayed()
        composeRule.onNodeWithText(targetContext.getString(R.string.remote_mac_selected_mark))
            .assertIsDisplayed()
    }

    @Test
    fun workspaceGroupsShowThreeSessionsUntilExpanded() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val alphaSessions = listOf("Alpha 5", "Alpha 4", "Alpha 3", "Alpha 2", "Alpha 1").map { title ->
            gatewaySession(id = title.lowercase().replace(' ', '-'), title = title, workspace = "alpha")
        }
        val betaSessions = listOf("Beta 2", "Beta 1").map { title ->
            gatewaySession(id = title.lowercase().replace(' ', '-'), title = title, workspace = "beta")
        }
        val state = MobileGatewayUiState(
            desktopName = "Primary MacBook Pro",
            isPaired = true,
            pairedMacs = listOf(
                RemoteMacSummary(id = "macbook", name = "Primary MacBook Pro", isSelected = true),
            ),
            selectedMacId = "macbook",
            isTransportConnected = true,
            desktopResponseAgeMillis = 1_000,
            status = ConnectionStatus.Connected,
            sessions = alphaSessions + betaSessions,
            selectedSessionId = alphaSessions.first().id,
        )

        composeRule.setContent {
            WillDeepTheme {
                HomeScreen(
                    state = state,
                    versionName = "1.24.0-rc2",
                    onScanClick = {},
                    onCreateSession = {},
                    onSelectSession = {},
                    onConnect = {},
                    onDisconnect = {},
                    onForget = {},
                    onRemoteMacSelected = {},
                    onToolDecision = { _, _ -> },
                    onToolAnswerChange = { _, _ -> },
                    onToolConfirmationChange = { _, _ -> },
                    onPatchDecision = { _, _ -> },
                    onWorkspacePickerDismiss = {},
                    onWorkspacePickerRetry = {},
                    onWorkspaceSelected = {},
                )
            }
        }

        assertTrue(
            composeRule.onAllNodesWithText(targetContext.getString(R.string.home_title))
                .fetchSemanticsNodes()
                .isEmpty()
        )
        composeRule.onNodeWithText("Alpha 5").assertIsDisplayed()
        composeRule.onNodeWithText("Alpha 3").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Alpha 2").fetchSemanticsNodes().isEmpty())
        saveRootScreenshot(targetContext, "workspace-grouped-home.png")

        composeRule.onNodeWithText(
            targetContext.getString(R.string.workspace_sessions_expand, 2)
        ).performScrollTo().performClick()
        composeRule.onNodeWithText("Alpha 1")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithTag(WORKSPACE_SWITCHER_TEST_TAG).performClick()
        composeRule.onNodeWithTag(workspacePillTestTag("beta")).performClick()
        assertTrue(composeRule.onAllNodesWithText("Alpha 5").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("Beta 2")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun liveMacGatewayPairingPayloadConnectsAndDisplaysConnectedState() {
        val arguments = InstrumentationRegistry.getArguments()
        val payload = arguments.getString("mobileGatewayPairingPayload").orEmpty()
        assumeTrue(
            "Set mobileGatewayPairingPayload to run live Mac gateway smoke.",
            payload.isNotBlank(),
        )
        val liveMessage = arguments.getString("mobileGatewayLiveMessage").orEmpty()
        val expectAgentActivity = arguments.getString("mobileGatewayExpectAgentActivity")
            .toBooleanFlag()
        val expectCodeActivity = arguments.getString("mobileGatewayExpectCodeActivity")
            .toBooleanFlag()
        val expectedTargetFile = arguments.getString("mobileGatewayExpectedTargetFile").orEmpty()
        val liveWorkspacePath = arguments.getString("mobileGatewayWorkspacePath").orEmpty()
        val agentActivityTimeoutMillis = arguments.getString("mobileGatewayAgentActivityTimeoutMillis")
            ?.toLongOrNull()
            ?.coerceAtLeast(1_000)
            ?: 60_000
        val desktopName = runCatching { JSONObject(payload).optString("desktop_name") }
            .getOrDefault("")
            .ifBlank { "Mac" }
        val viewModel = MobileGatewayViewModel(appContext)
        viewModel.updatePreferredWorkspacePath(liveWorkspacePath)
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            WillDeepTheme {
                WillDeepApp(
                    viewModel = viewModel,
                    pairingPayloadText = payload,
                    pairingPayloadVersion = 1,
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = 15_000) {
            viewModel.state.value.status == ConnectionStatus.Connected ||
                viewModel.state.value.status == ConnectionStatus.Error
        }

        assertEquals(ConnectionStatus.Connected, viewModel.state.value.status)
        composeRule.onNodeWithText(desktopName)
            .assertIsDisplayed()

        if (liveMessage.isNotBlank()) {
            composeRule.waitUntil(timeoutMillis = 15_000) {
                viewModel.state.value.sessions.any { it.id == viewModel.state.value.selectedSessionId }
            }
            val activeSession = viewModel.state.value.sessions.first {
                it.id == viewModel.state.value.selectedSessionId
            }
            val activeSessionTitle = activeSession.title.ifBlank {
                activeSession.workspaceName.ifBlank { activeSession.id.take(8) }
            }
            composeRule.onNodeWithText(activeSessionTitle)
                .performScrollTo()
                .performClick()
            composeRule.onNode(hasSetTextAction())
                .performTextReplacement(liveMessage)
            val baseline = AgentActivityBaseline.capture(viewModel.state.value)
            composeRule.onNodeWithContentDescription(targetContext.getString(R.string.send_button))
                .performClick()
            composeRule.waitUntil(timeoutMillis = 15_000) {
                viewModel.state.value.commandStatuses.any { command ->
                    command.type == "message.send" && command.state != MobileCommandState.Pending
                }
            }
            assertEquals(
                MobileCommandState.Accepted,
                viewModel.state.value.commandStatuses.last { it.type == "message.send" }.state,
            )
            reportLiveSmokeSignal("mobile_message_send_ack", "accepted")
            if (expectAgentActivity) {
                composeRule.waitUntil(timeoutMillis = agentActivityTimeoutMillis) {
                    viewModel.state.value.hasAgentActivityAfter(baseline)
                }
                val signal = viewModel.state.value.agentActivitySignalAfter(baseline)
                assertNotNull(signal)
                reportLiveSmokeSignal(
                    "mac_agent_activity_signal",
                    signal?.reportValue ?: "unknown",
                )
            }
            if (expectCodeActivity) {
                composeRule.waitUntil(timeoutMillis = agentActivityTimeoutMillis) {
                    viewModel.state.value.hasCodeActivityAfter(baseline)
                }
                val signal = viewModel.state.value.codeActivitySignalAfter(baseline)
                assertNotNull(signal)
                reportLiveSmokeSignal(
                    "mac_code_activity_signal",
                    signal?.reportValue ?: "unknown",
                )
            }
            if (expectedTargetFile.isNotBlank()) {
                approveNonInteractivePendingToolsUntil(
                    timeoutMillis = agentActivityTimeoutMillis,
                    viewModel = viewModel,
                ) {
                    viewModel.state.value.hasTargetFileActivityAfter(baseline, expectedTargetFile)
                }
                composeRule.waitUntil(timeoutMillis = agentActivityTimeoutMillis) {
                    viewModel.state.value.hasTargetFileActivityAfter(baseline, expectedTargetFile)
                }
                val signal = viewModel.state.value.targetFileActivitySignalAfter(
                    baseline,
                    expectedTargetFile,
                )
                assertNotNull(signal)
                reportLiveSmokeSignal(
                    "mac_target_file_signal",
                    signal?.reportValue ?: "unknown",
                )
            }
        }
    }

    private fun approveNonInteractivePendingToolsUntil(
        timeoutMillis: Long,
        viewModel: MobileGatewayViewModel,
        isDone: () -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        val approvedToolIds = mutableSetOf<String>()
        while (System.currentTimeMillis() < deadline && !isDone()) {
            val approval = viewModel.state.value.pendingTools.firstOrNull { pending ->
                pending.id !in approvedToolIds &&
                    !pending.requiresAnswer &&
                    !pending.requiresConfirmation
            }
            if (approval != null) {
                approvedToolIds += approval.id
                composeRule.runOnIdle {
                    viewModel.decideTool(approval, approved = true)
                }
            }
            Thread.sleep(250)
        }
    }

    private fun reportLiveSmokeSignal(key: String, value: String) {
        InstrumentationRegistry.getInstrumentation().sendStatus(
            0,
            Bundle().apply { putString(key, value) },
        )
        Log.i(LIVE_SMOKE_LOG_TAG, "$key=$value")
    }

    private fun saveRootScreenshot(context: android.content.Context, name: String) {
        val directory = requireNotNull(context.getExternalFilesDir(null))
        val screenshot = composeRule.onRoot().captureToImage().asAndroidBitmap()
        FileOutputStream(File(directory, name)).use { output ->
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    private fun gatewaySession(
        id: String,
        title: String,
        workspace: String,
    ): com.willdeep.android.mobile.GatewaySession {
        return com.willdeep.android.mobile.GatewaySession(
            id = id,
            title = title,
            workspaceName = workspace,
            messageCount = 1,
            isActive = false,
            isResponding = false,
        )
    }

    private fun String?.toBooleanFlag(): Boolean {
        return this == "1" ||
            this?.equals("true", ignoreCase = true) == true ||
            this?.equals("yes", ignoreCase = true) == true
    }

}

private class InstrumentedGatewayMock : AutoCloseable {
    val pairingToken = "pair_instrumented"
    val desktopName = "Instrumented Mac"
    val serverVersion = "1.67.0-rc1"
    val sessionTitle = "Instrumented coding session"
    val assistantDelta = "Mac WillDeep is applying the Android change."
    val toolApprovalId = "tool_instrumented"
    val toolApprovalTitle = "Run Gradle tests"
    val patchId = "patch_instrumented"
    val patchTitle = "Review generated Android patch"
    val patchDiff = "diff --git a/app/src/main/java/WillDeep.kt b/app/src/main/java/WillDeep.kt\n+val mobileGateway = true"
    val seenPaths = CopyOnWriteArrayList<String>()
    val seenCommands = CopyOnWriteArrayList<String>()
    val approvedToolIds = CopyOnWriteArrayList<String>()
    val approvedPatchIds = CopyOnWriteArrayList<String>()

    private val ready = CountDownLatch(1)
    private val server = ServerSocket(0)
    private val sockets = CopyOnWriteArrayList<Socket>()
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
        sockets.forEach { socket -> runCatching { socket.close() } }
        runCatching { server.close() }
        worker.join(1_000)
    }

    private fun handle(socket: Socket) {
        sockets += socket
        try {
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
                method == "GET" && path == "/mobile/ws" -> handleWebSocket(socket, headers)
                else -> writeJson(socket, 404, JSONObject().put("ok", false).put("error", "not found"))
            }
        } finally {
            sockets -= socket
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

    private fun handleWebSocket(socket: Socket, headers: Map<String, String>) {
        if (headers["authorization"] != "Bearer device_token_instrumented") {
            writeJson(socket, 401, JSONObject().put("ok", false).put("error", "unauthorized"))
            return
        }
        val key = headers["sec-websocket-key"].orEmpty()
        val accept = Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-1").digest((key + WEB_SOCKET_GUID).toByteArray(StandardCharsets.UTF_8)),
        )
        val response = buildString {
            append("HTTP/1.1 101 Switching Protocols\r\n")
            append("Upgrade: websocket\r\n")
            append("Connection: Upgrade\r\n")
            append("Sec-WebSocket-Accept: $accept\r\n")
            append("\r\n")
        }.toByteArray(StandardCharsets.UTF_8)
        socket.getOutputStream().write(response)
        writeWebSocketText(socket, snapshotEnvelope().toString())
        socket.getOutputStream().flush()
        while (running && !socket.isClosed) {
            val text = readWebSocketText(socket) ?: break
            val command = JSONObject(text)
            val commandType = command.optString("type")
            seenCommands += commandType
            when (commandType) {
                "message.send" -> {
                    writeWebSocketText(socket, ackEnvelope(command).toString())
                    writeWebSocketText(socket, messageAppendEnvelope(command).toString())
                    writeWebSocketText(socket, messageDeltaEnvelope().toString())
                    writeWebSocketText(socket, messageDoneEnvelope().toString())
                    writeWebSocketText(socket, toolPendingEnvelope().toString())
                    socket.getOutputStream().flush()
                }
                "tool.decide" -> {
                    val payload = command.optJSONObject("payload")
                    if (
                        payload?.optBoolean("approved") == true &&
                        payload.optString("typed_confirmation") == "confirm"
                    ) {
                        approvedToolIds += command.optJSONObject("payload")?.optString("id").orEmpty()
                    }
                    writeWebSocketText(socket, ackEnvelope(command).toString())
                    writeWebSocketText(socket, patchUpsertEnvelope().toString())
                    socket.getOutputStream().flush()
                }
                "diff.get" -> {
                    writeWebSocketText(socket, patchDiffAckEnvelope(command).toString())
                    socket.getOutputStream().flush()
                }
                "patch.decide" -> {
                    if (command.optJSONObject("payload")?.optBoolean("approved") == true) {
                        approvedPatchIds += command.optJSONObject("payload")?.optString("id").orEmpty()
                    }
                    writeWebSocketText(socket, ackEnvelope(command).toString())
                    socket.getOutputStream().flush()
                }
                else -> {
                    writeWebSocketText(socket, ackEnvelope(command).toString())
                    socket.getOutputStream().flush()
                }
            }
        }
    }

    private fun snapshotEnvelope(): JSONObject {
        return JSONObject()
            .put("id", "snapshot_instrumented")
            .put("type", "state.snapshot")
            .put("ts", "2026-06-14T12:00:00Z")
            .put(
                "payload",
                JSONObject()
                    .put("active_session_id", "s1")
                    .put(
                        "sessions",
                        org.json.JSONArray()
                            .put(
                                JSONObject()
                                    .put("id", "s1")
                                    .put("title", sessionTitle)
                                    .put("workspace_name", "willdeep-android")
                                    .put("message_count", 1)
                                    .put("is_active", true)
                                    .put("is_responding", false),
                            ),
                    ),
            )
    }

    private fun ackEnvelope(command: JSONObject): JSONObject {
        return JSONObject()
            .put("id", command.optString("id"))
            .put("type", "ack")
            .put("session_id", "s1")
            .put("ts", "2026-06-14T12:00:01Z")
            .put("payload", JSONObject().put("type", command.optString("type")))
    }

    private fun messageAppendEnvelope(command: JSONObject): JSONObject {
        return JSONObject()
            .put("id", "message_user_instrumented")
            .put("type", "message.append")
            .put("session_id", "s1")
            .put("ts", "2026-06-14T12:00:02Z")
            .put(
                "payload",
                JSONObject()
                    .put("id", "m_user")
                    .put("role", "user")
                    .put("content", command.optJSONObject("payload")?.optString("text").orEmpty())
                    .put("created_at", "2026-06-14T12:00:02Z"),
            )
    }

    private fun messageDeltaEnvelope(): JSONObject {
        return JSONObject()
            .put("id", "message_delta_instrumented")
            .put("type", "message.delta")
            .put("session_id", "s1")
            .put("ts", "2026-06-14T12:00:03Z")
            .put(
                "payload",
                JSONObject()
                    .put("message_id", "m_assistant")
                    .put("delta", assistantDelta),
            )
    }

    private fun messageDoneEnvelope(): JSONObject {
        return JSONObject()
            .put("id", "message_done_instrumented")
            .put("type", "message.done")
            .put("session_id", "s1")
            .put("ts", "2026-06-14T12:00:04Z")
            .put("payload", JSONObject().put("message_id", "m_assistant"))
    }

    private fun toolPendingEnvelope(): JSONObject {
        return JSONObject()
            .put("id", "tool_pending_instrumented")
            .put("type", "tool.pending")
            .put("session_id", "s1")
            .put("ts", "2026-06-14T12:00:05Z")
            .put(
                "payload",
                JSONObject()
                    .put("approval_id", toolApprovalId)
                    .put("title", toolApprovalTitle)
                    .put("tool_name", "shell")
                    .put("summary", "Mac WillDeep wants to run tests before applying the change.")
                    .put("command", "./gradlew :app:testDebugUnitTest")
                    .put("requires_confirmation", true),
            )
    }

    private fun patchUpsertEnvelope(): JSONObject {
        return JSONObject()
            .put("id", "patch_upsert_instrumented")
            .put("type", "patch.upsert")
            .put("session_id", "s1")
            .put("ts", "2026-06-14T12:00:06Z")
            .put(
                "payload",
                JSONObject()
                    .put("patch_id", patchId)
                    .put("title", patchTitle)
                    .put("summary", "Mac WillDeep generated a patch for Android review.")
                    .put("path", "app/src/main/java/com/willdeep/android/ui/WillDeepApp.kt")
                    .put("diffstat", "+1 -0"),
            )
    }

    private fun patchDiffAckEnvelope(command: JSONObject): JSONObject {
        return JSONObject()
            .put("id", command.optString("id"))
            .put("type", "ack")
            .put("session_id", "s1")
            .put("ts", "2026-06-14T12:00:07Z")
            .put(
                "payload",
                JSONObject()
                    .put("type", "diff.get")
                    .put("patch_id", patchId)
                    .put("title", patchTitle)
                    .put("diff", patchDiff),
            )
    }

    private fun writeWebSocketText(socket: Socket, text: String) {
        val body = text.toByteArray(StandardCharsets.UTF_8)
        val output = socket.getOutputStream()
        output.write(0x81)
        when {
            body.size <= 125 -> output.write(body.size)
            body.size <= 65_535 -> {
                output.write(126)
                output.write((body.size shr 8) and 0xff)
                output.write(body.size and 0xff)
            }
            else -> error("snapshot frame too large")
        }
        output.write(body)
    }

    private fun readWebSocketText(socket: Socket): String? {
        val input = socket.getInputStream()
        val first = input.read()
        if (first < 0) return null
        val second = input.read()
        if (second < 0) return null
        val opcode = first and 0x0f
        if (opcode == 0x8) return null
        val masked = (second and 0x80) != 0
        var length = second and 0x7f
        if (length == 126) {
            length = (input.read() shl 8) or input.read()
        } else if (length == 127) {
            repeat(4) { input.read() }
            length = 0
            repeat(4) { length = (length shl 8) or input.read() }
        }
        val mask = if (masked) ByteArray(4) { input.read().toByte() } else ByteArray(0)
        val payload = ByteArray(length) { index ->
            val value = input.read()
            if (masked) {
                (value xor mask[index % 4].toInt()).toByte()
            } else {
                value.toByte()
            }
        }
        return payload.toString(StandardCharsets.UTF_8)
    }

    private companion object {
        const val WEB_SOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
    }
}
