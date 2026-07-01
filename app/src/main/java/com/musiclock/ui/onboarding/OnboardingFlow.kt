package com.musiclock.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musiclock.design.AppTheme
import com.musiclock.design.components.AppButton
import com.musiclock.design.components.AppCard
import com.musiclock.design.components.AppChip
import com.musiclock.design.components.AppText
import com.musiclock.design.components.ButtonSize
import com.musiclock.design.components.ButtonVariant
import com.musiclock.design.components.ChipTone
import com.musiclock.design.components.ProgressSegments
import com.musiclock.model.NowPlaying
import com.musiclock.ui.lockscreen.skin.DefaultSkin
import com.musiclock.ui.lockscreen.skin.PlayerSkin
import com.musiclock.ui.lockscreen.skin.SkinController
import com.musiclock.ui.lockscreen.skin.SkinScope
import com.musiclock.ui.permissions.AppPermission
import com.musiclock.ui.permissions.PermissionAction
import kotlinx.coroutines.launch

/**
 * The onboarding wizard: a single `rememberSaveable`-backed step machine that walks the user from a
 * welcome peek through every [AppPermission] (priming → settings hand-off → confirmed) to a summary
 * and a ready/blocked end screen. Permission grants reuse [AppPermission.isGranted] re-polled on
 * `ON_RESUME` plus the same launchers as the old `PermissionGate`. Step ordering and the core-missing
 * rule live in [OnboardingLogic].
 *
 * @param onOpenMain navigates to the player gallery (supplied by the orchestrator).
 * @param onFinished invoked when onboarding is complete (the ready End screen, or opening main).
 */
@Composable
fun OnboardingFlow(
    skinController: SkinController,
    onOpenMain: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember { OnboardingController(context.applicationContext) }
    val skin by skinController.skin.collectAsStateWithLifecycle(initialValue = DefaultSkin)

    var step by rememberSaveable { mutableIntStateOf(STEP_WELCOME) }
    // Where a permission step should return to after grant/skip (-1 = natural next step).
    var returnTo by rememberSaveable { mutableIntStateOf(-1) }
    // Per-perm "skipped" UI state (session-local; not a real grant).
    val skipped = remember { mutableStateMapOf<AppPermission, Boolean>() }

    // Live granted-state per permission, re-polled on resume (settings screens return no result).
    val granted = remember { mutableStateMapOf<AppPermission, Boolean>() }
    fun refresh() = AppPermission.entries.forEach { granted[it] = it.isGranted(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun advance() {
        if (returnTo >= 0) {
            val r = returnTo; returnTo = -1; step = r
        } else {
            step = (step + 1).coerceAtMost(STEP_END)
        }
    }
    fun back() { returnTo = -1; step = (step - 1).coerceAtLeast(STEP_WELCOME) }
    fun jumpTo(target: Int, ret: Int = -1) { returnTo = ret; step = target }

    fun finish() {
        scope.launch { controller.markComplete() }
        onFinished()
        onOpenMain()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        // Fixed top: a back chevron (no skip) + progress. The step fills the rest so every screen
        // fits without scrolling.
        Header(onBack = if (step > STEP_WELCOME) ::back else null)

        // Only the per-permission steps show the wizard progress bar. The summary is the review
        // screen — its own rows carry truthful per-item status, so a "5/5 filled" bar there would
        // contradict the headline and the un-granted state below it.
        if (step in FIRST_PERM_STEP until STEP_SUMMARY) {
            ProgressSegments(total = OnboardingPerms.size, currentIndex = step - FIRST_PERM_STEP)
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (step) {
                STEP_WELCOME -> WelcomeStep(skin = skin, onStart = ::advance)
                STEP_SUMMARY -> SummaryStep(
                    granted = granted,
                    onRow = { perm -> jumpTo(permStepIndex(perm), ret = STEP_SUMMARY) },
                    onFinish = ::advance,
                )
                STEP_END -> EndStep(
                    granted = granted,
                    onFixCore = { jumpTo(permStepIndex(CorePerm), ret = STEP_END) },
                    onBackToSummary = { jumpTo(STEP_SUMMARY) },
                    onOpenMain = ::finish,
                )
                else -> permAtStep(step)?.let { perm ->
                    PermStep(
                        permission = perm,
                        isGranted = granted[perm] == true,
                        onRefresh = ::refresh,
                        onContinue = ::advance,
                        onSkip = { skipped[perm] = true; advance() },
                    )
                }
            }
        }
    }
}

/**
 * Shared per-step frame: a [body] that flexes to fill the available height (so content fits the
 * screen without scrolling) above a bottom-pinned [bottom] action area. [bodyArrangement] centers
 * the body for short steps or top-aligns it when the body itself carries a weighted element.
 */
@Composable
private fun StepLayout(
    bottom: @Composable ColumnScope.() -> Unit,
    bodyArrangement: Arrangement.Vertical = Arrangement.spacedBy(AppTheme.spacing.md, Alignment.CenterVertically),
    body: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = bodyArrangement,
            horizontalAlignment = Alignment.CenterHorizontally,
            content = body,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            content = bottom,
        )
    }
}

