package com.musiclock.ui.lockscreen.skin

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Teal = Color(0xFF00FFC8)
private val Pink = Color(0xFFFF3D81)
private val AuroraText2 = Color(0xFF8FA3B5)

/**
 * Aurora (portrait) — calm and ambient: two blurred gradient blobs drift behind a circular album
 * art, with a teal→pink progress and a soft rounded play button.
 */
@Composable
fun AuroraSkin(scope: SkinScope) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val drift by transition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(6000), RepeatMode.Reverse),
        label = "drift",
    )
    val playing = scope.isPlaying

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF08111A))) {
        // Drifting blurred blobs (only animate while playing).
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-60).dp, y = (if (playing) drift else 0f).dp)
                .size(280.dp)
                .blur(70.dp)
                .clip(CircleShape)
                .background(Teal.copy(alpha = 0.55f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = (if (playing) -drift else 0f).dp)
                .size(280.dp)
                .blur(70.dp)
                .clip(CircleShape)
                .background(Pink.copy(alpha = 0.55f)),
        )

        TinyTime(
            time = "9:41",
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.TopEnd).padding(20.dp),
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SkinAlbumArt(
                scope = scope,
                shape = CircleShape,
                fallback = Brush.linearGradient(listOf(Teal, Pink)),
                modifier = Modifier.size(170.dp),
            )
            Spacer(Modifier.height(28.dp))
            Text(scope.title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(scope.artist, color = AuroraText2, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)

            Spacer(Modifier.height(24.dp))
            AuroraProgress(scope)

            Spacer(Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AuroraIcon(Icons.Rounded.SkipPrevious, "Previous", scope.onPrev)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(66.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .clickable(onClick = scope.onPlayPause),
                ) {
                    Icon(
                        if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play",
                        tint = Color(0xFF08111A), modifier = Modifier.size(30.dp),
                    )
                }
                AuroraIcon(Icons.Rounded.SkipNext, "Next", scope.onNext)
            }
        }
    }
}

@Composable
private fun AuroraProgress(scope: SkinScope) {
    val fraction by androidx.compose.runtime.remember(scope) {
        androidx.compose.runtime.derivedStateOf { scope.progressFraction() }
    }
    val duration = scope.durationMs.coerceAtLeast(1L)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .pointerInput(duration) {
                detectTapGestures { offset -> scope.onSeek((offset.x / size.width * duration).toLong()) }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.horizontalGradient(listOf(Teal, Pink))),
        )
    }
}

@Composable
private fun AuroraIcon(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Icon(
        icon, contentDescription = desc, tint = Color.White,
        modifier = Modifier.size(30.dp).clip(CircleShape).clickable(onClick = onClick),
    )
}
