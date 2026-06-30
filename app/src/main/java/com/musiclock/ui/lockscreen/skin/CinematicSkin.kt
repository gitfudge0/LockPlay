package com.musiclock.ui.lockscreen.skin

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Cinematic (landscape) — full-bleed album art filling the whole screen, layered dark scrims for
 * readability, with a bottom-left content block: big shadowed title, artist, a thin white progress
 * bar, and white transport controls. A faint clock sits top-right.
 */
@Composable
fun CinematicSkin(scope: SkinScope) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Full-bleed album art (already crops via ContentScale internally).
        SkinAlbumArt(
            scope = scope,
            shape = RectangleShape,
            modifier = Modifier.fillMaxSize(),
            fallback = Brush.linearGradient(
                listOf(Color(0xFF1A1A22), Color(0xFF0A0A0E)),
            ),
        )

        // Left-to-right scrim so the bottom-left text stays readable.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color(0xCC000000),
                        0.6f to Color(0x00000000),
                    ),
                ),
        )
        // Vertical scrim darkening the bottom.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.4f to Color(0x00000000),
                        1f to Color(0xCC000000),
                    ),
                ),
        )

        TinyTime(
            time = "9:41",
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.statusBarsPadding().align(Alignment.TopEnd).padding(24.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 36.dp, end = 36.dp, bottom = 32.dp, top = 24.dp),
        ) {
            Text(
                scope.title,
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xAA000000),
                        offset = Offset(0f, 2f),
                        blurRadius = 8f,
                    ),
                ),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                scope.artist,
                color = Color(0xFFDDDDDD),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(16.dp))
            CinematicProgress(scope)

            Spacer(Modifier.height(18.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = scope.onPlayPause),
                ) {
                    Icon(
                        if (scope.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (scope.isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp),
                    )
                }
                CineIcon(Icons.Rounded.SkipPrevious, "Previous", scope.onPrev)
                CineIcon(Icons.Rounded.SkipNext, "Next", scope.onNext)
                CineIcon(Icons.Rounded.FavoriteBorder, "Favorite", {})
            }
        }
    }
}

@Composable
private fun CinematicProgress(scope: SkinScope) {
    val fraction by remember(scope) { derivedStateOf { scope.progressFraction() } }
    val duration = scope.durationMs.coerceAtLeast(1L)
    Box(
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.44f))
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    scope.onSeek((offset.x / size.width * duration).toLong())
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White),
        )
    }
}

@Composable
private fun CineIcon(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Icon(
        icon,
        contentDescription = desc,
        tint = Color.White,
        modifier = Modifier.size(28.dp).clip(CircleShape).clickable(onClick = onClick),
    )
}
