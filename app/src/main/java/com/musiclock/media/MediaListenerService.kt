package com.musiclock.media

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.util.Log
import com.musiclock.model.NowPlaying
import com.musiclock.trigger.LockLauncher

private const val TAG = "MediaListenerService"

/**
 * Reads the system's active media sessions and publishes a single [NowPlaying] snapshot to
 * [MediaRepository]. As a [NotificationListenerService] it holds notification-access, which is the
 * permission that grants [MediaSessionManager.getActiveSessions].
 *
 * It tracks the "top" session (preferring the one that is PLAYING), mirrors its metadata/playback
 * state, and listens for [Intent.ACTION_SCREEN_OFF] to hand off to [LockLauncher].
 */
class MediaListenerService : NotificationListenerService() {

    private var sessionManager: MediaSessionManager? = null
    private lateinit var componentName: ComponentName

    /** The controller we are currently mirroring, plus its attached callback (for clean detach). */
    private var controller: MediaController? = null
    private var controllerCallback: MediaController.Callback? = null

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            onSessionsChanged(controllers ?: emptyList())
        }

    private var screenOffReceiver: BroadcastReceiver? = null

    /** All session/controller callbacks are delivered here, so repository writes stay on one thread. */
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onListenerConnected() {
        super.onListenerConnected()
        // The framework may reconnect without an intervening disconnect; start from a clean slate
        // so we never end up with a duplicate session listener or a stale controller callback.
        teardown()
        componentName = ComponentName(this, MediaListenerService::class.java)
        sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

        // The framework can connect us transiently while notification access is mid-revocation (e.g.
        // right after the app's data is cleared); the session-listener registration then throws and
        // would take down the whole process. Treat it like a revoked grant and bail cleanly.
        try {
            sessionManager?.addOnActiveSessionsChangedListener(sessionsListener, componentName, mainHandler)
        } catch (e: SecurityException) {
            Log.w(TAG, "addOnActiveSessionsChangedListener denied — notification access revoked?", e)
            teardown()
            return
        }
        onSessionsChanged(activeSessions())

        registerScreenOffReceiver()
    }

    override fun onListenerDisconnected() {
        teardown()
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    // --- Session tracking -------------------------------------------------------------------

    private fun activeSessions(): List<MediaController> =
        try {
            sessionManager?.getActiveSessions(componentName) ?: emptyList()
        } catch (e: SecurityException) {
            Log.w(TAG, "getActiveSessions denied — notification access revoked?", e)
            emptyList()
        }

    private fun onSessionsChanged(controllers: List<MediaController>) {
        val candidates = controllers.filter { it.playbackState != null }
        val next = candidates.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: candidates.firstOrNull()

        if (next == null) {
            detachController()
            MediaRepository.clear()
            return
        }

        // Same controller, just publish a fresh snapshot (state may have changed).
        if (next == controller) {
            publish(next)
            return
        }

        detachController()
        controller = next
        controllerCallback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) = publish(next)
            override fun onPlaybackStateChanged(state: PlaybackState?) = publish(next)
            override fun onSessionDestroyed() = onSessionsChanged(activeSessions())
        }.also { next.registerCallback(it, mainHandler) }

        MediaRepository.setController(next)
        publish(next)
    }

    private fun detachController() {
        controllerCallback?.let { cb -> controller?.unregisterCallback(cb) }
        controllerCallback = null
        controller = null
    }

    /** Build a [NowPlaying] from the controller's current metadata + playback state and publish it. */
    private fun publish(controller: MediaController) {
        val metadata = controller.metadata
        val state = controller.playbackState

        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?.takeIf { it.isNotEmpty() }
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty()

        val albumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)

        MediaRepository.update(
            NowPlaying(
                isActive = true,
                title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
                artist = artist,
                albumArt = albumArt,
                isPlaying = state?.state == PlaybackState.STATE_PLAYING,
                positionMs = state?.position ?: 0L,
                durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
                positionUpdateTime = SystemClock.elapsedRealtime(),
                playbackSpeed = state?.playbackSpeed ?: 1f,
            )
        )
    }

    // --- Screen-off trigger -----------------------------------------------------------------

    private fun registerScreenOffReceiver() {
        if (screenOffReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_OFF) {
                    LockLauncher.onScreenOff(this@MediaListenerService)
                }
            }
        }
        registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_OFF), Context.RECEIVER_NOT_EXPORTED)
        screenOffReceiver = receiver
    }

    private fun unregisterScreenOffReceiver() {
        screenOffReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Screen-off receiver was not registered", e)
            }
        }
        screenOffReceiver = null
    }

    // --- Cleanup ----------------------------------------------------------------------------

    private fun teardown() {
        unregisterScreenOffReceiver()
        sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener)
        detachController()
        MediaRepository.clear()
    }
}
