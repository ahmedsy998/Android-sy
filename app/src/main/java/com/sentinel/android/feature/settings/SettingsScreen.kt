package com.sentinel.android.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sentinel.android.core.common.AppLanguage
import com.sentinel.android.core.common.LocalSentinelStrings
import com.sentinel.android.core.common.ThemeMode
import com.sentinel.android.core.designsystem.SentinelGlassCard
import com.sentinel.android.core.designsystem.SentinelPalette
import com.sentinel.android.data.local.SentinelPreferences

@Composable
fun SettingsScreen(
    preferences: SentinelPreferences,
    onSelectTheme: (ThemeMode) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalSentinelStrings.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.settingsTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Theme Mode Selection
        item {
            SentinelGlassCard(accentColor = SentinelPalette.ElectricBlue.copy(alpha = 0.55f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DarkMode, contentDescription = null, tint = SentinelPalette.CyanAccent)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Appearance Theme", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = preferences.themeMode == ThemeMode.DARK,
                        onClick = { onSelectTheme(ThemeMode.DARK) },
                        label = { Text(strings.darkTheme) },
                        modifier = Modifier.testTag("theme_dark_chip")
                    )
                    FilterChip(
                        selected = preferences.themeMode == ThemeMode.LIGHT,
                        onClick = { onSelectTheme(ThemeMode.LIGHT) },
                        label = { Text(strings.lightTheme) },
                        modifier = Modifier.testTag("theme_light_chip")
                    )
                    FilterChip(
                        selected = preferences.themeMode == ThemeMode.SYSTEM,
                        onClick = { onSelectTheme(ThemeMode.SYSTEM) },
                        label = { Text(strings.systemTheme) },
                        modifier = Modifier.testTag("theme_system_chip")
                    )
                }
            }
        }

        // Language & Layout Direction (English LTR / Arabic RTL)
        item {
            SentinelGlassCard(accentColor = SentinelPalette.CyanAccent.copy(alpha = 0.55f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Translate, contentDescription = null, tint = SentinelPalette.CyanAccent)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(strings.languageLabel, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = preferences.language == AppLanguage.ENGLISH,
                        onClick = { onSelectLanguage(AppLanguage.ENGLISH) },
                        label = { Text("English (LTR)") },
                        modifier = Modifier.testTag("lang_en_chip")
                    )
                    FilterChip(
                        selected = preferences.language == AppLanguage.ARABIC,
                        onClick = { onSelectLanguage(AppLanguage.ARABIC) },
                        label = { Text("العربية (RTL)") },
                        modifier = Modifier.testTag("lang_ar_chip")
                    )
                }
            }
        }

        // Backend Connectivity & Optional Console Services Info
        item {
            SentinelGlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CloudDone, contentDescription = null, tint = SentinelPalette.SuccessGreen)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Cloud Backend & Optional Extensions", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Cloud Firestore Enterprise: Connected with hardened zero-trust rules.\n" +
                        "• Firebase Auth: Google Sign-In via Jetpack Credential Manager.\n" +
                        "• Firebase AI Logic + App Check: Provisioned for Media Studio.\n" +
                        "• Optional Push Messaging (FCM) / Additional Auth Providers: Configure in the Firebase Console if deploying custom push notifications.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
