package com.sentinel.android.feature.sessions

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentinel.android.core.common.LocalSentinelStrings
import com.sentinel.android.core.designsystem.JetBrainsMonoFamily
import com.sentinel.android.core.designsystem.SentinelEmptyState
import com.sentinel.android.core.designsystem.SentinelGlassCard
import com.sentinel.android.core.designsystem.SentinelPalette
import com.sentinel.android.core.designsystem.SentinelStatusBadge
import com.sentinel.android.domain.model.MonitoringSession
import com.sentinel.android.domain.model.SessionStatus
import com.sentinel.android.domain.model.SessionType
import com.sentinel.android.service.ScreenShareForegroundService
import com.sentinel.android.service.ScreenShareRuntimeHolder
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveSessionScreen(
    session: MonitoringSession?,
    onBack: () -> Unit,
    onTogglePause: (MonitoringSession) -> Unit,
    onTerminate: (String) -> Unit
) {
    val strings = LocalSentinelStrings.current
    val context = LocalContext.current
    val screenShareState by ScreenShareRuntimeHolder.state.collectAsStateWithLifecycle()

    var elapsedSeconds by remember(session?.id) { mutableLongStateOf(0L) }
    val isActive = session?.statusEnum() == SessionStatus.ACTIVE
    val isPaused = session?.statusEnum() == SessionStatus.PAUSED
    val isTerminated = session?.statusEnum() == SessionStatus.TERMINATED

    LaunchedEffect(session?.id, session?.status) {
        val startMs = session?.startedAt?.toDate()?.time ?: System.currentTimeMillis()
        while (session != null && session.statusEnum() != SessionStatus.TERMINATED) {
            if (session.statusEnum() == SessionStatus.ACTIVE) {
                elapsedSeconds = ((System.currentTimeMillis() - startMs) / 1000L).coerceAtLeast(0L)
            }
            delay(1000L)
        }
    }

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("live_session_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = strings.liveSessionTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (session != null) {
                    SentinelStatusBadge(
                        label = session.status,
                        color = when (session.statusEnum()) {
                            SessionStatus.ACTIVE -> SentinelPalette.SuccessGreen
                            SessionStatus.PAUSED -> SentinelPalette.WarningAmber
                            else -> SentinelPalette.ErrorRed
                        },
                        pulsing = isActive
                    )
                }
            }
        }

        if (session == null) {
            item {
                SentinelEmptyState(
                    title = "Session Not Found",
                    description = "This monitoring session has ended or is no longer active.",
                    actionLabel = "Return to Command Center",
                    onAction = onBack
                )
            }
        } else {
            // Session Telemetry Strip (Duration & Connection Quality)
            item {
                SentinelGlassCard(accentColor = SentinelPalette.CyanAccent) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DURATION",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatDuration(elapsedSeconds),
                                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFamily),
                                color = SentinelPalette.CyanAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "CONNECTION QUALITY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            SentinelStatusBadge(
                                label = session.connectionQuality,
                                color = if (isTerminated) SentinelPalette.ErrorRed else SentinelPalette.SuccessGreen
                            )
                        }
                    }
                }
            }

            // Live Preview Surface (CameraX OR MediaProjection Bitmap)
            item {
                SentinelGlassCard(
                    accentColor = if (isActive) SentinelPalette.ElectricBlue else SentinelPalette.WarningAmber,
                    contentPadding = 12.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SentinelPalette.MidnightBg),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isTerminated -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Filled.StopCircle,
                                        contentDescription = null,
                                        tint = SentinelPalette.ErrorRed,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Session Terminated", color = Color.White, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            isPaused -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Filled.Pause,
                                        contentDescription = null,
                                        tint = SentinelPalette.WarningAmber,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Preview Paused by Owner", color = Color.White, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            session.typeEnum() == SessionType.FRONT_CAMERA || session.typeEnum() == SessionType.REAR_CAMERA -> {
                                CameraXLivePreview(
                                    useFrontCamera = session.typeEnum() == SessionType.FRONT_CAMERA,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            session.typeEnum() == SessionType.SCREEN_SHARE -> {
                                val frame = screenShareState.previewFrame
                                if (frame != null) {
                                    Image(
                                        bitmap = frame.asImageBitmap(),
                                        contentDescription = "Shared Screen Preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(20.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ScreenShare,
                                            contentDescription = null,
                                            tint = SentinelPalette.CyanAccent,
                                            modifier = Modifier.size(46.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = if (screenShareState.active) {
                                                "MediaProjection Active · Waiting for display frame..."
                                            } else {
                                                screenShareState.errorMessage ?: "MediaProjection stream active on enrolled device"
                                            },
                                            color = SentinelPalette.TextPrimaryDark,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                            else -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Filled.Videocam,
                                        contentDescription = null,
                                        tint = SentinelPalette.CyanAccent,
                                        modifier = Modifier.size(42.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(session.sharedItemSummary, color = Color.White)
                                }
                            }
                        }

                        // Always-visible Live Transparency Overlay Badge
                        if (!isTerminated) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp)
                            ) {
                                SentinelStatusBadge(
                                    label = if (isPaused) "PAUSED" else "VISIBLE · ${session.sessionType}",
                                    color = if (isPaused) SentinelPalette.WarningAmber else SentinelPalette.ErrorRed,
                                    pulsing = isActive
                                )
                            }
                        }
                    }
                }
            }

            // Active Permission Indicators & Participant Identity
            item {
                SentinelGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = SentinelPalette.CyanAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Participant: ${session.participantName}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Device: ${session.deviceAlias}  |  Session ID: ${session.id.take(12)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Active Transparent Permission Indicators",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (session.activePermissions.isEmpty()) {
                        Text(
                            text = "All session permissions released.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SentinelPalette.SuccessGreen
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            session.activePermissions.forEach { perm ->
                                SentinelStatusBadge(label = perm, color = SentinelPalette.ElectricBlue)
                            }
                        }
                    }
                }
            }

            // Pause / Resume & Immediate Stop Controls
            if (!isTerminated) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (session.typeEnum() == SessionType.SCREEN_SHARE) {
                                    ScreenShareForegroundService.setPaused(context, isActive)
                                }
                                onTogglePause(session)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("live_session_pause_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = if (isActive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isActive) strings.pauseSession else strings.resumeSession)
                        }

                        Button(
                            onClick = {
                                if (session.typeEnum() == SessionType.SCREEN_SHARE) {
                                    ScreenShareForegroundService.stop(context)
                                }
                                onTerminate(session.id)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("live_session_stop_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SentinelPalette.ErrorRed)
                        ) {
                            Icon(Icons.Filled.StopCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.stopNow, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun CameraXLivePreview(
    useFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    DisposableEffect(useFrontCamera, lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        val listener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val selector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
            } catch (e: Exception) {
                Log.w("SentinelCameraX", "Camera preview bind failed: ${e.message}")
            }
        }
        cameraProviderFuture.addListener(listener, executor)

        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

private fun formatDuration(totalSeconds: Long): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return "%02d:%02d:%02d".format(hrs, mins, secs)
}
