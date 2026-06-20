package com.clintmoyer.callscope.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlin.math.pow

enum class ThemeMode {
    System,
    Light,
    Dark,
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF23685C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBFEDE4),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF625B2A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E09C),
    onSecondaryContainer = Color(0xFF1E1C00),
    tertiary = Color(0xFF7B4D2F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBCA),
    onTertiaryContainer = Color(0xFF2E1504),
    background = Color(0xFFF8FAF7),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFF8FAF7),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFDCE5E0),
    onSurfaceVariant = Color(0xFF404944),
    outline = Color(0xFF707973),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA3D0C8),
    onPrimary = Color(0xFF003730),
    primaryContainer = Color(0xFF005046),
    onPrimaryContainer = Color(0xFFBFEDE4),
    secondary = Color(0xFFCCC381),
    onSecondary = Color(0xFF333100),
    secondaryContainer = Color(0xFF4A470F),
    onSecondaryContainer = Color(0xFFE9E09C),
    tertiary = Color(0xFFEAB999),
    onTertiary = Color(0xFF48290F),
    tertiaryContainer = Color(0xFF613F23),
    onTertiaryContainer = Color(0xFFFFDBCA),
    background = Color(0xFF101413),
    onBackground = Color(0xFFE0E3E0),
    surface = Color(0xFF101413),
    onSurface = Color(0xFFE0E3E0),
    surfaceVariant = Color(0xFF404944),
    onSurfaceVariant = Color(0xFFC0C9C3),
    outline = Color(0xFF8A938D),
)

@Composable
fun CallScopeTheme(
    mode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}

val ColorScheme.good: Color
    get() = if (isLight()) Color(0xFF23685C) else Color(0xFFA3D0C8)

val ColorScheme.warning: Color
    get() = if (isLight()) Color(0xFF9B5B00) else Color(0xFFFFC47E)

private fun ColorScheme.isLight(): Boolean = background.luminance() > 0.5f

private fun Color.luminance(): Float {
    fun channel(value: Float): Float = if (value <= 0.03928f) value / 12.92f else ((value + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
    return 0.2126f * channel(red) + 0.7152f * channel(green) + 0.0722f * channel(blue)
}
