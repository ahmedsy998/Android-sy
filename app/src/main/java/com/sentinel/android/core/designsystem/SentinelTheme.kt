package com.sentinel.android.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.sentinel.android.core.common.AppLanguage
import com.sentinel.android.core.common.LocalSentinelStrings
import com.sentinel.android.core.common.ThemeMode
import com.sentinel.android.core.common.sentinelStringsFor

val SentinelShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SentinelTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    language: AppLanguage = AppLanguage.ENGLISH,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) SentinelDarkColorScheme else SentinelLightColorScheme
    val layoutDirection = if (language == AppLanguage.ARABIC) LayoutDirection.Rtl else LayoutDirection.Ltr
    val typography = sentinelTypography(isArabic = language == AppLanguage.ARABIC)
    val strings = sentinelStringsFor(language)

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalSentinelStrings provides strings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = SentinelShapes,
            content = content
        )
    }
}
