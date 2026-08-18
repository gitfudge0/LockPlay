package com.lockplay.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lockplay.design.AppTheme
import com.lockplay.design.components.AppDialog
import com.lockplay.design.components.AppText

/**
 * Whether turning the lyrics toggle to [requestedValue] must first show the pre-enable explanation
 * dialog. Compose-free (A13) so the gating rule is testable on the JVM without a UI harness.
 * Turning on always requires confirmation; turning off never does.
 */
fun requiresConfirmation(requestedValue: Boolean): Boolean = requestedValue

/**
 * Labelled settings row for the opt-in lyrics feature: title, description, and a trailing switch.
 * Turning the switch on shows [AppDialog] with the privacy explanation first ([requiresConfirmation]);
 * [onEnabledChange] only fires once the user confirms. Turning it off fires immediately.
 */
@Composable
fun LyricsSettingRow(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            modifier = Modifier.weight(1f),
        ) {
            AppText("Lyrics", AppTheme.typography.body, color = AppTheme.colors.textPrimary)
            AppText(
                "Swipe the album art to see lyrics for the song that's playing.",
                AppTheme.typography.timestamp,
                color = AppTheme.colors.textSecondary,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = { requested ->
                if (requiresConfirmation(requested)) {
                    showConfirmDialog = true
                } else {
                    onEnabledChange(false)
                }
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppTheme.colors.textOnAccent,
                checkedTrackColor = AppTheme.colors.accent,
                checkedBorderColor = AppTheme.colors.accent,
                uncheckedThumbColor = AppTheme.colors.textSecondary,
                uncheckedTrackColor = AppTheme.colors.surfaceVariant,
                uncheckedBorderColor = AppTheme.colors.borderStrong,
            ),
        )
    }

    if (showConfirmDialog) {
        AppDialog(
            title = "Lyrics come from the internet",
            body = "To find lyrics, LockPlay sends the song title, artist, album, and length " +
                "to lrclib.net. " +
                "Nothing else leaves your device — not your album art, not what you've listened to " +
                "before. Lyrics aren't saved; they're fetched fresh and forgotten when the song changes.",
            confirmLabel = "Turn on lyrics",
            dismissLabel = "Not now",
            onConfirm = {
                showConfirmDialog = false
                onEnabledChange(true)
            },
            onDismiss = { showConfirmDialog = false },
        )
    }
}
