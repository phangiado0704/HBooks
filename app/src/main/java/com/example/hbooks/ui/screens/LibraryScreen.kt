package com.example.hbooks.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hbooks.data.models.Book
import com.example.hbooks.data.models.Playlist
import com.example.hbooks.ui.components.BookItem
import com.example.hbooks.ui.components.ErrorState
import com.example.hbooks.ui.components.LoadingState
import com.example.hbooks.ui.components.SectionTitle
import com.example.hbooks.ui.viewmodels.LibraryViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onBackClick: () -> Unit, onBookClick: (String) -> Unit) {
    val viewModel: LibraryViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val recentlyPlayedIds by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val books = uiState.books
    val bookById = remember(books) { books.associateBy { it.id } }
    val recentlyPlayedBooks = recentlyPlayedIds.mapNotNull { bookById[it] }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var playlistForRename by remember { mutableStateOf<Playlist?>(null) }
    var playlistForDelete by remember { mutableStateOf<Playlist?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library", color = Color.Red, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            item {
                when {
                    uiState.isLoading -> LoadingState(modifier = Modifier.fillMaxWidth())
                    uiState.errorMessage != null -> ErrorState(
                        message = uiState.errorMessage!!,
                        onRetry = viewModel::refreshBooks,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (!uiState.isLoading && uiState.errorMessage == null) {
                item {
                    SectionTitle(title = "Recently Played")
                    RecentlyPlayedSection(books = recentlyPlayedBooks, onBookClick = onBookClick)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    SectionTitle(title = "Playlists")
                    PlaylistsSection(
                        playlists = playlists,
                        onPlaylistClick = { playlist -> selectedPlaylist = playlist }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    selectedPlaylist?.let { playlist ->
        val playlistBooks = books.filter { playlist.bookIds.contains(it.id) }
        PlaylistDetailDialog(
            playlist = playlist,
            books = playlistBooks,
            onDismiss = { selectedPlaylist = null },
            onRename = {
                playlistForRename = playlist
                selectedPlaylist = null
            },
            onDelete = {
                playlistForDelete = playlist
                selectedPlaylist = null
            },
            onBookClick = onBookClick
        )
    }

    playlistForRename?.let { playlist ->
        val playlistBooks = books.filter { playlist.bookIds.contains(it.id) }
        EditPlaylistDialog(
            playlist = playlist,
            books = playlistBooks,
            onRename = { newName ->
                viewModel.renamePlaylist(playlist.id, newName)
                playlistForRename = null
            },
            onRemoveBook = { bookId ->
                viewModel.removeBookFromPlaylist(playlist.id, bookId)
                playlistForRename = playlistForRename?.let {
                    it.copy(bookIds = it.bookIds.filterNot { id -> id == bookId })
                }
            },
            onDismiss = { playlistForRename = null }
        )
    }

    playlistForDelete?.let { playlist ->
        DeletePlaylistDialog(
            playlistName = playlist.name,
            onConfirm = {
                viewModel.deletePlaylist(playlist.id)
                playlistForDelete = null
            },
            onDismiss = { playlistForDelete = null }
        )
    }
}

@Composable
private fun RecentlyPlayedSection(books: List<Book>, onBookClick: (String) -> Unit) {
    if (books.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No audiobooks played yet",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    } else {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(books) { book ->
                BookItem(book = book, onBookClick = onBookClick, modifier = Modifier.width(120.dp))
            }
        }
    }
}

@Composable
private fun PlaylistsSection(
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit
) {
    if (playlists.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "There are no playlists yet",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            playlists.chunked(2).forEach { rowPlaylists ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowPlaylists.forEach { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowPlaylists.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.Red
            )
            Text(
                text = "${playlist.bookIds.size} titles",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
private fun PlaylistDetailDialog(
    playlist: Playlist,
    books: List<Book>,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onBookClick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(playlist.name) },
        text = {
            Column {
                Text(
                    text = "${books.size} ${if (books.size == 1) "title" else "titles"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (books.isEmpty()) {
                    Text(
                        text = "No books saved to this playlist yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                } else {
                    books.forEach { book ->
                        TextButton(onClick = { onBookClick(book.id) }) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(book.title, style = MaterialTheme.typography.bodyLarge)
                                Text(book.author, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = Color.Red)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onRename) {
                    Text("Edit")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun EditPlaylistDialog(
    playlist: Playlist,
    books: List<Book>,
    onRename: (String) -> Unit,
    onRemoveBook: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember(playlist.id) { mutableStateOf(playlist.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit playlist") },
        text = {
            Column {
                Text("Update the playlist name", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Playlist name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (books.isEmpty()) {
                    Text(
                        text = "No audiobooks saved to this playlist yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                } else {
                    Text("Audiobooks in this playlist", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    books.forEach { book ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(book.title, style = MaterialTheme.typography.bodyLarge)
                                Text(book.author, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            IconButton(onClick = { onRemoveBook(book.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove from playlist"
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onRename(newName.trim()) },
                enabled = newName.isNotBlank() && newName.trim() != playlist.name
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DeletePlaylistDialog(
    playlistName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete playlist") },
        text = {
            Text(
                text = "Are you sure you want to delete \"$playlistName\"? This cannot be undone.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete", color = Color.Red) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
