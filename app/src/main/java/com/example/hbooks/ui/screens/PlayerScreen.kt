package com.example.hbooks.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.ModeNight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.hbooks.data.models.Book
import com.example.hbooks.data.models.Playlist
import com.example.hbooks.ui.viewmodels.PlayerUiState
import com.example.hbooks.ui.viewmodels.PlayerViewModel
import java.util.concurrent.TimeUnit

private val accentColor = Color(0xFFF88A8A)
private val backgroundColor = Color(0xFFFAFAFA)
private val secondaryTextColor = Color(0xFF9E9E9E)
private val primaryIconColor = Color(0xFF4D4D4D)
private val headerGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFF88A8A), Color(0xFFFFB6B6))
)

@Composable
fun PlayerScreen(bookId: String?, onBackClick: () -> Unit) {
    val viewModel: PlayerViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val book by viewModel.currentBook.collectAsStateWithLifecycle()
    val player by viewModel.player.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    var showSleepDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(bookId) {
        if (bookId != null) {
            viewModel.play(bookId)
        }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        HeaderBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(75.dp))

            book?.let { currentBook ->
                val isBookSaved = playlists.any { playlist -> currentBook.id in playlist.bookIds }
                PlayerArtwork(
                    book = currentBook,
                    player = player,
                    modifier = Modifier.offset(y = (-20).dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                BookDetails(book = currentBook)

                Spacer(modifier = Modifier.height(16.dp))

                SeekBarSection(uiState = uiState, onSeek = viewModel::onSeek)

                Spacer(modifier = Modifier.height(24.dp))

                PlayerControls(
                    isPlaying = uiState.isPlaying,
                    onPlayPause = viewModel::onPlayPause,
                    onRewind = viewModel::onRewind,
                    onFastForward = viewModel::onFastForward,
                    onSkipPrevious = viewModel::onSkipPrevious,
                    onSkipNext = viewModel::onSkipNext
                )

                Spacer(modifier = Modifier.height(20.dp))

                SecondaryControls(
                    sleepTimerRemainingMs = uiState.sleepTimerRemainingMs,
                    repeatMode = uiState.repeatMode,
                    isShuffleEnabled = uiState.isShuffleEnabled,
                    isBookSaved = isBookSaved,
                    onSleepTimerClick = { showSleepDialog = true },
                    onRepeatToggle = viewModel::cycleRepeatAndShuffleMode,
                    onSaveClick = { showSaveDialog = true }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        HeaderTopBar(onBackClick = onBackClick)
    }

    if (showSleepDialog) {
        SleepTimerDialog(
            isTimerActive = uiState.sleepTimerRemainingMs != null,
            onDismiss = { showSleepDialog = false },
            onTimerSelected = { minutes ->
                viewModel.setSleepTimer(minutes)
                showSleepDialog = false
            },
            onClearTimer = {
                viewModel.clearSleepTimer()
                showSleepDialog = false
            }
        )
    }

    val currentBook = book
    if (showSaveDialog && currentBook != null) {
        SaveToPlaylistDialog(
            playlists = playlists,
            bookTitle = currentBook.title,
            onPlaylistSelected = { playlistId ->
                viewModel.addCurrentBookToPlaylist(playlistId)
                showSaveDialog = false
            },
            onCreatePlaylist = { playlistName ->
                viewModel.createPlaylistAndAdd(playlistName)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false }
        )
    }
}

@Composable
private fun HeaderBackground() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(bottomStart = 220.dp, bottomEnd = 220.dp))
            .background(headerGradient)
    )
}

@Composable
private fun HeaderTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
        }
        Text("Now Playing", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.width(48.dp))
    }
}

@Composable
private fun PlayerArtwork(book: Book, player: Player?, modifier: Modifier = Modifier) {
    val showVideoSurface = (player?.videoSize?.width ?: 0) > 0
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        modifier = modifier
            .width(220.dp)
            .height(330.dp)
    ) {
        if (showVideoSurface && player != null) {
            VideoSurface(player = player)
        } else {
            AsyncImage(
                model = book.coverImageUrl,
                contentDescription = "Book Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(UnstableApi::class)
@Composable
private fun VideoSurface(player: Player, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            PlayerView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                useController = false
                setShutterBackgroundColor(Color.Transparent.toArgb())
                this.player = player
            }
        }
    )
}

@Composable
private fun BookDetails(book: Book) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = book.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = book.author,
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryTextColor
        )
    }
}