// --- Header -----------------------------------------------------------------------------------

@Composable
private fun Header(onBack: (() -> Unit)?) {
    // Fixed-height row so the layout below it doesn't shift between steps that do/don't show back.
    Row(
        modifier = Modifier.fillMaxWidth().height(AppTheme.spacing.xl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(AppTheme.spacing.xl)
                    .clip(CircleShape)
                    .background(AppTheme.colors.fillGhost)
                    .clickable(onClick = onBack),
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = AppTheme.colors.textSecondary,
                    modifier = Modifier.size(AppTheme.spacing.lg),
                )
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

// --- Welcome ----------------------------------------------------------------------------------

@Composable
private fun WelcomeStep(skin: PlayerSkin, onStart: () -> Unit) {
    StepLayout(
        bodyArrangement = Arrangement.spacedBy(AppTheme.spacing.md, Alignment.Top),
        bottom = { AppButton(label = "Get started", onClick = onStart, block = true, size = ButtonSize.Lg) },
    ) {
        // Peek flexes to fill the space above the copy; the taller it is, the wider the (height-fit)
        // player mock renders, so let it take the full flex.
        LockscreenPeek(skin = skin, modifier = Modifier.weight(1f))
        AppText("MUSICLOCK", AppTheme.typography.label, color = AppTheme.colors.accentOnSurface)
        AppText("Your music, on your lockscreen.", AppTheme.typography.display, color = AppTheme.colors.textPrimary, textAlign = TextAlign.Center)
        AppText(
            "Turn the lock screen into a full-bleed player that follows whatever you're listening to.",
            AppTheme.typography.body,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        // Extra breathing room between the copy and the bottom-pinned "Get started" button.
        Spacer(Modifier.height(AppTheme.spacing.lg))
    }
}

// --- Permission step --------------------------------------------------------------------------

@Composable
private fun PermStep(
    permission: AppPermission,
    isGranted: Boolean,
    onRefresh: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    val context = LocalContext.current
    val copy = remember(permission) { permCopy(permission) }
    val isRuntime = permission.action is PermissionAction.RuntimePermission

    val runtimeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onRefresh() }
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { onRefresh() }

    // Whether the user has launched the grant flow (drives the "awaiting" view for settings perms).
    var launched by rememberSaveable(permission) { mutableStateOf(false) }

    fun trigger() {
        launched = true
        when (val action = permission.action) {
            is PermissionAction.RuntimePermission -> runtimeLauncher.launch(action.manifestPermission)
            is PermissionAction.SettingsScreen -> settingsLauncher.launch(action.intent(context))
        }
    }

    when {
        isGranted -> PermConfirmed(copy = copy, onContinue = onContinue)
        launched && !isRuntime -> PermAwaiting(copy = copy, onReopen = ::trigger, onSkip = onSkip)
        else -> PermPrime(copy = copy, onGrant = ::trigger, onSkip = onSkip)
    }
}

@Composable
private fun PermPrime(copy: PermCopy, onGrant: () -> Unit, onSkip: () -> Unit) {
    StepLayout(
        bottom = {
            AppButton(label = copy.cta, onClick = onGrant, block = true, size = ButtonSize.Lg)
            AppButton(label = "Not now", onClick = onSkip, block = true, variant = ButtonVariant.Text)
        },
    ) {
        RoundIcon(copy.icon)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            AppText(copy.phase, AppTheme.typography.label, color = AppTheme.colors.accentOnSurface)
            if (copy.required) AppChip("Required", ChipTone.Accent)
        }
        AppText(copy.headline, AppTheme.typography.display, color = AppTheme.colors.textPrimary, textAlign = TextAlign.Center)
        AppText(copy.body, AppTheme.typography.body, color = AppTheme.colors.textSecondary, textAlign = TextAlign.Center)
        if (copy.handoff != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppTheme.shapes.medium)
                    .background(AppTheme.colors.fillGhost)
                    .padding(AppTheme.spacing.md),
            ) {
                AppText(copy.handoff, AppTheme.typography.timestamp, color = AppTheme.colors.textSecondary)
            }
        }
    }
}

