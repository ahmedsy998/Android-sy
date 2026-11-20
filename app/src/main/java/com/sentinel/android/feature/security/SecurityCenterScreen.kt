package com.sentinel.android.feature.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sentinel.android.core.common.LocalSentinelStrings
import com.sentinel.android.core.designsystem.JetBrainsMonoFamily
import com.sentinel.android.core.designsystem.SentinelGlassCard
import com.sentinel.android.core.designsystem.SentinelPalette
import com.sentinel.android.core.designsystem.SentinelStatusBadge
import com.sentinel.android.core.security.BiometricAuthHelper
import com.sentinel.android.data.local.SentinelPreferences
import com.sentinel.android.feature.dashboard.TokenStatusInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SecurityCenterScreen(
    operatorName: String,
    operatorEmail: String,
    operatorUid: String,
    tokenInfo: TokenStatusInfo,
    preferences: SentinelPreferences,
    enrolledDeviceCount: Int,
    onRefreshToken: () -> Unit,
    onUpdateSecurityPolicy: (biometricEnabled: Boolean, timeoutMinutes: Int) -> Unit,
    onOpenActivityLogs: () -> Unit,
    onOpenSettings: () -> Unit,
    onSignOut: () -> Unit
) {
    val strings = LocalSentinelStrings.current
    val context = LocalContext.current
    val biometricsSupported = BiometricAuthHelper.canAuthenticate(context)
    val fmt = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.securityTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("security_open_settings")
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = strings.settingsTitle)
                }
            }
        }

        // Encrypted Transport & Hardware Keystore Posture
        item {
            SentinelGlassCard(accentColor = SentinelPalette.SuccessGreen.copy(alpha = 0.65f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = SentinelPalette.SuccessGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Encrypted Communications", style = MaterialTheme.typography.titleMedium)
                    }
                    SentinelStatusBadge(label = "TLS 1.3", color = SentinelPalette.SuccessGreen)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Android Keystore AES-256-GCM: ${if (tokenInfo.keystoreHardwareReady) "ACTIVE & VERIFIED" else "INITIALIZING"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SentinelPalette.CyanAccent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Device Key Fingerprint: ${tokenInfo.deviceKeyFingerprint}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enrolled Consented Devices: $enrolledDeviceCount · Zero Stealth Policy Enforced",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Short-Lived Token Management
        item {
            SentinelGlassCard(accentColor = SentinelPalette.ElectricBlue.copy(alpha = 0.6f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Key, contentDescription = null, tint = SentinelPalette.CyanAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Short-Lived Token Management", style = MaterialTheme.typography.titleMedium)
                    }
                    SentinelStatusBadge(label = "OAUTH2", color = SentinelPalette.ElectricBlue)
                }
                Spacer(modifier = Modifier.height(10.dp))
                val issuedText = if (tokenInfo.issuedAtMillis > 0L) fmt.format(Date(tokenInfo.issuedAtMillis)) else "Verified"
                val expiryText = if (tokenInfo.expiresAtMillis > 0L) fmt.format(Date(tokenInfo.expiresAtMillis)) else "Auto-rotated"
                Text(
                    text = "Provider: ${tokenInfo.providerId}\nIssued: $issuedText\nExpires: $expiryText",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onRefreshToken,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("security_refresh_token_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rotate Short-Lived Auth Token")
                }
            }
        }

        // Biometric Protection & Session Expiration Policy
        item {
            SentinelGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = SentinelPalette.CyanAccent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Biometric Operator Lock", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (biometricsSupported) {
                                    "Require biometric or device credential on app launch"
                                } else {
                                    "Biometric hardware not enrolled on this device"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = preferences.biometricLockEnabled,
                        enabled = biometricsSupported,
                        onCheckedChange = { enabled ->
                            onUpdateSecurityPolicy(enabled, preferences.sessionTimeoutMinutes)
                        },
                        modifier = Modifier.testTag("security_biometric_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Session Expiration Window",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(15, 30, 60, 120).forEach { mins ->
                        FilterChip(
                            selected = preferences.sessionTimeoutMinutes == mins,
                            onClick = { onUpdateSecurityPolicy(preferences.biometricLockEnabled, mins) },
                            label = { Text("${mins}m") }
                        )
                    }
                }
            }
        }

        // Audit Logs & Permission History Shortcut
        item {
            SentinelGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.History, contentDescription = null, tint = SentinelPalette.WarningAmber)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Security Event & Permission Logs", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Immutable append-only Firestore audit records",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onOpenActivityLogs,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("security_open_logs_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SentinelPalette.ElectricBlue)
                ) {
                    Text("Inspect Full Activity Audit Logs")
                }
            }
        }

        // Account Management
        item {
            SentinelGlassCard(accentColor = SentinelPalette.ErrorRed.copy(alpha = 0.45f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = SentinelPalette.CyanAccent)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(operatorName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            operatorEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "UID: ${operatorUid.take(16)}...",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("security_sign_out_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SentinelPalette.ErrorRed)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.signOut, fontWeight = FontWeight.Bold)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
