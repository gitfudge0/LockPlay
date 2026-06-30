package com.musiclock.ui.lockscreen.skin

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Gold = Color(0xFFFFD86F)
private val PinkGlow = Color(0xFFFF5D8F)
private val PinkText = Color(0xFFFFB3C8)

/**
 * Jukebox (portrait) — a retro neon arch standing on a deep crimson field: a top-rounded gold-bordered
 * cabinet holding the title, a black marquee strip, a row of pulsing selector lights, a gold progress
 * line, and gold transport controls.
 */
@Composable
fun JukeboxSkin(scope: SkinScope) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF6A0F2A), Color(0xFF1A0008)),
                    radius = 1400f,
                ),
            ),
    ) {
        TinyTime(
            time = "9:41",
            color = Gold.copy(alpha = 0.6f),
            modifier = Modifier.statusBarsPadding().align(Alignment.TopEnd).padding(20.dp),
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            topStart = 90.dp, topEnd = 90.dp,
                            bottomStart = 14.dp, bottomEnd = 14.dp,
                        ),
                    )
                    .background(Brush.verticalGradient(listOf(Color(0xFF2A0A14), Color(0xFF400A1A))))
                    .border(
                        width = 3.dp,
                        color = Gold,
                        shape = RoundedCornerShape(
                            topStart = 90.dp, topEnd = 90.dp,
                            bottomStart = 14.dp, bottomEnd = 14.dp,
                        ),
                    )
                    .padding(horizontal = 28.dp, vertical = 44.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    scope.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    style = TextStyle(shadow = Shadow(color = PinkGlow, offset = Offset(0f, 0f), blurRadius = 22f)),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    scope.artist, color = PinkText, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(22.dp))

                // Marquee strip
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        scope.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "B-12  ·  ${scope.artist}", color = PinkGlow, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Pulsing selector lights
                val lightColors = listOf(PinkGlow, Gold, Color(0xFF5DFFB0), Color(0xFF5DB6FF))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    lightColors.forEachIndexed { i, c -> JukeLight(c, scope.isPlaying, i) }
                }

                Spacer(Modifier.height(20.dp))

                // Progress line
                ProgressLine(scope)
            }

            Spacer(Modifier.height(28.dp))

            // Transport controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(34.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                JukeIcon(Icons.Rounded.SkipPrevious, "Previous", 30.dp, scope.onPrev)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Gold)
                        .clickable(onClick = scope.onPlayPause),
                ) {
                    Icon(
                        if (scope.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (scope.isPlaying) "Pause" else "Play",
                        tint = Color(0xFF400A1A), modifier = Modifier.size(36.dp),
                    )
                }
                JukeIcon(Icons.Rounded.SkipNext, "Next", 30.dp, scope.onNext)
            }
        }
    }
}

@Composable
private fun JukeLight(color: Color, playing: Boolean, index: Int) {
    val alpha = if (playing) {
        val transition = rememberInfiniteTransition(label = "light$index")
        val a by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 700, delayMillis = index * 150),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "alpha$index",
        )
        a
    } else {
        0.5f
    }
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha)),
    )
}

@Composable
private fun ProgressLine(scope: SkinScope) {
    val fraction by remember(scope) { derivedStateOf { scope.progressFraction() } }
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Gold.copy(alpha = 0.18f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(3.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Gold),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(scope.position()), color = Gold.copy(alpha = 0.8f), fontSize = 10.sp)
            Text(formatTime(scope.durationMs), color = Gold.copy(alpha = 0.8f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun JukeIcon(icon: ImageVector, desc: String, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Icon(
        icon, contentDescription = desc, tint = Gold,
        modifier = Modifier.size(size).clip(CircleShape).clickable(onClick = onClick),
    )
}
