package com.sentinel.android.feature.enrollment

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
import com.sentinel.android.core.common.LocalSentinelStrings
import com.sentinel.android.core.common.UiState
import com.sentinel.android.core.designsystem.JetBrainsMonoFamily
import com.sentinel.android.core.designsystem.SentinelEmptyState
import com.sentinel.android.core.designsystem.SentinelErrorCard
import com.sentinel.android.core.designsystem.SentinelGlassCard
import com.sentinel.android.core.designsystem.SentinelPalette
import com.sentinel.android.core.designsystem.SentinelQrCodeCard
import com.sentinel.android.core.designsystem.SentinelSkeletonCard
import com.sentinel.android.core.designsystem.SentinelStatusBadge
import com.sentinel.android.core.designsystem.SentinelTransparencyBanner
import com.sentinel.android.domain.model.EnrollmentRequest
import com.sentinel.android.domain.model.EnrollmentStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceEnrollmentScreen(
    enrollmentsState: UiState<List<EnrollmentRequest>>,
    actionInProgress: Boolean,
    onBack: () -> Unit,
    onCreateInvitation: (alias: String, permissions: List<String>) -> Unit,
    onApproveRequest: (EnrollmentRequest) -> Unit,
    onRejectRequest: (EnrollmentRequest) -> Unit,
    onCancelRequest: (EnrollmentRequest) -> Unit
) {
    val strings = LocalSentinelStrings.current
    var aliasInput by remember { mutableStateOf("") }
    var permScreenShare by remember { mutableStateOf(true) }
    var permCamera by remember { mutableStateOf(false) }
    var permMediaPicker by remember { mutableStateOf(true) }
    var permFilesSaf by remember { mutableStateOf(true) }

    val enrollments = when (enrollmentsState) {
        is UiState.Success -> enrollmentsState.data
        is UiState.Offline -> enrollmentsState.cachedData
        else -> emptyList()
    }
    val pendingRequests = enrollments.filter { it.statusEnum() == EnrollmentStatus.PENDING }
    val historyRequests = enrollments.filter { it.statusEnum() != EnrollmentStatus.PENDING }

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
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("enrollment_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.enrollmentTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            SentinelTransparencyBanner(
                text = "Secondary devices must review all requested scopes and explicitly tap Approve before enrollment completes."
            )
        }

        // Create New Enrollment Code & QR
        item {
            SentinelGlassCard(accentColor = SentinelPalette.ElectricBlue.copy(alpha = 0.6f)) {
                Text(
                    text = "1. Configure Enrollment Scope",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = aliasInput,
                    onValueChange = { if (it.length <= 60) aliasInput = it },
                    label = { Text("Device Alias (e.g., Field Tablet 02)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("enrollment_alias_input"),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Requested Transparent Permissions",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = permScreenShare,
                        onClick = { permScreenShare = !permScreenShare },
                        label = { Text("Screen Share") }
                    )
                    FilterChip(
                        selected = permCamera,
                        onClick = { permCamera = !permCamera },
                        label = { Text("Visible Camera") }
                    )
                    FilterChip(
                        selected = permMediaPicker,
                        onClick = { permMediaPicker = !permMediaPicker },
                        label = { Text("Photo Picker") }
                    )
                    FilterChip(
                        selected = permFilesSaf,
                        onClick = { permFilesSaf = !permFilesSaf },
                        label = { Text("SAF Files") }
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        val selected = buildList {
                            if (permScreenShare) add("SCREEN_SHARE")
                            if (permCamera) add("CAMERA")
                            if (permMediaPicker) add("MEDIA_PICKER")
                            if (permFilesSaf) add("FILES_SAF")
                        }
                        onCreateInvitation(aliasInput, selected)
                        aliasInput = ""
                    },
                    enabled = !actionInProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("generate_enrollment_code_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SentinelPalette.ElectricBlue)
                ) {
                    Icon(Icons.Filled.QrCode2, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.generateCode)
                }
            }
        }

        // Active Pending Enrollment Invitations (with QR + Secondary Device Instructions + Explicit Consent Controls)
        if (pendingRequests.isNotEmpty()) {
            item {
                Text(
                    text = "Awaiting Explicit Device Owner Confirmation (${pendingRequests.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(pendingRequests, key = { it.id }) { req ->
                SentinelGlassCard(accentColor = SentinelPalette.CyanAccent) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = req.targetAlias,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        SentinelStatusBadge(label = req.status, color = SentinelPalette.WarningAmber, pulsing = true)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SentinelQrCodeCard(payload = "sentinel://enroll?code=${req.enrollmentCode}&alias=${req.targetAlias}")
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = req.enrollmentCode,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                letterSpacing = 2.sp
                            ),
                            color = SentinelPalette.CyanAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Secondary Device Instructions:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "1. Open Android Sentinel on the target device.\n2. Scan the QR code or verify code ${req.enrollmentCode}.\n3. Review every requested permission below and explicitly Approve or Reject.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        req.requestedPermissions.forEach { perm ->
                            SentinelStatusBadge(label = perm, color = SentinelPalette.ElectricBlue)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { onApproveRequest(req) },
                        enabled = !actionInProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("approve_enrollment_${req.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SentinelPalette.SuccessGreen)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = SentinelPalette.MidnightBg)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.confirmEnrollment, color = SentinelPalette.MidnightBg, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onRejectRequest(req) },
                            enabled = !actionInProgress,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("reject_enrollment_${req.id}"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = SentinelPalette.ErrorRed)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.rejectEnrollment, color = SentinelPalette.ErrorRed)
                        }
                        OutlinedButton(
                            onClick = { onCancelRequest(req) },
                            enabled = !actionInProgress,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("cancel_enrollment_${req.id}"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(strings.cancelEnrollment)
                        }
                    }
                }
            }
        }

        // Enrollment Audit Record History
        item {
            Text(
                text = "Enrollment Audit Record",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        when (enrollmentsState) {
            is UiState.Loading -> {
                items(2) { SentinelSkeletonCard(height = 84.dp) }
            }
            is UiState.Error -> {
                item { SentinelErrorCard(message = enrollmentsState.message) }
            }
            is UiState.Empty -> {
                item {
                    SentinelEmptyState(
                        title = "No Enrollment Records",
                        description = "Generate your first consent code above to pair an Android device with full transparency."
                    )
                }
            }
            else -> {
                if (historyRequests.isEmpty()) {
                    item {
                        Text(
                            text = "Completed approvals, rejections, and cancellations will be permanently audited here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(historyRequests, key = { it.id }) { item ->
                        val color = when (item.statusEnum()) {
                            EnrollmentStatus.APPROVED -> SentinelPalette.SuccessGreen
                            EnrollmentStatus.REJECTED -> SentinelPalette.ErrorRed
                            else -> SentinelPalette.WarningAmber
                        }
                        val fmt = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
                        val ts = fmt.format(item.createdAt?.toDate() ?: Date())
                        SentinelGlassCard(cornerRadius = 16.dp, contentPadding = 14.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.VerifiedUser,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(item.targetAlias, style = MaterialTheme.typography.titleSmall)
                                }
                                SentinelStatusBadge(label = item.status, color = color)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${item.enrollmentCode} · ${item.deviceModel} · $ts",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
