package com.sentinel.android.feature.media

import android.content.Intent
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sentinel.android.core.common.LocalSentinelStrings
import com.sentinel.android.core.designsystem.SentinelEmptyState
import com.sentinel.android.core.designsystem.SentinelErrorCard
import com.sentinel.android.core.designsystem.SentinelGlassCard
import com.sentinel.android.core.designsystem.SentinelPalette
import com.sentinel.android.core.designsystem.SentinelSkeletonCard
import com.sentinel.android.core.designsystem.SentinelTransparencyBanner
import com.sentinel.android.domain.model.StudioMediaItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaStudioScreen(
    genState: StudioGenerationUiState,
    savedMedia: List<StudioMediaItem>,
    onGenerate: (String) -> Unit,
    onSaveCurrent: () -> Unit,
    onGetShareUri: (String) -> android.net.Uri?,
    onDeleteItem: (StudioMediaItem) -> Unit
) {
    val strings = LocalSentinelStrings.current
    val context = LocalContext.current
    var promptText by remember { mutableStateOf("") }

    val quickPrompts = listOf(
        "Futuristic midnight blue digital shield emblem with cyan holographic rings",
        "Clean isometric enterprise network topology artwork in dark glass style",
        "Minimalist geometric satellite telemetry radar badge, electric blue glow"
    )

    fun shareSavedFile(filePath: String) {
        val uri = onGetShareUri(filePath) ?: return
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, strings.exportShare))
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
                text = strings.mediaStudioTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Authorized Firebase AI Logic Image Synthesis · Standard PNG Export",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            SentinelTransparencyBanner(
                text = "Standard clean PNG files only. Zero hidden payloads, zero executable content, safe FileProvider export."
            )
        }

        item {
            SentinelGlassCard(accentColor = SentinelPalette.ElectricBlue.copy(alpha = 0.6f)) {
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { if (it.length <= 240) promptText = it },
                    label = { Text("Describe original artwork to generate") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("media_studio_prompt_input"),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPrompts.forEachIndexed { idx, preset ->
                        AssistChip(
                            onClick = { promptText = preset },
                            label = {
                                Text(
                                    text = "Preset ${idx + 1}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onGenerate(promptText) },
                    enabled = promptText.trim().length >= 3 && genState !is StudioGenerationUiState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("media_studio_generate_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SentinelPalette.ElectricBlue)
                ) {
                    if (genState is StudioGenerationUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = androidx.compose.ui.graphics.Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Synthesizing (30–60s)...")
                    } else {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.generateImageAction)
                    }
                }
            }
        }

        // Generation Preview Canvas
        item {
            when (genState) {
                is StudioGenerationUiState.Idle -> Unit
                is StudioGenerationUiState.Loading -> {
                    SentinelGlassCard(accentColor = SentinelPalette.CyanAccent) {
                        SentinelSkeletonCard(height = 240.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Generating original high-resolution artwork via Firebase AI Logic (may take 30–60s)...",
                            style = MaterialTheme.typography.bodySmall,
                            color = SentinelPalette.CyanAccent
                        )
                    }
                }
                is StudioGenerationUiState.Error -> {
                    SentinelErrorCard(
                        message = genState.message,
                        onRetry = if (promptText.trim().length >= 3) {
                            { onGenerate(promptText) }
                        } else null
                    )
                }
                is StudioGenerationUiState.Success -> {
                    SentinelGlassCard(accentColor = SentinelPalette.SuccessGreen) {
                        Image(
                            bitmap = genState.bitmap.asImageBitmap(),
                            contentDescription = genState.prompt,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = genState.prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onSaveCurrent,
                                enabled = genState.savedPath == null,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("media_studio_save_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SentinelPalette.SuccessGreen)
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, tint = SentinelPalette.MidnightBg)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (genState.savedPath != null) "Saved" else strings.saveLocally,
                                    color = SentinelPalette.MidnightBg
                                )
                            }
                            if (genState.savedPath != null) {
                                OutlinedButton(
                                    onClick = { shareSavedFile(genState.savedPath) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("media_studio_share_current_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.exportShare)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Local Saved Library
        item {
            Text(
                text = "Saved Local Media Library (${savedMedia.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (savedMedia.isEmpty()) {
            item {
                SentinelEmptyState(
                    title = "No Saved Studio Assets",
                    description = "Generate original artwork above and save it locally as a standard PNG file to export or share safely."
                )
            }
        } else {
            items(savedMedia, key = { it.id }) { item ->
                val fmt = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                SentinelGlassCard(cornerRadius = 16.dp, contentPadding = 12.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            AsyncImage(
                                model = File(item.filePath),
                                contentDescription = item.prompt,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.prompt,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "PNG · ${fmt.format(Date(item.createdAtMillis))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { shareSavedFile(item.filePath) }) {
                            Icon(Icons.Filled.Share, contentDescription = strings.exportShare, tint = SentinelPalette.CyanAccent)
                        }
                        IconButton(onClick = { onDeleteItem(item) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = SentinelPalette.ErrorRed)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
