package com.sentinel.android.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import java.security.MessageDigest

@Composable
fun SentinelGlassCard(
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.outlineVariant,
    cornerRadius: Dp = 20.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val surface = MaterialTheme.colorScheme.surface
    val surfaceHigh = MaterialTheme.colorScheme.surfaceVariant
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    )
                ),
                shape = shape
            ),
        shape = shape,
        color = surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            surfaceHigh.copy(alpha = 0.45f),
                            surface
                        )
                    )
                )
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun SentinelStatusBadge(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false
) {
    val alpha = if (pulsing) {
        val transition = rememberInfiniteTransition(label = "pulse")
        val value by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dotAlpha"
        )
        value
    } else {
        1f
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = alpha))
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SentinelMetricTile(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    SentinelGlassCard(
        modifier = modifier,
        accentColor = accentColor.copy(alpha = 0.45f),
        cornerRadius = 18.dp,
        contentPadding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SentinelSkeletonCard(modifier: Modifier = Modifier, height: Dp = 96.dp) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = 0f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = Offset(shimmerX - 300f, 0f),
                    end = Offset(shimmerX, 200f)
                )
            )
    )
}

@Composable
fun SentinelEmptyState(
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    SentinelGlassCard(
        modifier = modifier,
        accentColor = SentinelPalette.ElectricBlue.copy(alpha = 0.5f),
        contentPadding = 24.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_sentinel_shield_art_1795200154016),
                contentDescription = title,
                modifier = Modifier
                    .size(116.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun SentinelErrorCard(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    SentinelGlassCard(
        modifier = modifier,
        accentColor = SentinelPalette.ErrorRed,
        contentPadding = 16.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = "Error",
                tint = SentinelPalette.ErrorRed,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (onRetry != null) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = SentinelPalette.ErrorRed)
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
fun SentinelOfflineBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SentinelPalette.WarningAmber.copy(alpha = 0.16f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SentinelPalette.WarningAmber.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = "Offline",
                tint = SentinelPalette.WarningAmber,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SentinelTransparencyBanner(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SentinelPalette.CyanAccent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, SentinelPalette.CyanAccent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = "Consent Verified",
                tint = SentinelPalette.CyanAccent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Deterministic high-contrast 21x21 QR-style enrollment matrix rendered with Canvas.
 */
@Composable
fun SentinelQrCodeCard(
    payload: String,
    modifier: Modifier = Modifier
) {
    val hashBytes = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
    val gridSize = 21

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(2.dp, SentinelPalette.CyanAccent.copy(alpha = 0.7f))
    ) {
        Canvas(
            modifier = Modifier
                .size(188.dp)
                .padding(14.dp)
        ) {
            val cellSize = size.width / gridSize
            fun isFinder(r: Int, c: Int, originR: Int, originC: Int): Boolean? {
                val dr = r - originR
                val dc = c - originC
                if (dr !in 0..6 || dc !in 0..6) return null
                return dr == 0 || dr == 6 || dc == 0 || dc == 6 || (dr in 2..4 && dc in 2..4)
            }

            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    val finder = isFinder(r, c, 0, 0)
                        ?: isFinder(r, c, 0, gridSize - 7)
                        ?: isFinder(r, c, gridSize - 7, 0)
                    val filled = when {
                        finder != null -> finder
                        (r == 7 && (c <= 7 || c >= gridSize - 8)) ||
                            (c == 7 && (r <= 7 || r >= gridSize - 8)) -> false
                        r == 6 || c == 6 -> (r + c) % 2 == 0
                        else -> {
                            val idx = (r * gridSize + c) % (hashBytes.size * 8)
                            val byteVal = hashBytes[idx / 8].toInt()
                            ((byteVal ushr (idx % 8)) and 1) == 1
                        }
                    }
                    if (filled) {
                        drawRect(
                            color = Color(0xFF080B12),
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize + 0.5f, cellSize + 0.5f)
                        )
                    }
                }
            }
        }
    }
}
