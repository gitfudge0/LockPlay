package com.lockplay.ui.lockscreen.skin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Editorial (portrait) — a typographic "poster" lockscreen inspired by Swiss/editorial album art:
 * the artist set huge in bold uppercase up top, mono-cased track/album meta, a thin red scrubber,
 * a minimal transport row, and a large vinyl disc — with the album art as its centre label — that
 * sits centred at the bottom edge and spins while playing. Colours are resolved locally from a
 * two-palette light/dark set; AppTheme / MaterialTheme tokens are intentionally ignored.
 *
 * Light: paper #F4F2EC, ink #161514, muted #6E6B64.
 * Dark:  paper #141210, ink #F4F2EC, muted #9A968D.
 * Accent (both): #E5402A.
 */
@Composable
fun EditorialSkin(scope: SkinScope) {
    val dark = isSystemInDarkTheme()
    val paper = if (dark) Color(0xFF141210) else Color(0xFFF4F2EC)
    val ink = if (dark) Color(0xFFF4F2EC) else Color(0xFF161514)
    val muted = if (dark) Color(0xFF9A968D) else Color(0xFF6E6B64)
    val accent = Color(0xFFE5402A)

    // Scale every fixed size to the available width so the skin reads the same full-screen and in the
    // small gallery preview (the preview paints the skin at the card's real, reduced pixel size).
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(paper)) {
        val s = (maxWidth / 360.dp).coerceIn(0.3f, 1.3f)

        // Disc first so it sits BEHIND the type; centred at the bottom, half off-screen.
        VinylDisc(
            scope = scope,
            accent = accent,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 150.dp * s)
                .size(320.dp * s),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 20.dp * s, end = 20.dp * s, top = 20.dp * s),
        ) {
            // Clock, top-left.
            Text(
                text = rememberClockText(),
                color = ink,
                fontSize = 14.sp * s,
                fontWeight = FontWeight.SemiBold,
            )

            // Drop the meta/controls cluster to ~30% down the screen.
            Box(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.30f),
                contentAlignment = Alignment.BottomCenter,
            ) {
                scope.lyricsPill?.let { pill ->
                    Box(modifier = Modifier.padding(bottom = 20.dp * s)) { pill() }
                }
            }

            // Song title — the poster headline.
            Text(
                text = scope.title.uppercase(),
                color = ink,
                fontSize = 52.sp * s,
                lineHeight = 46.sp * s,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp * s,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(16.dp * s))

            Text(
                text = "NOW PLAYING",
                color = accent,
                fontSize = 11.sp * s,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp * s,
            )
            Spacer(Modifier.height(6.dp * s))
            // Artist, then album — the meta detail.
            Text(
                text = scope.artist.uppercase(),
                color = ink,
                fontSize = 13.sp * s,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp * s,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (scope.album.isNotEmpty()) {
                Spacer(Modifier.height(2.dp * s))
                Text(
                    text = scope.album.uppercase(),
                    color = muted,
                    fontSize = 11.sp * s,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp * s,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(24.dp * s))

            // Scrubber + times, then transport — kept close to the meta above.
            EditorialScrubber(scope = scope, accent = accent, track = muted.copy(alpha = 0.4f), scale = s)
            Spacer(Modifier.height(8.dp * s))
            EditorialTimesRow(scope = scope, muted = muted, scale = s)

            Spacer(Modifier.height(20.dp * s))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp * s, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = ink,
                    modifier = Modifier
                        .size(28.dp * s)
                        .clip(CircleShape)
                        .clickable(onClick = scope.onPrev),
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp * s)
                        .clip(CircleShape)
                        .background(ink)
                        .clickable(onClick = scope.onPlayPause)
                        .semantics {
                            role = Role.Button
                            contentDescription = if (scope.isPlaying) "Pause" else "Play"
                        },
                ) {
                    Icon(
                        if (scope.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = paper,
                        modifier = Modifier.size(26.dp * s),
                    )
                }
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = ink,
                    modifier = Modifier
                        .size(28.dp * s)
                        .clip(CircleShape)
                        .clickable(onClick = scope.onNext),
                )
            }

            // Push everything up; the disc fills the space below, peeking from the bottom.
            Spacer(Modifier.weight(1f))
        }
    }
}

/** Black vinyl disc with radial grooves and the album art as a red-ringed centre label; spins while playing. */
@Composable
private fun VinylDisc(scope: SkinScope, accent: Color, modifier: Modifier = Modifier) {
    // Lambda-read spin: sampled in the graphicsLayer draw block so only the layer invalidates per frame.
    val spin = rememberSpinAngle(scope.isPlaying, 8000)

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = spin() }) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val outer = size.minDimension / 2f

            drawCircle(Color(0xFF080808), outer, c)
            var r = outer * 0.97f
            while (r > outer * 0.34f) {
                drawCircle(Color(0xFF1E1E1E), r, c, style = Stroke(width = 1.4f))
                r -= 4.5f
            }
            // Red ring framing the label.
            drawCircle(accent, outer * 0.34f, c, style = Stroke(width = 3f))
            // Spindle hole.
            drawCircle(Color(0xFF0D0B08), outer * 0.04f, c)
        }

        // Album art as the circular centre label — rotates with the disc.
        SkinAlbumArt(
            scope = scope,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxSize(0.32f)
                .graphicsLayer { rotationZ = spin() },
        )
    }
}

/** Thin 2dp scrubber; tappable to seek when duration is known. */
@Composable
private fun EditorialScrubber(scope: SkinScope, accent: Color, track: Color, scale: Float, modifier: Modifier = Modifier) {
    val fraction by remember(scope) { derivedStateOf { scope.progressFraction() } }
    val duration = scope.durationMs
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp * scale)
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    if (duration > 0) scope.onSeek((offset.x / size.width * duration).toLong())
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().fillMaxHeight().background(track))
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(accent))
    }
}

/** Elapsed left, total right (total omitted when duration is unknown). */
@Composable
private fun EditorialTimesRow(scope: SkinScope, muted: Color, scale: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = formatTime(scope.position()), color = muted, fontSize = 11.sp * scale)
        if (scope.durationMs > 0) {
            Text(text = formatTime(scope.durationMs), color = muted, fontSize = 11.sp * scale)
        }
    }
}
