package com.sentinel.android.feature.devices

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sentinel.android.core.common.LocalSentinelStrings
import com.sentinel.android.core.common.UiState
import com.sentinel.android.core.designsystem.SentinelEmptyState
import com.sentinel.android.core.designsystem.SentinelErrorCard
import com.sentinel.android.core.designsystem.SentinelGlassCard
import com.sentinel.android.core.designsystem.SentinelOfflineBanner
import com.sentinel.android.core.designsystem.SentinelPalette
import com.sentinel.android.core.designsystem.SentinelSkeletonCard
import com.sentinel.android.core.designsystem.SentinelStatusBadge
import com.sentinel.android.domain.model.DeviceConnectionStatus
import com.sentinel.android.domain.model.EnrolledDevice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeviceManagerScreen(
    isOnline: Boolean,
    devicesState: UiState<List<EnrolledDevice>>,
    actionInProgress: Boolean,
    onOpenEnrollment: () -> Unit,
    onEnrollCurrentDevice: (String) -> Unit,
    onUpdatePermissions: (
        device: EnrolledDevice,
        camera: Boolean,
        screenShare: Boolean,
        mediaPicker: Boolean,
        files: Boolean,
        status: DeviceConnectionStatus
    ) -> Unit,
    onRevokeDevice: (EnrolledDevice) -> Unit
) {
    val strings = LocalSentinelStrings.current
    var pendingRevokeDevice by remember { mutableStateOf<EnrolledDevice?>(null) }

    if (pendingRevokeDevice != null) {
        val target = pendingRevokeDevice!!
        AlertDialog(
            onDismissRequest = { pendingRevokeDevice = null },
            title = { Text("Revoke Device Access?") },
            text = {
                Text(
                    "Revoking '${target.deviceAlias}' permanently disables all permissions and blocks future monitoring sessions on this device."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRevokeDevice(target)
                        pendingRevokeDevice = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SentinelPalette.ErrorRed)
                ) {
                    Text(strings.revokeDevice)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRevokeDevice = null }) {
                    Text(strings.cancelEnrollment)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.devicesTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = onOpenEnrollment,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("devices_open_enrollment_button")
                ) {
                    Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("QR Enroll")
                }
            }
        }

        if (!isOnline) {
            item { SentinelOfflineBanner(message = strings.offlineBanner) }
        }

        item {
            Button(
                onClick = {
                    val defaultAlias = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
                    onEnrollCurrentDevice(defaultAlias)
                },
                enabled = !actionInProgress && isOnline,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("enroll_current_device_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SentinelPalette.ElectricBlue)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.enrollThisDevice)
            }
        }

        when (devicesState) {
            is UiState.Loading -> {
                items(3) { SentinelSkeletonCard(height = 170.dp) }
            }
            is UiState.Error -> {
                item { SentinelErrorCard(message = devicesState.message) }
            }
            is UiState.Empty -> {
                item {
                    SentinelEmptyState(
                        title = strings.noDevicesTitle,
                        description = strings.noDevicesBody,
                        actionLabel = strings.enrollDevice,
                        onAction = onOpenEnrollment
                    )
                }
            }
            is UiState.Success -> {
                items(devicesState.data, key = { it.id }) { device ->
                    DeviceDetailCard(
                        device = device,
                        canMutate = isOnline && device.statusEnum() != DeviceConnectionStatus.REVOKED,
                        onUpdatePermissions = onUpdatePermissions,
                        onRequestRevoke = { pendingRevokeDevice = device }
                    )
                }
            }
            is UiState.Offline -> {
                items(devicesState.cachedData, key = { it.id }) { device ->
                    DeviceDetailCard(
                        device = device,
                        canMutate = false,
                        onUpdatePermissions = onUpdatePermissions,
                        onRequestRevoke = { pendingRevokeDevice = device }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun DeviceDetailCard(
    device: EnrolledDevice,
    canMutate: Boolean,
    onUpdatePermissions: (
        device: EnrolledDevice,
        camera: Boolean,
        screenShare: Boolean,
        mediaPicker: Boolean,
        files: Boolean,
        status: DeviceConnectionStatus
    ) -> Unit,
    onRequestRevoke: () -> Unit
) {
    val strings = LocalSentinelStrings.current
    val status = device.statusEnum()
    val statusColor = when (status) {
        DeviceConnectionStatus.ONLINE -> SentinelPalette.SuccessGreen
        DeviceConnectionStatus.OFFLINE -> SentinelPalette.WarningAmber
        DeviceConnectionStatus.REVOKED -> SentinelPalette.ErrorRed
    }
    val fmt = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
    val enrolledDate = fmt.format(device.enrolledAt?.toDate() ?: Date())
    val lastActivity = fmt.format(device.lastActivityAt?.toDate() ?: Date())

    SentinelGlassCard(accentColor = statusColor.copy(alpha = 0.55f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.PhoneAndroid,
                    contentDescription = device.deviceAlias,
                    tint = SentinelPalette.CyanAccent,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = device.deviceAlias,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${device.deviceModel} · ${device.androidVersion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            SentinelStatusBadge(
                label = status.name,
                color = statusColor,
                pulsing = status == DeviceConnectionStatus.ONLINE
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Enrolled: $enrolledDate  |  Last Active: $lastActivity",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Code: ${device.enrollmentCode}  |  Consent: ${if (device.consentVerified) "Verified" else "Pending"}",
            style = MaterialTheme.typography.labelSmall,
            color = SentinelPalette.CyanAccent
        )

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(8.dp))

        PermissionToggleRow(
            label = "Screen Streaming (MediaProjection)",
            checked = device.screenShareAllowed,
            enabled = canMutate,
            onCheckedChange = {
                onUpdatePermissions(device, device.cameraAllowed, it, device.mediaPickerAllowed, device.filesAllowed, status)
            }
        )
        PermissionToggleRow(
            label = "Visible Camera Sessions (CameraX)",
            checked = device.cameraAllowed,
            enabled = canMutate,
            onCheckedChange = {
                onUpdatePermissions(device, it, device.screenShareAllowed, device.mediaPickerAllowed, device.filesAllowed, status)
            }
        )
        PermissionToggleRow(
            label = "Shared Photos & Videos (Photo Picker)",
            checked = device.mediaPickerAllowed,
            enabled = canMutate,
            onCheckedChange = {
                onUpdatePermissions(device, device.cameraAllowed, device.screenShareAllowed, it, device.filesAllowed, status)
            }
        )
        PermissionToggleRow(
            label = "Shared Documents (SAF)",
            checked = device.filesAllowed,
            enabled = canMutate,
            onCheckedChange = {
                onUpdatePermissions(device, device.cameraAllowed, device.screenShareAllowed, device.mediaPickerAllowed, it, status)
            }
        )

        if (status != DeviceConnectionStatus.REVOKED) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val toggled = if (status == DeviceConnectionStatus.ONLINE) {
                            DeviceConnectionStatus.OFFLINE
                        } else {
                            DeviceConnectionStatus.ONLINE
                        }
                        onUpdatePermissions(
                            device,
                            device.cameraAllowed,
                            device.screenShareAllowed,
                            device.mediaPickerAllowed,
                            device.filesAllowed,
                            toggled
                        )
                    },
                    enabled = canMutate,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (status == DeviceConnectionStatus.ONLINE) "Set Offline" else "Set Online")
                }
                OutlinedButton(
                    onClick = onRequestRevoke,
                    enabled = canMutate,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("revoke_device_${device.id}"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(16.dp), tint = SentinelPalette.ErrorRed)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.revokeDevice, color = SentinelPalette.ErrorRed)
                }
            }
        }
    }
}

@Composable
private fun PermissionToggleRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
