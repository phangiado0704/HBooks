package com.example.hbooks.services

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.hbooks.data.models.Book
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackState(
    val currentBook: Book? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0,
    val duration: Long = 0
)

/**
 * Singleton that tracks playback state across the app.
 * Used by MiniPlayer to show current playback without needing PlayerViewModel.
 */
object PlaybackStateManager {

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    fun initialize(context: Context) {
        if (controllerFuture != null) return

        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future

        future.addListener(
            {
                val controller = future.get()
                mediaController = controller

                controller.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                    }

                    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                        // Metadata changed, but book info comes from updateCurrentBook
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            _playbackState.value = _playbackState.value.copy(
                                duration = controller.duration
                            )
                        }
                        if (playbackState == Player.STATE_ENDED) {
                            _playbackState.value = _playbackState.value.copy(isPlaying = false)
                        }
                    }
                })

                // Sync initial state
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = controller.isPlaying,
                    duration = controller.duration.coerceAtLeast(0)
                )
            },
            MoreExecutors.directExecutor()
        )
    }

    fun updateCurrentBook(book: Book?) {
        _playbackState.value = _playbackState.value.copy(currentBook = book)
    }

    fun updatePosition(position: Long) {
        _playbackState.value = _playbackState.value.copy(currentPosition = position)
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun clearPlayback() {
        _playbackState.value = PlaybackState()
    }

    fun release() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
    }
}
