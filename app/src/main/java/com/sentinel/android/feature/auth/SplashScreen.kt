package com.sentinel.android.feature.auth

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.sentinel.android.core.common.LocalSentinelStrings
import com.sentinel.android.core.designsystem.SentinelPalette

@Composable
fun SplashScreen(statusLabel: String) {
    val strings = LocalSentinelStrings.current
    val transition = rememberInfiniteTransition(label = "splashPulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )
    val glowAlpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        SentinelPalette.ElectricBlue.copy(alpha = 0.22f),
                        SentinelPalette.MidnightBg
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(164.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    SentinelPalette.CyanAccent.copy(alpha = glowAlpha),
                                    SentinelPalette.MidnightBg.copy(alpha = 0f)
                                )
                            )
                        )
                )
                Image(
                    painter = painterResource(id = R.drawable.sentinel_app_icon_1795200152921),
                    contentDescription = "Android Sentinel Logo",
                    modifier = Modifier
                        .size(112.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "ANDROID SENTINEL",
                style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = 2.5.sp),
                color = SentinelPalette.TextPrimaryDark,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.tagline,
                style = MaterialTheme.typography.bodyMedium,
                color = SentinelPalette.TextSecondaryDark,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(36.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .width(200.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50)),
                color = SentinelPalette.CyanAccent,
                trackColor = SentinelPalette.ElevatedSurfaceHigh
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelMedium,
                color = SentinelPalette.CyanAccent
            )
        }
    }
}