@Composable
private fun PermAwaiting(copy: PermCopy, onReopen: () -> Unit, onSkip: () -> Unit) {
    StepLayout(
        bottom = {
            AppButton(label = "Open Settings again", onClick = onReopen, block = true, size = ButtonSize.Lg)
            AppButton(label = "Not now", onClick = onSkip, block = true, variant = ButtonVariant.Text)
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            PulseDot()
            AppText("Waiting for you in Settings…", AppTheme.typography.artist, color = AppTheme.colors.textPrimary)
        }
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                copy.steps.forEachIndexed { i, s ->
                    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                        AppText("${i + 1}", AppTheme.typography.label, color = AppTheme.colors.accentOnSurface)
                        AppText(s, AppTheme.typography.body, color = AppTheme.colors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun PermConfirmed(copy: PermCopy, onContinue: () -> Unit) {
    StepLayout(
        bottom = { AppButton(label = "Continue", onClick = onContinue, block = true, size = ButtonSize.Lg) },
    ) {
        AnimatedVisibility(visible = true, enter = scaleIn() + fadeIn()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(AppTheme.spacing.xxl)
                    .clip(CircleShape)
                    .background(AppTheme.colors.positive.copy(alpha = 0.16f)),
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = AppTheme.colors.positive,
                    modifier = Modifier.size(AppTheme.spacing.lg),
                )
            }
        }
        AppText(copy.grantedTitle, AppTheme.typography.display, color = AppTheme.colors.textPrimary, textAlign = TextAlign.Center)
        AppText(copy.grantedBody, AppTheme.typography.body, color = AppTheme.colors.textSecondary, textAlign = TextAlign.Center)
    }
}

// --- Summary ----------------------------------------------------------------------------------

@Composable
private fun SummaryStep(
    granted: Map<AppPermission, Boolean>,
    onRow: (AppPermission) -> Unit,
    onFinish: () -> Unit,
) {
    val count = grantedCount(granted)
    val coreMissing = isCoreMissing(granted)
    StepLayout(
        bodyArrangement = Arrangement.spacedBy(AppTheme.spacing.sm, Alignment.Top),
        bottom = {
            AppButton(
                label = if (coreMissing) "Finish anyway" else "Finish",
                onClick = onFinish,
                block = true,
                size = ButtonSize.Lg,
            )
        },
    ) {
        AppText(
            if (coreMissing) "One required step left" else "$count of ${OnboardingPerms.size} set up",
            AppTheme.typography.display,
            color = AppTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        AppText(
            if (coreMissing) "Grant notification access to finish, or continue with limited features."
            else "Review what's on. Tap any item to change it.",
            AppTheme.typography.body,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AppTheme.spacing.xs))
        OnboardingPerms.forEach { perm ->
            SummaryRow(perm = perm, isGranted = granted[perm] == true, onClick = { onRow(perm) })
        }
    }
}

@Composable
private fun SummaryRow(perm: AppPermission, isGranted: Boolean, onClick: () -> Unit) {
    val copy = remember(perm) { permCopy(perm) }
    AppCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(AppTheme.spacing.xl)
                    .clip(CircleShape)
                    .background(AppTheme.colors.accentMuted),
            ) {
                Icon(copy.icon, contentDescription = null, tint = AppTheme.colors.accentOnSurface, modifier = Modifier.size(AppTheme.spacing.lg))
            }
            Column(modifier = Modifier.weight(1f)) {
                AppText(perm.title, AppTheme.typography.artist, color = AppTheme.colors.textPrimary)
                AppText(
                    if (perm.optional) "Optional" else "Required",
                    AppTheme.typography.timestamp,
                    color = AppTheme.colors.textTertiary,
                )
            }
            AppChip(
                text = if (isGranted) "On" else if (perm.optional) "Optional" else "Needed",
                tone = if (isGranted) ChipTone.Positive else if (perm.optional) ChipTone.Outline else ChipTone.Accent,
            )
        }
    }
}

// --- End --------------------------------------------------------------------------------------

