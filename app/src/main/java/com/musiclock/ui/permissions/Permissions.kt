package com.musiclock.ui.permissions

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Declarative description of every permission the app needs for the lockscreen experience.
 *
 * Each entry knows how to (a) report whether it is currently granted and (b) produce the Intent that
 * sends the user to the exact settings screen / runtime prompt that grants it. The UI layer
 * ([PermissionGate]) renders these and re-polls [isGranted] on resume.
 */

/** Whether our [com.musiclock.media.MediaListenerService] is enabled as a notification listener. */
fun isNotificationListenerEnabled(context: Context): Boolean {
    if (NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)) {
        return true
    }
    // Fallback: parse the raw secure setting (component flattened strings contain our package).
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return !flat.isNullOrEmpty() && flat.split(":").any { it.contains(context.packageName) }
}

private fun packageUri(context: Context): Uri = Uri.fromParts("package", context.packageName, null)

/**
 * How granting a permission is triggered from the UI.
 *
 * - [RuntimePermission] uses an `ActivityResultContracts.RequestPermission` launcher.
 * - [SettingsScreen] uses a `StartActivityForResult` launcher to open a settings page (no result;
 *   state is re-polled on resume).
 */
sealed interface PermissionAction {
    /** Request a dangerous runtime permission via the system dialog. */
    data class RuntimePermission(val manifestPermission: String) : PermissionAction

    /** Open a settings screen that lets the user toggle the permission manually. */
    data class SettingsScreen(val intent: (Context) -> Intent) : PermissionAction
}

/**
 * A single permission row shown during onboarding.
 *
 * @property optional when true the app still works without it; it never blocks the "all set" state.
 */
enum class AppPermission(
    val title: String,
    val rationale: String,
    val optional: Boolean = false,
) {
    PostNotifications(
        title = "Notifications",
        rationale = "Show the music controls and the full-screen lockscreen as a notification.",
        optional = true,
    ) {
        override fun isGranted(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED

        override val action = PermissionAction.RuntimePermission(Manifest.permission.POST_NOTIFICATIONS)
    },

    NotificationListener(
        title = "Notification access",
        rationale = "Read media notifications so we know what's playing and can control it.",
    ) {
        override fun isGranted(context: Context): Boolean = isNotificationListenerEnabled(context)

        override val action = PermissionAction.SettingsScreen { _ ->
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }
    },

    DisplayOverApps(
        title = "Display over other apps",
        rationale = "Draw the music controls on top of the lock screen.",
        optional = true,
    ) {
        override fun isGranted(context: Context): Boolean = Settings.canDrawOverlays(context)

        override val action = PermissionAction.SettingsScreen { ctx ->
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri(ctx))
        }
    },

    FullScreenIntent(
        title = "Full-screen notifications",
        rationale = "Launch the immersive lockscreen player when the screen turns on.",
        optional = true,
    ) {
        override fun isGranted(context: Context): Boolean {
            // Only gated from API 34; on API 33 it is always allowed.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
            val nm = context.getSystemService(NotificationManager::class.java)
            return nm?.canUseFullScreenIntent() ?: false
        }

        override val action = PermissionAction.SettingsScreen { ctx ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri(ctx))
            } else {
                // No dedicated screen pre-34; fall back to the app's notification settings.
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            }
        }
    },

    BatteryOptimization(
        title = "Ignore battery optimizations",
        rationale = "Keep the listener alive in the background so playback is never missed.",
        optional = true,
    ) {
        override fun isGranted(context: Context): Boolean {
            val pm = context.getSystemService(PowerManager::class.java)
            return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        }

        override val action = PermissionAction.SettingsScreen { ctx ->
            // ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS prompts directly for our package.
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri(ctx))
        }
    };

    /** Whether the permission is currently granted on this device. */
    abstract fun isGranted(context: Context): Boolean

    /** How the UI grants this permission. */
    abstract val action: PermissionAction
}
