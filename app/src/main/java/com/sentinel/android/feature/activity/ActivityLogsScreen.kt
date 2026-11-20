package com.sentinel.android.feature.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.sentinel.android.core.common.LocalSentinelStrings
import com.sentinel.android.core.common.UiState
import com.sentinel.android.core.designsystem.JetBrainsMonoFamily
import com.sentinel.android.core.designsystem.SentinelEmptyState
import com.sentinel.android.core.designsystem.SentinelErrorCard
import com.sentinel.android.core.designsystem.SentinelGlassCard
import com.sentinel.android.core.designsystem.SentinelOfflineBanner
import com.sentinel.android.core.designsystem.SentinelPalette
import com.sentinel.android.core.designsystem.SentinelSkeletonCard
import com.sentinel.android.core.designsystem.SentinelStatusBadge
import com.sentinel.android.domain.model.ActivityCategory
import com.sentinel.android.domain.model.ActivityLog
import com.sentinel.android.domain.model.LogSeverity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityLogsScreen(
    isOnline: Boolean,
    logsState: UiState<List<ActivityLog>>,
    onBack: () -> Unit
) {
    val strings = LocalSentinelStrings.current
    var selectedCategory by remember { mutableStateOf<ActivityCategory?>(null) }

    val allLogs = when (logsState) {
        is UiState.Success -> logsState.data
        is UiState.Offline -> logsState.cachedData
        else -> emptyList()
    }
    val filteredLogs = if (selectedCategory == null) {
        allLogs
    } else {
        allLogs.filter { it.categoryEnum() == selectedCategory }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("logs_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.activityLogs,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (!isOnline) {
            item { SentinelOfflineBanner(message = strings.offlineBanner) }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("ALL") }
                )
                ActivityCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.name) }
                    )
                }
            }
        }

        when (logsState) {
            is UiState.Loading -> {
                items(4) { SentinelSkeletonCard(height = 90.dp) }
            }
            is UiState.Error -> {
                item { SentinelErrorCard(message = logsState.message) }
            }
            is UiState.Empty -> {
                item {
                    SentinelEmptyState(
                        title = "Audit Trail Empty",
                        description = "Enrollment, consent, permission, session, and authentication events are recorded here automatically."
                    )
                }
            }
            else -> {
                if (filteredLogs.isEmpty()) {
                    item {
                        SentinelEmptyState(
                            title = "No Matching Events",
                            description = "No audit entries recorded under category ${selectedCategory?.name ?: "ALL"} yet."
                        )
                    }
                } else {
                    items(filteredLogs, key = { it.id }) { log ->
                        AuditLogEntryCard(log)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun AuditLogEntryCard(log: ActivityLog) {
    val color = when (log.severityEnum()) {
        LogSeverity.SUCCESS -> SentinelPalette.SuccessGreen
        LogSeverity.WARNING -> SentinelPalette.WarningAmber
        LogSeverity.CRITICAL -> SentinelPalette.ErrorRed
        LogSeverity.INFO -> SentinelPalette.CyanAccent
    }
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val ts = fmt.format(log.createdAt?.toDate() ?: Date())

    SentinelGlassCard(accentColor = color.copy(alpha = 0.45f), cornerRadius = 16.dp, contentPadding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = log.action,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            SentinelStatusBadge(label = log.category, color = color)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = log.details,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "TIME: $ts",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                color = SentinelPalette.CyanAccent
            )
            if (log.deviceId.isNotBlank() || log.sessionId.isNotBlank()) {
                Text(
                    text = buildString {
                        if (log.deviceId.isNotBlank()) append("DEV:${log.deviceId.take(10)}  ")
                        if (log.sessionId.isNotBlank()) append("SID:${log.sessionId.take(10)}")
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
