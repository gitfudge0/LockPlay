package com.lockplay.ui.lockscreen

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lockplay.design.AppTheme
import com.lockplay.lyrics.LyricLine
import com.lockplay.lyrics.Lyrics
import com.lockplay.lyrics.LyricsStatus
import com.lockplay.lyrics.activeLineIndex
import com.lockplay.lyrics.lyricsPositionMs
import com.lockplay.model.NowPlaying
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs

/**
 * Cadence of the overlay's own position ticker. Finer than the 1s ticker hoisted in
 * [LockscreenScreen] because a lyric line has to land on the beat, not within a second of it.
 */
private const val PositionTickMs = 32L

/** Where the active line sits: a third down the lyric area, not dead centre — the next lines matter. */
private const val AnchorBandFraction = 0.33f

/** Bottom slice of the overlay left to the skin's transport: scrimmed over, never pointer-claimed. */
private const val TransportSafeFraction = 0.22f

/** Idle time after a manual scroll before auto-scroll takes back over. */
private const val AutoScrollResumeMs = 4_000L

private val MinTouchTarget = 48.dp

/**
 * The lyrics surface drawn over the active skin.
 *
 * Reads [Lyrics] produced by [com.lockplay.lyrics.LyricsRepository] and the live position from
 * [NowPlaying.positionAt], and calls [onSeek] when a line is tapped. It runs its OWN ~32ms ticker,
 * scoped to this composable so it dies the instant the overlay leaves composition — a free-running
 * loop on a lockscreen is a battery bug. Every tick RECOMPUTES the position from the session sample
 * rather than accumulating, so it cannot drift.
 *
 * Reads tokens through [AppTheme] only (S7); the two scrim colors are the documented exception.
 */
@Composable
fun LyricsOverlay(
    state: NowPlaying,
    lyrics: Lyrics,
    onSeek: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Held as a raw long, never read during composition of the overlay itself: only the
    // derivedStateOf below reads it, so a 32ms tick recomposes nothing unless the line changes.
    val positionState = remember { mutableLongStateOf(state.positionAt(SystemClock.elapsedRealtime())) }

    LaunchedEffect(state.isPlaying, state.positionMs, state.positionUpdateTime) {
        positionState.longValue = state.positionAt(SystemClock.elapsedRealtime())
        // Paused: seed once and stop. No loop at all while nothing is moving.
        if (state.isPlaying) {
            while (isActive) {
                delay(PositionTickMs)
                positionState.longValue = state.positionAt(SystemClock.elapsedRealtime())
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(lyricsScrimBrush()),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LyricsTopBar(onClose = onClose)
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (lyrics.status) {
                    LyricsStatus.Synced -> SyncedLyrics(
                        lines = lyrics.lines,
                        durationMs = state.durationMs,
                        positionMs = { positionState.longValue },
                        onSeek = onSeek,
                    )

                    LyricsStatus.Plain -> PlainLyrics(lyrics.plainText)
                    LyricsStatus.Loading -> LoadingLyrics()
                    LyricsStatus.NotFound -> NotFoundLyrics()
                    LyricsStatus.Offline -> OfflineLyrics()
                    // Idle: the scrim alone, so the flip reads as a surface arriving before content does.
                    LyricsStatus.Idle -> Unit
                }
            }
        }
        LyricsAttribution(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(AppTheme.spacing.md),
        )
    }
}

@Composable
private fun LyricsTopBar(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = AppTheme.spacing.screenPadding,
                end = AppTheme.spacing.screenPadding,
                top = AppTheme.spacing.lg,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Lyrics",
            style = AppTheme.typography.label,
            color = AppTheme.colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(MinTouchTarget)
                .pointerInput(Unit) { detectTapGestures(onTap = { onClose() }) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close lyrics",
                tint = AppTheme.colors.textPrimary,
            )
        }
    }
}

/**
 * The timed-lyric view: a scrolling column that keeps the active line in an anchor band, yields to
 * a manual scroll, and seeks on a tap.
 *
 * [positionMs] is a lambda so the ticker's writes stay out of this function's own recomposition
 * scope — only [activeIndex] can invalidate it.
 */
