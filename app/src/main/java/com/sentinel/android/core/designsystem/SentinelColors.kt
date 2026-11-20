package com.sentinel.android.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object SentinelPalette {
    // Core futuristic dark identity
    val MidnightBg = Color(0xFF080B12)
    val MidnightSurface = Color(0xFF0E1420)
    val ElevatedSurface = Color(0xFF121C2A)
    val ElevatedSurfaceHigh = Color(0xFF1A273B)
    val BorderSubtle = Color(0xFF23334D)
    val BorderGlow = Color(0xFF2E4A75)

    // Accents
    val ElectricBlue = Color(0xFF2878FF)
    val ElectricBlueContainer = Color(0xFF102A57)
    val CyanAccent = Color(0xFF36C8F5)
    val CyanContainer = Color(0xFF0E3342)

    // Semantic statuses
    val SuccessGreen = Color(0xFF24D18B)
    val SuccessContainer = Color(0xFF0D3326)
    val WarningAmber = Color(0xFFFFB547)
    val WarningContainer = Color(0xFF3A290E)
    val ErrorRed = Color(0xFFFF5364)
    val ErrorContainer = Color(0xFF3D131B)

    // Text on dark
    val TextPrimaryDark = Color(0xFFF2F6FF)
    val TextSecondaryDark = Color(0xFF9AA9C2)
    val TextMutedDark = Color(0xFF6B7D99)

    // Light theme counterparts
    val LightBg = Color(0xFFF3F6FC)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFE6EEF9)
    val LightBorder = Color(0xFFCFDCEE)
    val LightElectricBlue = Color(0xFF1962E6)
    val LightCyan = Color(0xFF008EB8)
    val LightTextPrimary = Color(0xFF0B1322)
    val LightTextSecondary = Color(0xFF4B5C78)
}

val SentinelDarkColorScheme = darkColorScheme(
    primary = SentinelPalette.ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = SentinelPalette.ElectricBlueContainer,
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = SentinelPalette.CyanAccent,
    onSecondary = SentinelPalette.MidnightBg,
    secondaryContainer = SentinelPalette.CyanContainer,
    onSecondaryContainer = Color(0xFFCFF4FF),
    tertiary = SentinelPalette.SuccessGreen,
    onTertiary = SentinelPalette.MidnightBg,
    tertiaryContainer = SentinelPalette.SuccessContainer,
    onTertiaryContainer = Color(0xFFC2F7DF),
    background = SentinelPalette.MidnightBg,
    onBackground = SentinelPalette.TextPrimaryDark,
    surface = SentinelPalette.ElevatedSurface,
    onSurface = SentinelPalette.TextPrimaryDark,
    surfaceVariant = SentinelPalette.ElevatedSurfaceHigh,
    onSurfaceVariant = SentinelPalette.TextSecondaryDark,
    surfaceContainerLowest = SentinelPalette.MidnightBg,
    surfaceContainerLow = SentinelPalette.MidnightSurface,
    surfaceContainer = SentinelPalette.ElevatedSurface,
    surfaceContainerHigh = SentinelPalette.ElevatedSurfaceHigh,
    surfaceContainerHighest = Color(0xFF22334D),
    outline = SentinelPalette.BorderGlow,
    outlineVariant = SentinelPalette.BorderSubtle,
    error = SentinelPalette.ErrorRed,
    onError = Color.White,
    errorContainer = SentinelPalette.ErrorContainer,
    onErrorContainer = Color(0xFFFFD6DA)
)

val SentinelLightColorScheme = lightColorScheme(
    primary = SentinelPalette.LightElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E6FF),
    onPrimaryContainer = Color(0xFF001B47),
    secondary = SentinelPalette.LightCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDEFFF),
    onSecondaryContainer = Color(0xFF002533),
    tertiary = Color(0xFF0E9F66),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC9F7E4),
    onTertiaryContainer = Color(0xFF002817),
    background = SentinelPalette.LightBg,
    onBackground = SentinelPalette.LightTextPrimary,
    surface = SentinelPalette.LightSurface,
    onSurface = SentinelPalette.LightTextPrimary,
    surfaceVariant = SentinelPalette.LightSurfaceVariant,
    onSurfaceVariant = SentinelPalette.LightTextSecondary,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F9FE),
    surfaceContainer = Color(0xFFEEF3FB),
    surfaceContainerHigh = Color(0xFFE4ECF7),
    surfaceContainerHighest = Color(0xFFD9E4F4),
    outline = Color(0xFF8FA4C2),
    outlineVariant = SentinelPalette.LightBorder,
    error = Color(0xFFD92B3E),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD9),
    onErrorContainer = Color(0xFF410008)
)
