package com.lockplay.trigger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.lockplay.media.MediaRepository
import com.lockplay.ui.lockscreen.LockscreenActivity

/**
 * Launches [LockscreenActivity] when the screen turns off while media is playing.
 *
 * On Android 12+ a plain `startActivity()` from a receiver/foreground service is
 * silently dropped by Background-Activity-Launch (BAL) restrictions. To survive
 * this we use a 3-tier strategy, picking the strongest path available at runtime:
 *
 *  - Tier 1 (full-screen-intent): the BAL-blessed path. A HIGH-importance
 *    notification with `setFullScreenIntent(..., true)` is allowed to bring up
 *    an activity over the lockscreen. Requires `USE_FULL_SCREEN_INTENT` to be
 *    granted (API 34+ may deny it; on API 33 it is implicitly granted).
 *  - Tier 2 (overlay BAL exemption): if FSI is denied but the app can draw
 *    overlays, that permission grants a BAL exemption, so we post the
 *    notification AND attempt a direct `startActivity()`.
 *  - Tier 3 (tap-to-open): nothing privileged is available, so we post a
 *    high-priority notification whose content intent opens the lockscreen when
 *    the user taps it.
 */
object LockLauncher {

    private const val TAG = "LockLauncher"
    private const val CHANNEL_ID = "lockplay_lockscreen"
    private const val CHANNEL_NAME = "Lockscreen"
    private const val NOTIFICATION_ID = 1001

    /**
     * Entry point: called when the screen turns off. No-op unless media is
     * playing. Ensures the channel exists, then launches via the best tier.
     */
    fun onScreenOff(context: Context) {
        if (!MediaRepository.isPlaying()) return

        ensureChannel(context)

        val intent = buildLockIntent(context)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when {
            canUseFullScreenIntent(nm) -> {
                Log.i(TAG, "Tier 1: full-screen-intent")
                // The notification is only a vehicle for the full-screen intent — once the activity
                // is up there is nothing left to tap, but the heads-up banner would otherwise ride
                // out its timeout on top of the lockscreen we just launched. LockscreenActivity
                // cancels it in onCreate; this is the backstop for when the activity never arrives.
                // ponytail: 1s is "long enough to launch" — raise it only if a slow device shows the
                // lockscreen after the notification is already gone.
                val notification = baseBuilder(context, pendingIntent)
                    .setFullScreenIntent(pendingIntent, true)
                    .setTimeoutAfter(1_000)
                    .build()
                nm.notify(NOTIFICATION_ID, notification)
            }

            Settings.canDrawOverlays(context) -> {
                Log.i(TAG, "Tier 2: overlay BAL exemption")
                val notification = baseBuilder(context, pendingIntent).build()
                nm.notify(NOTIFICATION_ID, notification)
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.w(TAG, "Tier 2: startActivity blocked, falling back to notification", e)
                }
            }

            else -> {
                Log.i(TAG, "Tier 3: tap-to-open notification")
                val notification = baseBuilder(context, pendingIntent).build()
                nm.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    /**
     * Creates the lockscreen notification channel. Idempotent — calling
     * createNotificationChannel with the same id simply updates it.
     */
    /** Dismiss the launch notification once the lockscreen is on screen, so no cruft lingers. */
    fun cancel(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(NOTIFICATION_ID)
    }

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null)
        }
        nm.createNotificationChannel(channel)
    }

    /**
     * Builds the intent that opens [LockscreenActivity] as a new task.
     */
    fun buildLockIntent(context: Context): Intent =
        Intent(context, LockscreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

    private fun baseBuilder(context: Context, contentIntent: PendingIntent): Notification.Builder =
        Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("LockPlay")
            .setContentText("Tap to open lockscreen")
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setContentIntent(contentIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)

    /**
     * `canUseFullScreenIntent()` exists from API 34. On API 33 the permission
     * is implicitly granted, so treat it as available.
     */
    private fun canUseFullScreenIntent(nm: NotificationManager): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            nm.canUseFullScreenIntent()
        } else {
            true
        }
}
