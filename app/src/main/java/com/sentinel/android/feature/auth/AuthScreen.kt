package com.sentinel.android.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.sentinel.android.core.common.AppLanguage
import com.sentinel.android.core.common.LocalSentinelStrings
import com.sentinel.android.core.designsystem.SentinelErrorCard
import com.sentinel.android.core.designsystem.SentinelGlassCard
import com.sentinel.android.core.designsystem.SentinelPalette
import com.sentinel.android.core.designsystem.SentinelStatusBadge

@Composable
fun AuthScreen(
    currentLanguage: AppLanguage,
    onToggleLanguage: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val strings = LocalSentinelStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SentinelStatusBadge(
                    label = "ZERO-TRUST · TLS 1.3",
                    color = SentinelPalette.CyanAccent,
                    pulsing = true
                )
                OutlinedButton(
                    onClick = onToggleLanguage,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("auth_language_toggle")
                ) {
                    Text(if (currentLanguage == AppLanguage.ENGLISH) "العربية (RTL)" else "English (LTR)")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Hero Artwork Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_sentinel_hero_1795200152850),
                    contentDescription = "Android Sentinel Security Command",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    SentinelPalette.MidnightBg.copy(alpha = 0.88f),
                                    SentinelPalette.MidnightBg.copy(alpha = 0.25f)
                                )
                            )
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.sentinel_app_icon_1795200152921),
                            contentDescription = "Sentinel Emblem",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "ANDROID SENTINEL",
                            style = MaterialTheme.typography.headlineMedium.copy(letterSpacing = 1.5.sp),
                            color = SentinelPalette.TextPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "v1.0 · Consent-First Platform",
                            style = MaterialTheme.typography.labelMedium,
                            color = SentinelPalette.CyanAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SentinelGlassCard(
                accentColor = SentinelPalette.ElectricBlue.copy(alpha = 0.65f),
                contentPadding = 22.dp
            ) {
                Text(
                    text = strings.authSubtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(18.dp))

                SecurityPillarRow(
                    icon = Icons.Filled.Visibility,
                    title = "100% Visible Consent",
                    subtitle = "Every camera, screen share, and file transfer requires explicit owner approval."
                )
                Spacer(modifier = Modifier.height(10.dp))
                SecurityPillarRow(
                    icon = Icons.Filled.Key,
                    title = "Android Keystore AES-256-GCM",
                    subtitle = "Hardware-backed local vault & short-lived verified Firebase OAuth tokens."
                )
                Spacer(modifier = Modifier.height(10.dp))
                SecurityPillarRow(
                    icon = Icons.Filled.GppGood,
                    title = "Verified Account Ownership",
                    subtitle = "Sign in or register seamlessly using Google Credential Manager with built-in account recovery."
                )

                Spacer(modifier = Modifier.height(22.dp))

                if (errorMessage != null) {
                    SentinelErrorCard(message = errorMessage!!)
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Button(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SentinelPalette.ElectricBlue,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = androidx.compose.ui.graphics.Color.White,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Authenticating...")
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.signInWithGoogle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Encrypted Session",
                            tint = SentinelPalette.SuccessGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = strings.authSecurityGuarantee,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityPillarRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SentinelPalette.ElectricBlue.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = SentinelPalette.CyanAccent,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BiometricLockScreen(
    onRequestBiometric: () -> Unit,
    onSignOut: () -> Unit
) {
    val strings = LocalSentinelStrings.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        SentinelGlassCard(
            modifier = Modifier.widthIn(max = 460.dp),
            accentColor = SentinelPalette.CyanAccent,
            contentPadding = 24.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SentinelPalette.ElectricBlue.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fingerprint,
                        contentDescription = "Biometric Lock",
                        tint = SentinelPalette.CyanAccent,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = strings.biometricPromptTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.biometricPromptSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(22.dp))
                Button(
                    onClick = onRequestBiometric,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("biometric_unlock_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Fingerprint, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.biometricUnlock)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("biometric_sign_out_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(strings.signOut)
                }
            }
        }
    }
}
