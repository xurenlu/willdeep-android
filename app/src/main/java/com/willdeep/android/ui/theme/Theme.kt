package com.willdeep.android.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFFD65E36),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFBE2D8),
    onPrimaryContainer = Color(0xFF70240F),
    secondary = Color(0xFF77736D),
    tertiary = Color(0xFF2F9E44),
    background = Color(0xFFF7F6F2),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F0EB),
    outline = Color(0xFF77736D),
    outlineVariant = Color(0xFFE2DED7),
)

@Composable
fun WillDeepTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
