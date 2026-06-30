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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF111111)
private val Muted = Color(0xFF999999)
private val Faint = Color(0xFFBBBBBB)
private val Hairline = Color(0xFFDDDDDD)

/**
 * Minimal (portrait) — quiet, light layout: left-aligned, vertically centered text on a near-white
 * field, with a hairline progress track, timestamps, and a restrained black/white transport row.
 */
@Composable
fun MinimalSkin(scope: SkinScope) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA)),
    ) {
        TinyTime(
            time = "9:41",
            color = Faint,
            modifier = Modifier.statusBarsPadding().align(Alignment.TopEnd).padding(20.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "NOW PLAYING",
                color = Faint,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 3.sp,
            )

            Spacer(Modifier.height(18.dp))
            Text(
                scope.title,
                color = Ink,
                fontSize = 30.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-0.5).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(8.dp))
            Text(
                scope.artist,
                color = Muted,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(36.dp))
            MinimalProgress(scope)

            Spacer(Modifier.height(36.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Ink,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(onClick = scope.onPrev),
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Ink)
                        .clickable(onClick = scope.onPlayPause),
                ) {
                    Icon(
                        if (scope.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (scope.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = Ink,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(onClick = scope.onNext),
                )
            }
        }
    }
}

@Composable
private fun MinimalProgress(scope: SkinScope) {
    val fraction by remember(scope) { derivedStateOf { scope.progressFraction() } }
    val duration = scope.durationMs.coerceAtLeast(1L)
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .pointerInput(duration) {
                    detectTapGestures { offset ->
                        scope.onSeek((offset.x / size.width * duration).toLong())
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Hairline),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Ink),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatTime(scope.position()), color = Faint, fontSize = 11.sp)
            Text(formatTime(scope.durationMs), color = Faint, fontSize = 11.sp)
        }
    }
}
