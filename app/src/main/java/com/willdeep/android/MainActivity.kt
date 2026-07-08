package com.willdeep.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.willdeep.android.ui.WillDeepApp
import com.willdeep.android.ui.theme.WillDeepTheme

class MainActivity : ComponentActivity() {
    private var sharedMessageRequest by mutableStateOf(SharedMessageRequest())
    private var attentionActionRequest by mutableStateOf(AttentionActionRequest())
    private var pairingPayloadRequest by mutableStateOf(PairingPayloadRequest())

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // No-op: notification delivery degrades gracefully when the user declines.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        importIntent(intent)
        setContent {
            WillDeepTheme {
                WillDeepApp(
                    sharedMessageText = sharedMessageRequest.text,
                    sharedMessageVersion = sharedMessageRequest.version,
                    attentionAction = attentionActionRequest.value,
                    attentionActionVersion = attentionActionRequest.version,
                    pairingPayloadText = pairingPayloadRequest.text,
                    pairingPayloadVersion = pairingPayloadRequest.version,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        importIntent(intent)
    }

    private fun importIntent(intent: Intent?) {
        importSharedMessage(intent)
        importAttentionAction(intent)
        importPairingPayload(intent)
    }

    private fun importSharedMessage(intent: Intent?) {
        val text = SharedMessageIntentParser.extractText(
            action = intent?.action,
            mimeType = intent?.type,
            extraText = intent?.getStringExtra(Intent.EXTRA_TEXT),
            extraSubject = intent?.getStringExtra(Intent.EXTRA_SUBJECT),
            extraTitle = intent?.getCharSequenceExtra(Intent.EXTRA_TITLE)?.toString(),
            extraProcessText = intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString(),
        )
        if (text.isBlank()) return
        sharedMessageRequest = SharedMessageRequest(
            text = text,
            version = sharedMessageRequest.version + 1,
        )
    }

    private fun importAttentionAction(intent: Intent?) {
        val request = MobileAttentionActions.parse(
            action = intent?.action,
            targetType = intent?.getStringExtra(MobileAttentionActions.EXTRA_TARGET_TYPE),
            targetId = intent?.getStringExtra(MobileAttentionActions.EXTRA_TARGET_ID),
            sessionId = intent?.getStringExtra(MobileAttentionActions.EXTRA_SESSION_ID),
        ) ?: return
        attentionActionRequest = AttentionActionRequest(
            value = request,
            version = attentionActionRequest.version + 1,
        )
    }

    private fun importPairingPayload(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val value = intent.dataString?.trim().orEmpty()
        if (value.isBlank()) return
        pairingPayloadRequest = PairingPayloadRequest(
            text = value,
            version = pairingPayloadRequest.version + 1,
        )
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

private data class SharedMessageRequest(
    val text: String = "",
    val version: Int = 0,
)

private data class AttentionActionRequest(
    val value: MobileAttentionActionRequest? = null,
    val version: Int = 0,
)

private data class PairingPayloadRequest(
    val text: String = "",
    val version: Int = 0,
)
