package com.example.hbooks.data.repository

import com.example.hbooks.data.models.Playlist
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PlaylistRepository {

    private const val ANONYMOUS_USER_ID = "anonymous"

    private val firebaseAuth = FirebaseAuth.getInstance()
    private var activeUserId = firebaseAuth.currentUser?.uid ?: ANONYMOUS_USER_ID

    private val playlistsByUser = mutableMapOf<String, List<Playlist>>()
    private val playlists = MutableStateFlow(loadPlaylistsForUser(activeUserId))

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        val newUserId = auth.currentUser?.uid ?: ANONYMOUS_USER_ID
        setActiveUser(newUserId)
    }

    init {
        firebaseAuth.addAuthStateListener(authListener)
    }

    fun playlistsFlow(): StateFlow<List<Playlist>> = playlists.asStateFlow()

    fun addBookToPlaylist(playlistId: String, bookId: String) {
        updatePlaylists { current ->
            current.map { playlist ->
                if (playlist.id == playlistId && !playlist.bookIds.contains(bookId)) {
                    playlist.copy(bookIds = playlist.bookIds + bookId)
                } else {
                    playlist
                }
            }
        }
    }

    fun createPlaylist(name: String, initialBookId: String?): Playlist {
        val sanitizedName = name.trim()
        require(sanitizedName.isNotEmpty()) { "Playlist name cannot be blank" }
        val playlist = Playlist(
            id = UUID.randomUUID().toString(),
            name = sanitizedName,
            bookIds = initialBookId?.let { listOf(it) } ?: emptyList()
        )
        updatePlaylists { it + playlist }
        return playlist
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        val sanitized = newName.trim()
        require(sanitized.isNotEmpty()) { "Playlist name cannot be blank" }
        updatePlaylists { current ->
            current.map { playlist ->
                if (playlist.id == playlistId) {
                    playlist.copy(name = sanitized)
                } else {
                    playlist
                }
            }
        }
    }

    fun deletePlaylist(playlistId: String) {
        updatePlaylists { current ->
            current.filterNot { it.id == playlistId }
        }
    }

    fun removeBookFromPlaylist(playlistId: String, bookId: String) {
        updatePlaylists { current ->
            current.map { playlist ->
                if (playlist.id == playlistId && playlist.bookIds.contains(bookId)) {
                    playlist.copy(bookIds = playlist.bookIds.filterNot { it == bookId })
                } else {
                    playlist
                }
            }
        }
    }

    private fun defaultPlaylists(): List<Playlist> = listOf(
        Playlist(id = "favorites", name = "Favorites"),
        Playlist(id = "morning", name = "Morning commute"),
        Playlist(id = "focus", name = "Focus session")
    )

    private fun loadPlaylistsForUser(userId: String): List<Playlist> =
        playlistsByUser.getOrPut(userId) { defaultPlaylists() }

    private fun updatePlaylists(transform: (List<Playlist>) -> List<Playlist>) {
        val updated = transform(loadPlaylistsForUser(activeUserId))
        playlistsByUser[activeUserId] = updated
        playlists.value = updated
    }

    private fun setActiveUser(userId: String) {
        if (userId == activeUserId) return
        activeUserId = userId
        playlists.value = loadPlaylistsForUser(userId)
    }
}
