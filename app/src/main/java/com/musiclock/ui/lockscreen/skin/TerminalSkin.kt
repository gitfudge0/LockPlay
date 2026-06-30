package com.musiclock.ui.lockscreen.skin

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TermBg = Color(0xFF05140A)
private val TermGreen = Color(0xFF33FF88)
private val TermDim = Color(0xFF33FF88)
private val TermBright = Color(0xFF7FFFD4)
private val TermNow = Color(0xFFAAFFCC)

/**
 * Terminal / CLI (portrait) — a faux shell session on a near-black green field: a prompt invocation,
 * the now-playing line, a live equalizer, a status line with a thin progress readout, and a blinking
 * block cursor. Everything monospace, left-aligned, vertically centered.
 */
@Composable
fun TerminalSkin(scope: SkinScope) {
    Box(modifier = Modifier.fillMaxSize().background(TermBg)) {
        TinyTime(
            time = "9:41",
            color = TermGreen.copy(alpha = 0.6f),
            modifier = Modifier.statusBarsPadding().align(Alignment.TopEnd).padding(20.dp),
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                buildAnnotatedString {
                    withStyle(androidx.compose.ui.text.SpanStyle(color = TermDim.copy(alpha = 0.55f))) {
                        append("$ ")
                    }
                    withStyle(androidx.compose.ui.text.SpanStyle(color = TermBright)) { append("lock") }
                    withStyle(androidx.compose.ui.text.SpanStyle(color = TermDim.copy(alpha = 0.55f))) {
                        append(" --media --keep-alive")
                    }
                },
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            )

            Spacer(Modifier.height(14.dp))

            Text(
                "♪ now_playing: \"" + scope.title + " — " + scope.artist + "\"",
                color = TermNow,
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(22.dp))

            EqualizerBars(
                bars = 14,
                color = TermGreen,
                playing = scope.isPlaying,
                modifier = Modifier.fillMaxWidth().height(60.dp),
            )

            Spacer(Modifier.height(22.dp))

            Text(
                formatTime(scope.position()) + " / " + formatTime(scope.durationMs) + " · vol 0.78 · 🔒",
                color = TermGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(8.dp))

            ProgressLine(scope)

            Spacer(Modifier.height(22.dp))

            PromptLine(playing = scope.isPlaying)

            Spacer(Modifier.height(26.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TransportIcon(Icons.Rounded.SkipPrevious, "Previous", scope.onPrev)
                TransportIcon(
                    if (scope.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (scope.isPlaying) "Pause" else "Play",
                    scope.onPlayPause,
                )
                TransportIcon(Icons.Rounded.SkipNext, "Next", scope.onNext)
            }
        }
    }
}

@Composable
private fun ProgressLine(scope: SkinScope) {
    val fraction by remember(scope) { derivedStateOf { scope.progressFraction() } }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(TermGreen.copy(alpha = 0.18f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(2.dp)
                .background(TermGreen),
        )
    }
}

@Composable
private fun PromptLine(playing: Boolean) {
    val transition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by transition.animateFloat(
        initialValue = if (playing) 1f else 1f,
        targetValue = if (playing) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blink",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$ ",
            color = TermGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp,
        )
        Box(
            modifier = Modifier
                .size(width = 8.dp, height = 16.dp)
                .alpha(if (playing) cursorAlpha else 1f)
                .background(TermGreen),
        )
    }
}

@Composable
private fun TransportIcon(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Icon(
        icon,
        contentDescription = desc,
        tint = TermGreen,
        modifier = Modifier.size(30.dp).clickable(onClick = onClick),
    )
}
