package com.sentinel.android.feature.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhonelinkOff
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.sentinel.android.core.common.LocalSentinelStrings
import com.sentinel.android.core.common.UiState
import com.sentinel.android.core.designsystem.SentinelEmptyState
import com.sentinel.android.core.designsystem.SentinelErrorCard
import com.sentinel.android.core.designsystem.SentinelGlassCard
import com.sentinel.android.core.designsystem.SentinelMetricTile
import com.sentinel.android.core.designsystem.SentinelOfflineBanner
import com.sentinel.android.core.designsystem.SentinelPalette
import com.sentinel.android.core.designsystem.SentinelSkeletonCard
import com.sentinel.android.core.designsystem.SentinelStatusBadge
import com.sentinel.android.core.designsystem.SentinelTransparencyBanner
import com.sentinel.android.domain.model.ActivityLog
import com.sentinel.android.domain.model.DeviceConnectionStatus
import com.sentinel.android.domain.model.EnrolledDevice
import com.sentinel.android.domain.model.LogSeverity
import com.sentinel.android.domain.model.MonitoringSession
import com.sentinel.android.domain.model.SessionStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    operatorName: String,
    isOnline: Boolean,
    devicesState: UiState<List<EnrolledDevice>>,
    sessionsState: UiState<List<MonitoringSession>>,
    logsState: UiState<List<ActivityLog>>,
    onNavigateToEnrollment: () -> Unit,
    onNavigateToDevices: () -> Unit,
    onNavigateToCommand: () -> Unit,
    onNavigateToMediaStudio: () -> Unit,
    onNavigateToActivityLogs: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenLiveSession: (String) -> Unit
) {
    val strings = LocalSentinelStrings.current
    val devices = when (devicesState) {
        is UiState.Success -> devicesState.data
        is UiState.Offline -> devicesState.cachedData
        else -> emptyList()
    }
    val sessions = when (sessionsState) {
        is UiState.Success -> sessionsState.data
        is UiState.Offline -> sessionsState.cachedData
        else -> emptyList()
    }
    val activeSessions = sessions.filter {
        it.statusEnum() == SessionStatus.ACTIVE || it.statusEnum() == SessionStatus.PAUSED
    }
    val onlineCount = devices.count { it.statusEnum() == DeviceConnectionStatus.ONLINE }
    val offlineCount = devices.count { it.statusEnum() != DeviceConnectionStatus.ONLINE }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.dashboardTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = operatorName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.testTag("dashboard_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = strings.settingsTitle,
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        if (!isOnline) {
            item {
                SentinelOfflineBanner(message = strings.offlineBanner)
            }
        }

        // Security Status Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_sentinel_hero_1795200152850),
                    contentDescription = strings.securityStatus,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    SentinelPalette.MidnightBg.copy(alpha = 0.92f),
                                    SentinelPalette.MidnightBg.copy(alpha = 0.35f)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SentinelStatusBadge(
                                label = strings.protectedLabel,
                                color = SentinelPalette.SuccessGreen,
                                pulsing = true
                            )
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = SentinelPalette.CyanAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = strings.securityStatus,
                                style = MaterialTheme.typography.titleLarge,
                                color = SentinelPalette.TextPrimaryDark
                            )
                            Text(
                                text = "TLS 1.3 · Keystore AES-256 · Zero-Trust Rules Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = SentinelPalette.CyanAccent
                            )
                        }
                    }
                }
            }
        }

        // 2x2 Telemetry KPI Grid
        item {
            if (devicesState is UiState.Loading) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SentinelSkeletonCard(modifier = Modifier.weight(1f), height = 96.dp)
                    SentinelSkeletonCard(modifier = Modifier.weight(1f), height = 96.dp)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SentinelMetricTile(
                            label = strings.registeredDevices,
                            value = devices.size.toString(),
                            icon = Icons.Filled.Devices,
                            accentColor = SentinelPalette.ElectricBlue,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToDevices() }
                                .testTag("metric_registered_devices")
                        )
                        SentinelMetricTile(
                            label = strings.onlineNow,
                            value = onlineCount.toString(),
                            icon = Icons.Filled.Wifi,
                            accentColor = SentinelPalette.SuccessGreen,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToDevices() }
                                .testTag("metric_online_devices")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SentinelMetricTile(
                            label = strings.offlineDevices,
                            value = offlineCount.toString(),
                            icon = Icons.Filled.PhonelinkOff,
                            accentColor = SentinelPalette.WarningAmber,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToDevices() }
                                .testTag("metric_offline_devices")
                        )
                        SentinelMetricTile(
                            label = strings.activeSessions,
                            value = activeSessions.size.toString(),
                            icon = Icons.Filled.CastConnected,
                            accentColor = SentinelPalette.CyanAccent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToCommand() }
                                .testTag("metric_active_sessions")
                        )
                    }
                }
            }
        }

        // Active Authorized Sessions Banner
        if (activeSessions.isNotEmpty()) {
            item {
                Text(
                    text = strings.activeSessions,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(activeSessions, key = { it.id }) { session ->
                SentinelGlassCard(
                    accentColor = SentinelPalette.CyanAccent,
                    modifier = Modifier
                        .clickable { onOpenLiveSession(session.id) }
                        .testTag("active_session_card_${session.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.deviceAlias,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${session.sessionType.replace('_', ' ')} · ${session.participantName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        SentinelStatusBadge(
                            label = session.status,
                            color = if (session.statusEnum() == SessionStatus.ACTIVE) {
                                SentinelPalette.SuccessGreen
                            } else {
                                SentinelPalette.WarningAmber
                            },
                            pulsing = session.statusEnum() == SessionStatus.ACTIVE
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onOpenLiveSession(session.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SentinelPalette.ElectricBlue)
                    ) {
                        Text("Open Live Session Controls")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Quick Actions Grid
        item {
            Text(
                text = strings.quickActions,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    label = strings.enrollDevice,
                    icon = Icons.Filled.AddToQueue,
                    accent = SentinelPalette.ElectricBlue,
                    onClick = onNavigateToEnrollment,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_enroll")
                )
                QuickActionCard(
                    label = strings.openCommand,
                    icon = Icons.Filled.SettingsRemote,
                    accent = SentinelPalette.CyanAccent,
                    onClick = onNavigateToCommand,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_command")
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    label = strings.mediaStudio,
                    icon = Icons.Filled.AutoAwesome,
                    accent = SentinelPalette.SuccessGreen,
                    onClick = onNavigateToMediaStudio,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_studio")
                )
                QuickActionCard(
                    label = strings.activityLogs,
                    icon = Icons.Filled.History,
                    accent = SentinelPalette.WarningAmber,
                    onClick = onNavigateToActivityLogs,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_logs")
                )
            }
        }

        item {
            SentinelTransparencyBanner(text = strings.transparencyNotice)
        }

        // Recent Activity Audit Trail
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.recentActivity,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelLarge,
                    color = SentinelPalette.CyanAccent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigateToActivityLogs() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("dashboard_view_all_logs")
                )
            }
        }

        when (logsState) {
            is UiState.Loading -> {
                items(3) { SentinelSkeletonCard(height = 76.dp) }
            }
            is UiState.Error -> {
                item { SentinelErrorCard(message = logsState.message) }
            }
            is UiState.Empty -> {
                item {
                    SentinelEmptyState(
                        title = "No Audit Events Yet",
                        description = "Every device enrollment, permission update, and approved monitoring session will appear here with verifiable timestamps.",
                        actionLabel = strings.enrollDevice,
                        onAction = onNavigateToEnrollment
                    )
                }
            }
            is UiState.Success -> {
                items(logsState.data.take(6), key = { it.id }) { log ->
                    CompactAuditRow(log = log)
                }
            }
            is UiState.Offline -> {
                items(logsState.cachedData.take(6), key = { it.id }) { log ->
                    CompactAuditRow(log = log)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun QuickActionCard(
    label: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(74.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = label, tint = accent, modifier = Modifier.size(20.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CompactAuditRow(log: ActivityLog) {
    val badgeColor = when (log.severityEnum()) {
        LogSeverity.SUCCESS -> SentinelPalette.SuccessGreen
        LogSeverity.WARNING -> SentinelPalette.WarningAmber
        LogSeverity.CRITICAL -> SentinelPalette.ErrorRed
        LogSeverity.INFO -> SentinelPalette.CyanAccent
    }
    val formatter = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
    val timeText = formatter.format(log.createdAt?.toDate() ?: Date())

    SentinelGlassCard(cornerRadius = 16.dp, contentPadding = 13.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = log.action,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            SentinelStatusBadge(label = log.category, color = badgeColor)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = log.details,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = timeText + if (log.sessionId.isNotBlank()) " · Session ${log.sessionId.take(8)}" else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}
