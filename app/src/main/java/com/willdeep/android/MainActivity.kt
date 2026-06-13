package com.willdeep.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.willdeep.android.ui.WillDeepApp
import com.willdeep.android.ui.theme.WillDeepTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WillDeepTheme {
                WillDeepApp()
            }
        }
    }
}
