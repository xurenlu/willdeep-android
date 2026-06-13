package com.willdeep.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF1E5EFF),
    onPrimary = Color.White,
    secondary = Color(0xFF526070),
    tertiary = Color(0xFF006D5B),
    background = Color(0xFFFAFBFD),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE7EBF2),
    outline = Color(0xFF707783),
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF9DB7FF),
    onPrimary = Color(0xFF003180),
    secondary = Color(0xFFBBC7D7),
    tertiary = Color(0xFF77D7C3),
    background = Color(0xFF101418),
    surface = Color(0xFF171B20),
    surfaceVariant = Color(0xFF3F4650),
    outline = Color(0xFF8A929E),
)

@Composable
fun WillDeepTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
