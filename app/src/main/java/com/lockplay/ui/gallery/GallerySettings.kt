package com.lockplay.ui.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.lockplay.design.AppTheme
import com.lockplay.design.components.AppDialog
import com.lockplay.design.components.AppText
import com.lockplay.lyrics.LyricsRepository

/**
 * Whether turning the lyrics toggle to [requestedValue] must first show the pre-enable explanation
 * dialog. Compose-free (A13) so the gating rule is testable on the JVM without a UI harness.
 * Turning on always requires confirmation; turning off never does.
 */
fun requiresConfirmation(requestedValue: Boolean): Boolean = requestedValue

/**
 * Whether the local-lyrics affordances (permission + folder picker) should be shown. Compose-free
 * (A13) so it's testable on the JVM: the section only makes sense once lyrics are enabled at all.
 */
fun showLocalLyricsSection(lyricsEnabled: Boolean): Boolean = lyricsEnabled

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

/**
 * Local-lyrics affordances shown below [LyricsSettingRow] once lyrics are enabled
 * ([showLocalLyricsSection]): a runtime permission row for reading on-device audio to search for
 * embedded lyrics, and a folder picker row for a `.lrc` folder. Stateless/hoisted like the row above
 * — permission state is read from the platform since it isn't app state to hoist.
 */
@Composable
fun LocalLyricsSettings(
    folderUri: String,
    onFolderUriChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var readAudioGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> readAudioGranted = granted }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (e: SecurityException) {
                Log.w("LocalLyricsSettings", "Failed to persist folder permission", e)
            }
            LyricsRepository.clearCache()
            onFolderUriChange(uri.toString())
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !readAudioGranted) {
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                },
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                modifier = Modifier.weight(1f),
            ) {
                AppText("Read music files", AppTheme.typography.body, color = AppTheme.colors.textPrimary)
                AppText(
                    "Find lyrics inside songs on this device",
                    AppTheme.typography.timestamp,
                    color = AppTheme.colors.textSecondary,
                )
            }
            AppText(
                if (readAudioGranted) "Granted" else "Allow",
                AppTheme.typography.timestamp,
                color = AppTheme.colors.accentOnSurface,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { folderLauncher.launch(null) },
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                modifier = Modifier.weight(1f),
            ) {
                AppText("Lyrics folder", AppTheme.typography.body, color = AppTheme.colors.textPrimary)
                AppText(
                    "Folder with .lrc files",
                    AppTheme.typography.timestamp,
                    color = AppTheme.colors.textSecondary,
                )
            }
            if (folderUri.isNotEmpty()) {
                AppText(
                    "Clear",
                    AppTheme.typography.timestamp,
                    color = AppTheme.colors.accentOnSurface,
                    modifier = Modifier.clickable { onFolderUriChange("") },
                )
            }
        }
    }
}