@Composable
private fun SyncedLyrics(
    lines: List<LyricLine>,
    durationMs: Long,
    positionMs: () -> Long,
    onSeek: (Long) -> Unit,
) {
    val listState = rememberLazyListState()
    var autoScrollEnabled by remember { mutableStateOf(true) }
    // Bumped on every new user drag so the resume timer restarts rather than firing mid-scroll.
    var manualScrollMarker by remember { mutableIntStateOf(0) }

    val activeIndex by remember(lines, durationMs) {
        derivedStateOf { activeLineIndex(lines, lyricsPositionMs(positionMs(), durationMs)) }
    }

    // Only a real drag disengages auto-scroll; animateScrollToItem also sets isScrollInProgress,
    // and keying off that would make the auto-scroll switch itself off on its first move.
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) {
                autoScrollEnabled = false
                manualScrollMarker++
            }
        }
    }

    LaunchedEffect(manualScrollMarker, autoScrollEnabled) {
        if (!autoScrollEnabled) {
            delay(AutoScrollResumeMs)
            autoScrollEnabled = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val lyricAreaHeight: Dp = maxHeight * (1f - TransportSafeFraction)
        val anchorOffsetPx = with(LocalDensity.current) { (lyricAreaHeight * AnchorBandFraction).roundToPx() }

        // "unlock still works": with nothing to scroll, the LazyColumn must not swallow vertical
        // drag, or a short lyric would quietly disable swipe-up-to-unlock.
        val hasScrollRange = listState.canScrollForward || listState.canScrollBackward

        LaunchedEffect(activeIndex, autoScrollEnabled, anchorOffsetPx) {
            if (autoScrollEnabled && activeIndex >= 0) {
                listState.animateScrollToItem(activeIndex, scrollOffset = -anchorOffsetPx)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(lyricAreaHeight)) {
                LazyColumn(
                    state = listState,
                    userScrollEnabled = hasScrollRange,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = AppTheme.spacing.screenPadding,
                        end = AppTheme.spacing.screenPadding,
                        top = lyricAreaHeight * AnchorBandFraction,
                        bottom = lyricAreaHeight * AnchorBandFraction,
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                ) {
                    itemsIndexed(lines, key = { index, _ -> index }) { index, line ->
                        LyricRow(
                            line = line,
                            distance = if (activeIndex < 0) Int.MAX_VALUE else abs(index - activeIndex),
                            onSeek = onSeek,
                        )
                    }
                }

                if (!autoScrollEnabled) {
                    NowPlayingPill(
                        onClick = { autoScrollEnabled = true },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = AppTheme.spacing.md),
                    )
                }
            }
        }
    }
}

/**
 * One lyric line. Uses [detectTapGestures] rather than `clickable` on purpose: a drag that happens
 * to end on a line must scroll, never seek.
 */
@Composable
private fun LyricRow(
    line: LyricLine,
    distance: Int,
    onSeek: (Long) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppTheme.spacing.xxl)
            .pointerInput(line.atMs) {
                detectTapGestures(onTap = { onSeek(line.atMs) })
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = line.text,
            style = AppTheme.typography.display,
            color = AppTheme.colors.textPrimary,
            // graphicsLayer, not a new TextStyle: alpha here is a draw-time property, so a changed
            // active line does not re-layout or re-measure the text.
            modifier = Modifier.graphicsLayer { alpha = lineAlpha(distance) },
        )
    }
}

/** Opacity ramp by distance from the active line. Distance is `abs(index - activeIndex)`. */
private fun lineAlpha(distance: Int): Float = when (distance) {
    0 -> 1.00f
    1 -> 0.55f
    2 -> 0.35f
    else -> 0.22f
}

/**
 * The backdrop the lyrics sit on: opaque where the words are, thinning towards the bottom so the
 * skin's transport stays readable underneath.
 */
@Composable
private fun lyricsScrimBrush(): Brush = Brush.verticalGradient(
    // ponytail: hardcoded near-black scrim instead of a token — the lyrics sheet is deliberately
    // one fixed surface across all five skins; promote to a token if a theme ever needs a light one.
    0f to Color(0xFF060607).copy(alpha = 0.93f),
    // ponytail: hardcoded scrim stop — same upgrade trigger as the stop above.
    0.62f to Color(0xFF060607).copy(alpha = 0.92f),
    // ponytail: hardcoded scrim stop — same upgrade trigger as the stop above.
    0.80f to Color(0xFF060607).copy(alpha = 0.86f),
    // ponytail: hardcoded scrim stop, faded so the transport shows through — same upgrade trigger.
    1f to Color(0xFF060607).copy(alpha = 0.62f),
)

/** Re-engage control shown only while a manual scroll holds auto-scroll off. */
@Composable
private fun NowPlayingPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(AppTheme.colors.fillGhost, AppTheme.shapes.pill)
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
    ) {
        Text(
            text = "Now Playing  ⌄",
            style = AppTheme.typography.label,
            color = AppTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
    }
}
