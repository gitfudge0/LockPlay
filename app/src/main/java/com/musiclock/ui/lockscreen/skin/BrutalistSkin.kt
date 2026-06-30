package com.musiclock.ui.lockscreen.skin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Lime = Color(0xFFE6FF00)
private val Ink = Color(0xFF000000)
private val Paper = Color(0xFFFFFFFF)

/**
 * Brutalist (portrait) — a solid lime field, raw monospace, hard black borders. No gradients, no
 * rounding; the track title shouts in huge uppercase, transport is three bordered rectangles.
 */
@Composable
fun BrutalistSkin(scope: SkinScope) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Lime),
    ) {
        TinyTime(
            time = formatTime(scope.position()),
            color = Ink,
            modifier = Modifier.statusBarsPadding().align(Alignment.TopEnd).padding(20.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
        ) {
            // Tag chip
            Text(
                text = "NOW PLAYING",
                color = Lime,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier
                    .background(Ink)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )

            Spacer(Modifier.height(22.dp))

            // Huge title
            Text(
                text = scope.title.uppercase(),
                color = Ink,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 40.sp,
                lineHeight = 38.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(14.dp))

            // Artist
            Text(
                text = "// " + scope.artist,
                color = Ink,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(28.dp))

            // Progress bar — thick black border, black fill = fraction
            val fraction by androidx.compose.runtime.remember(scope) {
                androidx.compose.runtime.derivedStateOf { scope.progressFraction() }
            }
            val duration = scope.durationMs.coerceAtLeast(1L)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .border(4.dp, Ink)
                    .pointerInput(duration) {
                        detectTapGestures { offset ->
                            scope.onSeek((offset.x / size.width * duration).toLong())
                        }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(Ink),
                )
            }

            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(scope.position()), color = Ink, fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(formatTime(scope.durationMs), color = Ink, fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Spacer(Modifier.height(28.dp))

            // Transport — three bordered rectangles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrutalButton(
                    glyph = "◀◀",
                    bg = Paper,
                    fg = Ink,
                    onClick = scope.onPrev,
                    modifier = Modifier.weight(1f),
                )
                BrutalButton(
                    glyph = if (scope.isPlaying) "❙❙" else "▶",
                    bg = Ink,
                    fg = Lime,
                    onClick = scope.onPlayPause,
                    modifier = Modifier.weight(1f),
                )
                BrutalButton(
                    glyph = "▶▶",
                    bg = Paper,
                    fg = Ink,
                    onClick = scope.onNext,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BrutalButton(glyph: String, bg: Color, fg: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(58.dp)
            .border(4.dp, Ink)
            .background(bg)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = glyph,
            color = fg,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
        )
    }
}