@Composable
private fun EndStep(
    granted: Map<AppPermission, Boolean>,
    onFixCore: () -> Unit,
    onBackToSummary: () -> Unit,
    onOpenMain: () -> Unit,
) {
    if (isCoreMissing(granted)) {
        StepLayout(
            bottom = {
                AppButton(label = "Enable notification access", onClick = onFixCore, block = true, size = ButtonSize.Lg)
                AppButton(label = "Back to summary", onClick = onBackToSummary, block = true, variant = ButtonVariant.Text)
            },
        ) {
            RoundIcon(Icons.Filled.Lock)
            AppText("MusicLock can't see your music yet", AppTheme.typography.display, color = AppTheme.colors.textPrimary, textAlign = TextAlign.Center)
            AppText(
                "Notification access is required before the lockscreen player can appear.",
                AppTheme.typography.body,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    val degraded = degradedPerms(granted)
    StepLayout(
        bottom = { AppButton(label = "Open MusicLock", onClick = onOpenMain, block = true, size = ButtonSize.Lg) },
    ) {
        AppChip("You're all set", ChipTone.Positive)
        AppText("Press play and look at your lockscreen", AppTheme.typography.display, color = AppTheme.colors.textPrimary, textAlign = TextAlign.Center)
        AppText(
            "Start any music, then lock your phone — MusicLock takes over the screen.",
            AppTheme.typography.body,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (degraded.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppTheme.shapes.medium)
                    .background(AppTheme.colors.warning.copy(alpha = 0.16f))
                    .padding(AppTheme.spacing.md),
            ) {
                AppText(
                    "Skipped: " + degraded.joinToString { it.title } + ". Some features may be limited.",
                    AppTheme.typography.timestamp,
                    color = AppTheme.colors.warning,
                )
            }
        }
    }
}

// --- Shared bits ------------------------------------------------------------------------------

@Composable
private fun RoundIcon(icon: ImageVector) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(AppTheme.spacing.xxl)
            .clip(CircleShape)
            .background(AppTheme.colors.accentMuted),
    ) {
        Icon(icon, contentDescription = null, tint = AppTheme.colors.accentOnSurface, modifier = Modifier.size(AppTheme.spacing.lg))
    }
}

@Composable
private fun PulseDot() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(
        modifier = Modifier
            .size(AppTheme.spacing.sm)
            .clip(CircleShape)
            .background(AppTheme.colors.accent.copy(alpha = alpha)),
    )
}

/** A warm diagonal-gradient stand-in cover so the preview's art reads as a real album, not a blank. */
private fun sampleAlbumArt(): android.graphics.Bitmap {
    val size = 512
    val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    android.graphics.Canvas(bmp).drawPaint(
        android.graphics.Paint().apply {
            shader = android.graphics.LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                intArrayOf(0xFFE8552E.toInt(), 0xFFF5853F.toInt(), 0xFFF0B429.toInt()),
                null, android.graphics.Shader.TileMode.CLAMP,
            )
        },
    )
    return bmp
}

/** A live preview of [skin] with a sample track, fading into the surface at the bottom. */
@Composable
private fun LockscreenPeek(skin: PlayerSkin, modifier: Modifier = Modifier) {
    val sampleScope = remember {
        val sample = NowPlaying(
            isActive = true,
            title = "Midnight City",
            artist = "M83",
            albumArt = sampleAlbumArt(),
            isPlaying = true,
            positionMs = 78_000L,
            durationMs = 240_000L,
        )
        SkinScope(
            state = sample,
            position = { 78_000L },
            onSeek = {}, onPrev = {}, onPlayPause = {}, onNext = {},
        )
    }
    // The skins are full-screen portrait layouts designed for a real screen, so rendering them small
    // makes their text wrap. Instead render the skin at TRUE device size and scale the whole thing
    // down (graphicsLayer) into a phone-shaped frame — the layout is identical to the real screen,
    // just smaller, so nothing wraps.
    val config = LocalConfiguration.current
    val screenW = config.screenWidthDp.dp
    val screenH = config.screenHeightDp.dp
    // Force the skin's LIGHT palette for the preview: the dark palette (#0E0E12) is indistinguishable
    // from the near-black onboarding surface, so the mock reads as blank. A light card pops as a
    // recognisable phone screen.
    val lightConfig = remember(config) {
        Configuration(config).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_NO
        }
    }
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        // Phone-shaped frame sized to the available HEIGHT, so the WHOLE design — through the
        // transport controls at the bottom — fits with no crop. Width follows the device aspect;
        // centered horizontally.
        val frameH = maxHeight
        val frameW = frameH * (config.screenWidthDp.toFloat() / config.screenHeightDp)
        Box(
            modifier = Modifier
                .size(frameW, frameH)
                // Round only the top — the bottom fades straight into the surface, so a rounded
                // bottom edge would read as a floating card rather than a peek.
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Measure the skin at TRUE device pixels (so it lays out exactly like a real
                    // screen — no wrapping), then scale to the frame HEIGHT so it all fits.
                    .layout { measurable, constraints ->
                        val screenWpx = with(density) { screenW.roundToPx() }
                        val screenHpx = with(density) { screenH.roundToPx() }
                        val scale = constraints.maxHeight.toFloat() / screenHpx
                        val placeable = measurable.measure(Constraints.fixed(screenWpx, screenHpx))
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.placeWithLayer(0, 0) {
                                scaleX = scale
                                scaleY = scale
                                transformOrigin = TransformOrigin(0f, 0f)
                            }
                        }
                    },
            ) {
                CompositionLocalProvider(LocalConfiguration provides lightConfig) {
                    skin.content(sampleScope)
                }
            }
            // Subtle bottom fade so the mock reads as embedded in the surface.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.92f to androidx.compose.ui.graphics.Color.Transparent,
                            1f to AppTheme.colors.surface,
                        ),
                    ),
            )
        }
    }
}

