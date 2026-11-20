package com.sentinel.android.feature.sessions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CameraRear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sentinel.android.core.common.LocalSentinelStrings
import com.sentinel.android.core.common.UiState
import com.sentinel.android.core.designsystem.SentinelEmptyState
import com.sentinel.android.core.designsystem.SentinelGlassCard
import com.sentinel.android.core.designsystem.SentinelPalette
import com.sentinel.android.core.designsystem.SentinelStatusBadge
import com.sentinel.android.core.designsystem.SentinelTransparencyBanner
import com.sentinel.android.domain.model.DeviceConnectionStatus
import com.sentinel.android.domain.model.EnrolledDevice
import com.sentinel.android.domain.model.MonitoringSession
import com.sentinel.android.domain.model.SessionStatus
import com.sentinel.android.domain.model.SessionType
import com.sentinel.android.domain.model.SharedFileDescriptor
import com.sentinel.android.service.ScreenShareForegroundService

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommandCenterScreen(
    devicesState: UiState<List<EnrolledDevice>>,
    sessionsState: UiState<List<MonitoringSession>>,
    sharedItems: List<SharedFileDescriptor>,
    onNavigateToEnrollment: () -> Unit,
    onStartSession: (
        device: EnrolledDevice,
        sessionType: SessionType,
        activePermissions: List<String>,
        summary: String,
        onStarted: (String) -> Unit
    ) -> Unit,
    onOpenLiveSession: (String) -> Unit,
    onTerminateSession: (String) -> Unit,
    onRecordSharedItem: (EnrolledDevice?, SharedFileDescriptor, Boolean) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val strings = LocalSentinelStrings.current
    val context = LocalContext.current

    val eligibleDevices = when (devicesState) {
        is UiState.Success -> devicesState.data.filter { it.statusEnum() != DeviceConnectionStatus.REVOKED }
        is UiState.Offline -> devicesState.cachedData.filter { it.statusEnum() != DeviceConnectionStatus.REVOKED }
        else -> emptyList()
    }
    val activeSessions = when (sessionsState) {
        is UiState.Success -> sessionsState.data.filter {
            it.statusEnum() == SessionStatus.ACTIVE || it.statusEnum() == SessionStatus.PAUSED
        }
        else -> emptyList()
    }

    var selectedDeviceId by remember(eligibleDevices) {
        mutableStateOf(eligibleDevices.firstOrNull()?.id)
    }
    val selectedDevice = eligibleDevices.firstOrNull { it.id == selectedDeviceId } ?: eligibleDevices.firstOrNull()

    var pendingConsentType by remember { mutableStateOf<SessionType?>(null) }
    var pendingCameraType by remember { mutableStateOf<SessionType?>(null) }

    // 1. Android Photo Picker (Zero-permission system picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val descriptor = inspectUriMetadata(context, uri, fallbackMime = "image/*")
            onRecordSharedItem(selectedDevice, descriptor, true)
        }
    }

    // 2. Android Storage Access Framework (SAF) document picker
    val safDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val descriptor = inspectUriMetadata(context, uri, fallbackMime = "application/octet-stream")
            onRecordSharedItem(selectedDevice, descriptor, false)
        }
    }

    // 3. Runtime Camera permission launcher (Front/Rear CameraX sessions)
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val device = selectedDevice
        val camType = pendingCameraType
        pendingCameraType = null
        if (granted && device != null && camType != null) {
            onStartSession(
                device,
                camType,
                listOf("CAMERA", if (camType == SessionType.FRONT_CAMERA) "FRONT_LENS" else "REAR_LENS"),
                "Visible CameraX preview approved by owner"
            ) { sessionId ->
                onOpenLiveSession(sessionId)
            }
        } else if (!granted) {
            onShowMessage("Camera permission denied. Camera sessions require explicit owner approval.")
        }
    }

    // 4. System MediaProjection permission launcher
    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val device = selectedDevice
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null && device != null) {
            onStartSession(
                device,
                SessionType.SCREEN_SHARE,
                listOf("MEDIA_PROJECTION", "FOREGROUND_NOTIFICATION"),
                "Visible screen streaming with ongoing system notification"
            ) { sessionId ->
                ScreenShareForegroundService.start(context, sessionId, result.resultCode, data)
                onOpenLiveSession(sessionId)
            }
        } else {
            onShowMessage("Screen sharing permission cancelled by device owner.")
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCaptureLauncher.launch(manager.createScreenCaptureIntent())
    }

    fun launchScreenShareFlow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            screenCaptureLauncher.launch(manager.createScreenCaptureIntent())
        }
    }

    fun launchCameraFlow(type: SessionType) {
        val device = selectedDevice ?: return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            onStartSession(
                device,
                type,
                listOf("CAMERA", if (type == SessionType.FRONT_CAMERA) "FRONT_LENS" else "REAR_LENS"),
                "Visible CameraX preview approved by owner"
            ) { sessionId ->
                onOpenLiveSession(sessionId)
            }
        } else {
            pendingCameraType = type
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Explicit Consent Confirmation Dialog before starting Camera or Screen Share
    if (pendingConsentType != null && selectedDevice != null) {
        val type = pendingConsentType!!
        val description = when (type) {
            SessionType.SCREEN_SHARE ->
                "Starting Screen Streaming triggers Android's system MediaProjection consent dialog and displays a persistent ongoing notification with an immediate Stop control."
            SessionType.FRONT_CAMERA ->
                "Starting Front Camera Session displays a live visible CameraX preview on screen with active permission badges and an instant Stop button."
            SessionType.REAR_CAMERA ->
                "Starting Rear Camera Session displays a live visible CameraX preview on screen with active permission badges and an instant Stop button."
            else -> "Confirm explicit owner consent to proceed."
        }
        AlertDialog(
            onDismissRequest = { pendingConsentType = null },
            title = { Text("Explicit Owner Consent Required") },
            text = { Text(description) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingConsentType = null
                        when (type) {
                            SessionType.SCREEN_SHARE -> launchScreenShareFlow()
                            SessionType.FRONT_CAMERA, SessionType.REAR_CAMERA -> launchCameraFlow(type)
                            else -> Unit
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SentinelPalette.ElectricBlue)
                ) {
                    Text("Approve & Start")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConsentType = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = strings.commandTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Explicit User-Consented Controls · Zero Stealth Execution",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            SentinelTransparencyBanner(text = strings.transparencyNotice)
        }

        // Immediate Termination Controls for Active Sessions
        if (activeSessions.isNotEmpty()) {
            item {
                Text(
                    text = "Active Authorized Sessions (${activeSessions.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(activeSessions, key = { it.id }) { session ->
                SentinelGlassCard(accentColor = SentinelPalette.ErrorRed.copy(alpha = 0.7f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(session.deviceAlias, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${session.sessionType.replace('_', ' ')} · ${session.status}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SentinelPalette.CyanAccent
                            )
                        }
                        SentinelStatusBadge(label = "LIVE", color = SentinelPalette.SuccessGreen, pulsing = true)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onOpenLiveSession(session.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("View Live")
                        }
                        Button(
                            onClick = {
                                if (session.typeEnum() == SessionType.SCREEN_SHARE) {
                                    ScreenShareForegroundService.stop(context)
                                }
                                onTerminateSession(session.id)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("terminate_session_${session.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SentinelPalette.ErrorRed)
                        ) {
                            Icon(Icons.Filled.StopCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.stopNow)
                        }
                    }
                }
            }
        }

        // Target Device Selector
        if (eligibleDevices.isEmpty()) {
            item {
                SentinelEmptyState(
                    title = "Enroll a Device First",
                    description = "Command Center capabilities require at least one consented, non-revoked enrolled device.",
                    actionLabel = strings.enrollDevice,
                    onAction = onNavigateToEnrollment
                )
            }
        } else {
            item {
                SentinelGlassCard(cornerRadius = 16.dp, contentPadding = 14.dp) {
                    Text(
                        text = "Target Enrolled Device",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        eligibleDevices.forEach { dev ->
                            FilterChip(
                                selected = dev.id == selectedDevice?.id,
                                onClick = { selectedDeviceId = dev.id },
                                label = { Text(dev.deviceAlias) }
                            )
                        }
                    }
                }
            }

            // 5 Consented Command Cards
            item {
                CommandActionTile(
                    title = strings.screenSharingSession,
                    subtitle = "Android MediaProjection · System confirmation & ongoing notification",
                    icon = Icons.AutoMirrored.Filled.ScreenShare,
                    accent = SentinelPalette.ElectricBlue,
                    enabled = selectedDevice?.screenShareAllowed == true,
                    disabledReason = "Enable Screen Share permission in Device Manager",
                    testTag = "command_screen_share",
                    onClick = { pendingConsentType = SessionType.SCREEN_SHARE }
                )
            }
            item {
                CommandActionTile(
                    title = strings.frontCameraSession,
                    subtitle = "Visible CameraX front lens preview · Explicit owner confirmation",
                    icon = Icons.Filled.CameraFront,
                    accent = SentinelPalette.CyanAccent,
                    enabled = selectedDevice?.cameraAllowed == true,
                    disabledReason = "Enable Camera permission in Device Manager",
                    testTag = "command_front_camera",
                    onClick = { pendingConsentType = SessionType.FRONT_CAMERA }
                )
            }
            item {
                CommandActionTile(
                    title = strings.rearCameraSession,
                    subtitle = "Visible CameraX rear lens preview · Explicit owner confirmation",
                    icon = Icons.Filled.CameraRear,
                    accent = SentinelPalette.CyanAccent,
                    enabled = selectedDevice?.cameraAllowed == true,
                    disabledReason = "Enable Camera permission in Device Manager",
                    testTag = "command_rear_camera",
                    onClick = { pendingConsentType = SessionType.REAR_CAMERA }
                )
            }
            item {
                CommandActionTile(
                    title = strings.sharePhotosVideos,
                    subtitle = "Zero-permission Android System Photo Picker · Owner selects items explicitly",
                    icon = Icons.Filled.PhotoLibrary,
                    accent = SentinelPalette.SuccessGreen,
                    enabled = selectedDevice?.mediaPickerAllowed == true,
                    disabledReason = "Enable Photo Picker permission in Device Manager",
                    testTag = "command_photo_picker",
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    }
                )
            }
            item {
                CommandActionTile(
                    title = strings.shareFilesSaf,
                    subtitle = "Android Storage Access Framework (SAF) · Never scans private storage",
                    icon = Icons.Filled.FolderOpen,
                    accent = SentinelPalette.WarningAmber,
                    enabled = selectedDevice?.filesAllowed == true,
                    disabledReason = "Enable SAF Files permission in Device Manager",
                    testTag = "command_saf_files",
                    onClick = { safDocumentLauncher.launch(arrayOf("*/*")) }
                )
            }
        }

        if (sharedItems.isNotEmpty()) {
            item {
                Text(
                    text = "Explicitly Shared Items This Session (${sharedItems.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(sharedItems, key = { "${it.uriString}_${it.sharedAtMillis}" }) { item ->
                SentinelGlassCard(cornerRadius = 14.dp, contentPadding = 12.dp) {
                    Text(item.displayName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "${item.mimeType} · ${formatBytes(item.sizeBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SentinelPalette.CyanAccent
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun CommandActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    enabled: Boolean,
    disabledReason: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (enabled) accent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background((if (enabled) accent else Color.Gray).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (enabled) accent else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (enabled) subtitle else disabledReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else SentinelPalette.WarningAmber
                )
            }
            SentinelStatusBadge(
                label = if (enabled) "READY" else "LOCKED",
                color = if (enabled) accent else SentinelPalette.WarningAmber
            )
        }
    }
}

private fun inspectUriMetadata(
    context: Context,
    uri: Uri,
    fallbackMime: String
): SharedFileDescriptor {
    var displayName = uri.lastPathSegment ?: "Selected Item"
    var sizeBytes = 0L
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
                    if (sizeIdx >= 0) sizeBytes = cursor.getLong(sizeIdx)
                }
            }
    }
    val mime = context.contentResolver.getType(uri) ?: fallbackMime
    return SharedFileDescriptor(
        uriString = uri.toString(),
        displayName = displayName,
        mimeType = mime,
        sizeBytes = sizeBytes,
        sharedAtMillis = System.currentTimeMillis()
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "Size verified by system"
    val kb = bytes / 1024.0
    return if (kb < 1024.0) "%.1f KB".format(kb) else "%.2f MB".format(kb / 1024.0)
}