@Composable
private fun SeekBarSection(uiState: PlayerUiState, onSeek: (Long) -> Unit) {
    val duration = uiState.duration.takeIf { it > 0 } ?: 1L
    val position = uiState.currentPosition.coerceIn(0L, duration)
    Column {
        Slider(
            value = position.toFloat(),
            onValueChange = { onSeek(it.toLong().coerceIn(0L, duration)) },
            valueRange = 0f..duration.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color.LightGray.copy(alpha = 0.3f)
            )
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(position), style = MaterialTheme.typography.bodySmall, color = secondaryTextColor)
            Text(formatTime(duration), style = MaterialTheme.typography.bodySmall, color = secondaryTextColor)
        }
    }
}

@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onRewind: () -> Unit,
    onFastForward: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onSkipPrevious) { Icon(Icons.Default.SkipPrevious, "Skip Previous", modifier = Modifier.size(32.dp), tint = primaryIconColor) }
        IconButton(onClick = onRewind) { Icon(Icons.Default.FastRewind, "Rewind", modifier = Modifier.size(32.dp), tint = primaryIconColor) }
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(accentColor)
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
        IconButton(onClick = onFastForward) { Icon(Icons.Default.FastForward, "Fast Forward", modifier = Modifier.size(32.dp), tint = primaryIconColor) }
        IconButton(onClick = onSkipNext) { Icon(Icons.Default.SkipNext, "Skip Next", modifier = Modifier.size(32.dp), tint = primaryIconColor) }
    }
}

@Composable
private fun SecondaryControls(
    sleepTimerRemainingMs: Long?,
    repeatMode: Int,
    isShuffleEnabled: Boolean,
    isBookSaved: Boolean,
    onSleepTimerClick: () -> Unit,
    onRepeatToggle: () -> Unit,
    onSaveClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val sleepDescription = sleepTimerRemainingMs?.let { "Ends in ${formatTime(it)}" } ?: "Off"
        SecondaryControlItem(
            icon = Icons.Outlined.ModeNight,
            label = "Sleep Timer",
            description = sleepDescription,
            active = sleepTimerRemainingMs != null,
            onClick = onSleepTimerClick
        )

        val repeatDescription = when {
            isShuffleEnabled -> "Shuffle"
            repeatMode == Player.REPEAT_MODE_ONE -> "Repeat 1"
            repeatMode == Player.REPEAT_MODE_ALL -> "Repeat all"
            else -> "Off"
        }
        SecondaryControlItem(
            icon = Icons.Default.Repeat,
            label = "Repeat / Shuffle",
            description = repeatDescription,
            active = repeatMode != Player.REPEAT_MODE_OFF || isShuffleEnabled,
            onClick = onRepeatToggle
        )

        val saveDescription = if (isBookSaved) "In playlist" else "Add to playlist"
        SecondaryControlItem(
            icon = Icons.Default.PlaylistAdd,
            label = "Save",
            description = saveDescription,
            active = isBookSaved,
            onClick = onSaveClick
        )
    }
}

@Composable
private fun SecondaryControlItem(
    icon: ImageVector,
    label: String,
    description: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val tint = if (active) accentColor else secondaryTextColor
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(32.dp))
        }
        Text(label, color = tint, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(
            text = description,
            color = secondaryTextColor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun SleepTimerDialog(
    isTimerActive: Boolean,
    onDismiss: () -> Unit,
    onTimerSelected: (Int) -> Unit,
    onClearTimer: () -> Unit
) {
    val options = listOf(15, 30, 45, 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep timer") },
        text = {
            Column {
                Text(
                    text = "Stop playback after:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = primaryIconColor
                )
                Spacer(modifier = Modifier.height(12.dp))
                options.forEach { minutes ->
                    TextButton(onClick = { onTimerSelected(minutes) }) {
                        Text("$minutes minutes")
                    }
                }
                if (isTimerActive) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = onClearTimer) {
                        Text("Turn off timer")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun SaveToPlaylistDialog(
    playlists: List<Playlist>,
    bookTitle: String,
    onPlaylistSelected: (String) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newPlaylistName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save \"$bookTitle\"") },
        text = {
            Column {
                if (playlists.isNotEmpty()) {
                    Text(
                        text = "Choose a playlist",
                        style = MaterialTheme.typography.bodyMedium,
                        color = primaryIconColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    playlists.forEach { playlist ->
                        TextButton(onClick = { onPlaylistSelected(playlist.id) }) {
                            Text(playlist.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Text(
                        text = "No playlists yet. Create one below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryTextColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("New playlist name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreatePlaylist(newPlaylistName.trim())
                    newPlaylistName = ""
                },
                enabled = newPlaylistName.isNotBlank()
            ) {
                Text("Create & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

private fun formatTime(timeMs: Long): String {
    val safeTime = timeMs.takeUnless { it == Long.MIN_VALUE }?.coerceAtLeast(0L) ?: 0L
    val minutes = TimeUnit.MILLISECONDS.toMinutes(safeTime)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(safeTime) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%02d:%02d", minutes, seconds)
}