// --- Copy -------------------------------------------------------------------------------------

/** Per-permission onboarding copy + icon, adapted from the design's PERMS array. */
private data class PermCopy(
    val icon: ImageVector,
    val phase: String,
    val required: Boolean,
    val headline: String,
    val body: String,
    val cta: String,
    val handoff: String?,
    val steps: List<String>,
    val grantedTitle: String,
    val grantedBody: String,
)

private fun permCopy(perm: AppPermission): PermCopy = when (perm) {
    AppPermission.NotificationListener -> PermCopy(
        icon = Icons.Filled.MusicNote,
        phase = "Phase 1 · Make it work",
        required = true,
        headline = "Let MusicLock see your music",
        body = "Grant notification access so MusicLock knows what's playing and can control it — the one permission it can't work without.",
        cta = "Open Settings",
        handoff = "Find MusicLock in the list, then turn it on.",
        steps = listOf("Open Notification access settings", "Find MusicLock in the list", "Toggle it on and come back"),
        grantedTitle = "MusicLock can see your music",
        grantedBody = "Nice — it now follows whatever you play.",
    )
    AppPermission.DisplayOverApps -> PermCopy(
        icon = Icons.Filled.Lock,
        phase = "Phase 2 · Put it on the lockscreen",
        required = false,
        headline = "Show the player on your lockscreen",
        body = "Allow MusicLock to draw over other apps so your player appears right on the lock screen.",
        cta = "Open Settings",
        handoff = "Toggle 'Allow display over other apps' on.",
        steps = listOf("Open the overlay permission screen", "Switch 'display over other apps' on", "Return to MusicLock"),
        grantedTitle = "Ready for the lockscreen",
        grantedBody = "Your player can now appear over the lock screen.",
    )
    AppPermission.FullScreenIntent -> PermCopy(
        icon = Icons.Filled.Fullscreen,
        phase = "Phase 2 · Put it on the lockscreen",
        required = false,
        headline = "Launch full-screen when music starts",
        body = "Let MusicLock open the immersive player full-screen the moment the screen turns on.",
        cta = "Open Settings",
        handoff = "Allow full-screen notifications for MusicLock.",
        steps = listOf("Open full-screen notifications settings", "Allow it for MusicLock", "Return to MusicLock"),
        grantedTitle = "Full-screen is on",
        grantedBody = "The player will take over the whole screen.",
    )
    AppPermission.BatteryOptimization -> PermCopy(
        icon = Icons.Filled.BatteryChargingFull,
        phase = "Phase 2 · Put it on the lockscreen",
        required = false,
        headline = "Keep MusicLock awake",
        body = "Optional — let MusicLock ignore battery optimizations so it never misses a track in the background.",
        cta = "Open Settings",
        handoff = "Choose 'Don't optimize' for MusicLock.",
        steps = listOf("Open the battery settings", "Pick 'Don't optimize' for MusicLock", "Return to MusicLock"),
        grantedTitle = "Always ready",
        grantedBody = "MusicLock will stay awake in the background.",
    )
    AppPermission.PostNotifications -> PermCopy(
        icon = Icons.Filled.Notifications,
        phase = "Phase 3 · Quick taps",
        required = false,
        headline = "Quick controls in your notifications",
        body = "Allow notifications so you get tap-anywhere playback controls outside the lockscreen.",
        cta = "Allow notifications",
        handoff = null,
        steps = emptyList(),
        grantedTitle = "Notifications on",
        grantedBody = "Quick controls are ready whenever you need them.",
    )
}
