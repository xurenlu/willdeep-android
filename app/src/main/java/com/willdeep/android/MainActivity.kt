package com.willdeep.android

import android.content.Intent
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        importSharedMessage(intent)
        setContent {
            WillDeepTheme {
                WillDeepApp(
                    sharedMessageText = sharedMessageRequest.text,
                    sharedMessageVersion = sharedMessageRequest.version,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        importSharedMessage(intent)
    }

    private fun importSharedMessage(intent: Intent?) {
        val text = SharedMessageIntentParser.extractText(
            action = intent?.action,
            mimeType = intent?.type,
            extraText = intent?.getStringExtra(Intent.EXTRA_TEXT),
            extraSubject = intent?.getStringExtra(Intent.EXTRA_SUBJECT),
        )
        if (text.isBlank()) return
        sharedMessageRequest = SharedMessageRequest(
            text = text,
            version = sharedMessageRequest.version + 1,
        )
    }
}

private data class SharedMessageRequest(
    val text: String = "",
    val version: Int = 0,
)
