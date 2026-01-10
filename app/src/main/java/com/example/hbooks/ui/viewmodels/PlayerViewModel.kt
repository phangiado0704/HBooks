package com.example.hbooks.ui.viewmodels

import android.app.Application
import android.content.ComponentName
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.hbooks.data.models.Book
import com.example.hbooks.data.models.Playlist
import com.example.hbooks.data.repository.BookRepository
import com.example.hbooks.data.repository.PlaybackPositionRepository
import com.example.hbooks.data.repository.PlaylistRepository
import com.example.hbooks.data.repository.RecentlyPlayedRepository
import com.example.hbooks.services.PlaybackService
import com.example.hbooks.services.PlaybackStateManager
import com.example.hbooks.util.FirebaseStorageFetcher
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val mediaItem: MediaItem? = null,
    val sleepTimerRemainingMs: Long? = null,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val isShuffleEnabled: Boolean = false,
    val playbackSpeed: Float = 1.0f
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    application: Application,
    private val bookRepository: BookRepository,
    private val storageFetcher: FirebaseStorageFetcher
) : AndroidViewModel(application) {

    private val playlistRepository = PlaylistRepository
    private val recentlyPlayedRepository = RecentlyPlayedRepository
    private val playbackPositionRepository = PlaybackPositionRepository

    private val _player = MutableStateFlow<Player?>(null)
    val player = _player.asStateFlow()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()

    // This will hold the book details for the UI
    private val _currentBook = MutableStateFlow<Book?>(null)
    val currentBook = _currentBook.asStateFlow()
    private val playbackQueue = MutableStateFlow<List<Book>>(emptyList())

    private var mediaController: MediaController? = null
    private val controllerFuture: ListenableFuture<MediaController>
    private var sleepTimerJob: Job? = null

    val playlists: StateFlow<List<Playlist>> = playlistRepository.playlistsFlow()

    init {
        val sessionToken = SessionToken(getApplication(), ComponentName(getApplication(), PlaybackService::class.java))
        controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                val controller = controllerFuture.get()
                mediaController = controller
                _player.value = controller
                _uiState.update {
                    it.copy(
                        repeatMode = controller.repeatMode,
                        isShuffleEnabled = controller.shuffleModeEnabled
                    )
                }

                controller.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _uiState.update { it.copy(isPlaying = isPlaying) }
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        _uiState.update { it.copy(mediaItem = mediaItem, duration = controller.duration) }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            _uiState.update { it.copy(duration = controller.duration) }
                        }
                    }

                    override fun onRepeatModeChanged(repeatMode: Int) {
                        _uiState.update { it.copy(repeatMode = repeatMode) }
                    }

                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                        _uiState.update { it.copy(isShuffleEnabled = shuffleModeEnabled) }
                    }

                    override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
                        _uiState.update { it.copy(playbackSpeed = playbackParameters.speed) }
                    }
                })

                // Update position every second and auto-save every 10 seconds
                viewModelScope.launch {
                    var autoSaveCounter = 0
                    while (true) {
                        if (controller.isPlaying) {
                            val position = controller.currentPosition
                            _uiState.update { it.copy(currentPosition = position) }
                            // Sync with PlaybackStateManager for MiniPlayer
                            PlaybackStateManager.updatePosition(position)
                            autoSaveCounter++
                            // Auto-save position every 10 seconds
                            if (autoSaveCounter >= 10) {
                                autoSaveCounter = 0
                                saveCurrentPosition()
                            }
                        }
                        delay(1000)
                    }
                }
            },
            MoreExecutors.directExecutor()
        )

        loadPlaybackQueue()
        
        // Load saved positions from Firestore on startup
        viewModelScope.launch {
            playbackPositionRepository.loadFromFirestore()
        }
    }

    fun play(mediaId: String) {
        viewModelScope.launch {
            // Save position of current book before switching
            saveCurrentPosition()

            bookRepository.getBook(mediaId)
                .onSuccess { book ->
                    if (book != null) {
                        _currentBook.value = book
                        // Sync with PlaybackStateManager for MiniPlayer
                        PlaybackStateManager.updateCurrentBook(book)
                        if (book.audioUrl.isBlank()) {
                            Log.e(TAG, "Book ${book.id} does not contain an audioUrl.")
                            _uiState.update { it.copy(isPlaying = false) }
                            return@onSuccess
                        }
                        recentlyPlayedRepository.markPlayed(book.id)
                        val playbackUrl = try {
                            storageFetcher.getDownloadUrl(book.audioUrl)
                        } catch (error: Exception) {
                            Log.e(TAG, "Unable to resolve audio URL for ${book.id}", error)
                            _uiState.update { it.copy(isPlaying = false) }
                            null
                        }
                        if (playbackUrl != null) {
                            // Get saved position for this book
                            val savedPosition = playbackPositionRepository.getPositionMs(book.id)
                            
                            val mediaItem = MediaItem.Builder()
                                .setMediaId(book.id)
                                .setUri(playbackUrl)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(book.title)
                                        .setArtist(book.author)
                                        .build()
                                )
                                .build()
                            mediaController?.setMediaItem(mediaItem)
                            mediaController?.prepare()
                            
                            // Seek to saved position if available
                            if (savedPosition > 0) {
                                mediaController?.seekTo(savedPosition)
                                Log.d(TAG, "Resuming ${book.id} from ${savedPosition}ms")
                            }
                            
                            mediaController?.play()
                        }
                    } else {
                        Log.e(TAG, "Book with id $mediaId not found.")
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "Unable to start playback for $mediaId", error)
                    _uiState.update { it.copy(isPlaying = false) }
                }
        }
    }

    fun onPlayPause() {
        if (mediaController?.isPlaying == true) {
            mediaController?.pause()
            // Save position when pausing
            viewModelScope.launch { saveCurrentPosition() }
        } else {
            mediaController?.play()
        }
    }

    fun onSeek(position: Long) {
        mediaController?.seekTo(position)
        // Save position after seeking
        viewModelScope.launch { saveCurrentPosition() }
    }

    fun onRewind() {
        mediaController?.seekBack()
    }

    fun onFastForward() {
        mediaController?.seekForward()
    }

    private suspend fun saveCurrentPosition() {
        val bookId = _currentBook.value?.id ?: return
        val position = mediaController?.currentPosition ?: return
        val duration = mediaController?.duration ?: return
        if (position > 0 && duration > 0) {
            playbackPositionRepository.savePosition(bookId, position, duration)
        }
    }

    fun onSkipPrevious() {
        val previousBook = findAdjacentBook(forward = false)
        if (previousBook != null) {
            play(previousBook.id)
        } else {
            mediaController?.seekTo(0)
        }
    }

    fun onSkipNext() {
        val nextBook = findAdjacentBook(forward = true)
        if (nextBook != null) {
            play(nextBook.id)
        }
    }

    fun setSleepTimer(minutes: Int) {
        startSleepTimer(minutes * 60 * 1000L)
    }

    fun clearSleepTimer() {
        cancelSleepTimer()
        _uiState.update { it.copy(sleepTimerRemainingMs = null) }
    }

    private fun startSleepTimer(durationMs: Long) {
        cancelSleepTimer()
        _uiState.update { it.copy(sleepTimerRemainingMs = durationMs) }
        val job = viewModelScope.launch {
            var remaining = durationMs
            try {
                while (remaining > 0 && isActive) {
                    delay(1000)
                    remaining -= 1000
                    _uiState.update { it.copy(sleepTimerRemainingMs = remaining.coerceAtLeast(0)) }
                }
                if (remaining <= 0 && isActive) {
                    mediaController?.pause()
                }
            } finally {
                _uiState.update { it.copy(sleepTimerRemainingMs = null) }
            }
        }
        job.invokeOnCompletion { sleepTimerJob = null }
        sleepTimerJob = job
    }

    private fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
    }

    fun cycleRepeatAndShuffleMode() {
        val controller = mediaController ?: return
        val nextState = when {
            !controller.shuffleModeEnabled && controller.repeatMode == Player.REPEAT_MODE_OFF -> PlaybackMode.REPEAT_ALL
            !controller.shuffleModeEnabled && controller.repeatMode == Player.REPEAT_MODE_ALL -> PlaybackMode.REPEAT_ONE
            !controller.shuffleModeEnabled && controller.repeatMode == Player.REPEAT_MODE_ONE -> PlaybackMode.SHUFFLE
            controller.shuffleModeEnabled -> PlaybackMode.OFF
            else -> PlaybackMode.OFF
        }
        when (nextState) {
            PlaybackMode.REPEAT_ALL -> {
                controller.shuffleModeEnabled = false
                controller.repeatMode = Player.REPEAT_MODE_ALL
            }
            PlaybackMode.REPEAT_ONE -> {
                controller.shuffleModeEnabled = false
                controller.repeatMode = Player.REPEAT_MODE_ONE
            }
            PlaybackMode.SHUFFLE -> {
                controller.repeatMode = Player.REPEAT_MODE_OFF
                controller.shuffleModeEnabled = true
            }
            PlaybackMode.OFF -> {
                controller.repeatMode = Player.REPEAT_MODE_OFF
                controller.shuffleModeEnabled = false
            }
        }
        _uiState.update {
            it.copy(
                repeatMode = controller.repeatMode,
                isShuffleEnabled = controller.shuffleModeEnabled
            )
        }
    }

    fun cyclePlaybackSpeed() {
        val controller = mediaController ?: return
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        val currentSpeed = _uiState.value.playbackSpeed
        val currentIndex = speeds.indexOfFirst { kotlin.math.abs(it - currentSpeed) < 0.01f }
        val nextIndex = if (currentIndex == -1 || currentIndex == speeds.lastIndex) 0 else currentIndex + 1
        val newSpeed = speeds[nextIndex]
        controller.setPlaybackSpeed(newSpeed)
        _uiState.update { it.copy(playbackSpeed = newSpeed) }
    }

    fun addCurrentBookToPlaylist(playlistId: String) {
        val bookId = _currentBook.value?.id ?: return
        playlistRepository.addBookToPlaylist(playlistId, bookId)
    }

    fun createPlaylistAndAdd(name: String) {
        val bookId = _currentBook.value?.id ?: return
        playlistRepository.createPlaylist(name, bookId)
    }

    override fun onCleared() {
        // Save position before clearing
        val bookId = _currentBook.value?.id
        val position = mediaController?.currentPosition
        val duration = mediaController?.duration
        if (bookId != null && position != null && duration != null && position > 0 && duration > 0) {
            // Use runBlocking since we're in onCleared and need to complete before destruction
            kotlinx.coroutines.runBlocking {
                playbackPositionRepository.savePosition(bookId, position, duration)
            }
        }
        
        sleepTimerJob?.cancel()
        MediaController.releaseFuture(controllerFuture)
        super.onCleared()
    }

    companion object {
        private const val TAG = "PlayerViewModel"
    }

    private enum class PlaybackMode {
        OFF,
        REPEAT_ALL,
        REPEAT_ONE,
        SHUFFLE
    }

    private fun loadPlaybackQueue() {
        viewModelScope.launch {
            bookRepository.getBooks()
                .onSuccess { books ->
                    playbackQueue.value = books
                }
                .onFailure { error ->
                    Log.e(TAG, "Unable to load playback queue", error)
                }
        }
    }

    private fun findAdjacentBook(forward: Boolean): Book? {
        val queue = playbackQueue.value
        if (queue.isEmpty()) {
            loadPlaybackQueue()
            return null
        }
        val currentId = _currentBook.value?.id
        val currentIndex = queue.indexOfFirst { it.id == currentId }
        if (_uiState.value.isShuffleEnabled && queue.size > 1) {
            val randomIndex = queue.indices.filter { it != currentIndex }.random()
            return queue[randomIndex]
        }
        if (currentIndex == -1) {
            return queue.firstOrNull()
        }
        val nextIndex = if (forward) {
            if (currentIndex == queue.lastIndex) 0 else currentIndex + 1
        } else {
            if (currentIndex == 0) queue.lastIndex else currentIndex - 1
        }
        return queue.getOrNull(nextIndex)
    }
}
