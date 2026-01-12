package com.example.hbooks.data.repository

import android.util.Log
import com.example.hbooks.data.models.Playlist
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object PlaylistRepository {

    private const val TAG = "PlaylistRepository"
    private const val ANONYMOUS_USER_ID = "anonymous"
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_PLAYLISTS = "playlists"

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var activeUserId = firebaseAuth.currentUser?.uid ?: ANONYMOUS_USER_ID

    private val playlistsByUser = mutableMapOf<String, List<Playlist>>()
    private val playlists = MutableStateFlow<List<Playlist>>(emptyList())

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        val newUserId = auth.currentUser?.uid ?: ANONYMOUS_USER_ID
        setActiveUser(newUserId)
    }

    init {
        firebaseAuth.addAuthStateListener(authListener)
        // Load from Firestore on init
        scope.launch {
            loadFromFirestore(activeUserId)
        }
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
        // Also delete from Firestore
        scope.launch {
            deletePlaylistFromFirestore(playlistId)
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

    private fun loadPlaylistsForUser(userId: String): List<Playlist> =
        playlistsByUser.getOrPut(userId) { emptyList() }

    private fun updatePlaylists(transform: (List<Playlist>) -> List<Playlist>) {
        val updated = transform(loadPlaylistsForUser(activeUserId))
        playlistsByUser[activeUserId] = updated
        playlists.value = updated

        // Persist to Firestore
        scope.launch {
            saveAllPlaylistsToFirestore(activeUserId, updated)
        }
    }

    private fun setActiveUser(userId: String) {
        if (userId == activeUserId) return
        activeUserId = userId
        // Load data for new user from Firestore
        scope.launch {
            loadFromFirestore(userId)
        }
    }

    private suspend fun loadFromFirestore(userId: String) {
        if (userId == ANONYMOUS_USER_ID) {
            playlists.value = loadPlaylistsForUser(userId)
            return
        }

        try {
            val snapshot = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_PLAYLISTS)
                .get()
                .await()

            val loadedPlaylists = snapshot.documents.mapNotNull { doc ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    Playlist(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        bookIds = (doc.get("bookIds") as? List<String>) ?: emptyList()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse playlist: ${doc.id}", e)
                    null
                }
            }

            playlistsByUser[userId] = loadedPlaylists
            if (userId == activeUserId) {
                playlists.value = loadedPlaylists
            }
            Log.d(TAG, "Loaded ${loadedPlaylists.size} playlists from Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load playlists from Firestore", e)
            playlists.value = loadPlaylistsForUser(userId)
        }
    }

    private suspend fun saveAllPlaylistsToFirestore(userId: String, playlistList: List<Playlist>) {
        if (userId == ANONYMOUS_USER_ID) return

        try {
            for (playlist in playlistList) {
                val data = mapOf(
                    "name" to playlist.name,
                    "bookIds" to playlist.bookIds,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_PLAYLISTS)
                    .document(playlist.id)
                    .set(data)
                    .await()
            }
            Log.d(TAG, "Saved ${playlistList.size} playlists to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save playlists to Firestore", e)
        }
    }

    private suspend fun deletePlaylistFromFirestore(playlistId: String) {
        if (activeUserId == ANONYMOUS_USER_ID) return

        try {
            firestore.collection(COLLECTION_USERS)
                .document(activeUserId)
                .collection(COLLECTION_PLAYLISTS)
                .document(playlistId)
                .delete()
                .await()
            Log.d(TAG, "Deleted playlist $playlistId from Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete playlist from Firestore", e)
        }
    }
}
