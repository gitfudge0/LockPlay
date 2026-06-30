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
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NeuBg = Color(0xFFE0E5EC)
private val NeuTrack = Color(0xFFD1D9E6)
private val NeuLight = Color(0xFFFFFFFF)
private val NeuDark = Color(0xFFB8BEC9)
private val NeuTitle = Color(0xFF3D4456)
private val NeuArtist = Color(0xFF5B6478)
private val NeuMuted = Color(0xFF9AA3B5)
private val NeuAccentA = Color(0xFF7C8AFF)
private val NeuAccentB = Color(0xFFA98BFF)

/**
 * Neumorphic (portrait) — soft UI on a #e0e5ec field: a raised album tile, muted title/artist,
 * an inset gradient progress pill, and a raised circular play/pause flanked by prev/next.
 */
@Composable
fun NeumorphicSkin(scope: SkinScope) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeuBg),
    ) {
        TinyTime(
            time = "9:41",
            color = NeuMuted,
            modifier = Modifier.statusBarsPadding().align(Alignment.TopEnd).padding(20.dp),
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Raised album tile
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(30.dp),
                        ambientColor = NeuDark,
                        spotColor = NeuDark,
                    )
                    .clip(RoundedCornerShape(30.dp))
                    .background(NeuBg),
            ) {
                if (scope.state.albumArt != null) {
                    SkinAlbumArt(
                        scope = scope,
                        shape = RoundedCornerShape(30.dp),
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = NeuMuted,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }

            Spacer(Modifier.height(30.dp))
            Text(
                scope.title, color = NeuTitle, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                scope.artist, color = NeuArtist, fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))
            NeuProgress(scope)

            Spacer(Modifier.height(28.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(34.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                NeuIcon(Icons.Rounded.SkipPrevious, "Previous", 30.dp, NeuArtist, scope.onPrev)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(62.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = CircleShape,
                            ambientColor = NeuDark,
                            spotColor = NeuDark,
                        )
                        .clip(CircleShape)
                        .background(NeuBg)
                        .clickable(onClick = scope.onPlayPause),
                ) {
                    Icon(
                        if (scope.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (scope.isPlaying) "Pause" else "Play",
                        tint = NeuAccentA, modifier = Modifier.size(30.dp),
                    )
                }
                NeuIcon(Icons.Rounded.SkipNext, "Next", 30.dp, NeuArtist, scope.onNext)
            }
        }
    }
}

@Composable
private fun NeuProgress(scope: SkinScope) {
    val fraction by remember(scope) { derivedStateOf { scope.progressFraction() } }
    val duration = scope.durationMs.coerceAtLeast(1L)
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(NeuTrack)
                .pointerInput(duration) {
                    detectTapGestures { offset ->
                        scope.onSeek((offset.x / size.width * duration).toLong())
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(NeuAccentA, NeuAccentB))),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(scope.position()), color = NeuMuted, fontSize = 11.sp)
            Text(formatTime(scope.durationMs), color = NeuMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun NeuIcon(icon: ImageVector, desc: String, size: Dp, tint: Color, onClick: () -> Unit) {
    Icon(
        icon, contentDescription = desc, tint = tint,
        modifier = Modifier.size(size).clip(CircleShape).clickable(onClick = onClick),
    )
}
