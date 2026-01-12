package com.example.hbooks.data.repository

import android.util.Log
import com.example.hbooks.data.models.Bookmark
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

object BookmarkRepository {

    private const val TAG = "BookmarkRepository"
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_BOOKMARKS = "bookmarks"
    private const val ANONYMOUS_USER_ID = "anonymous"

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private var activeUserId = firebaseAuth.currentUser?.uid ?: ANONYMOUS_USER_ID

    // In-memory cache of bookmarks per user, grouped by bookId
    private val bookmarksByUser = mutableMapOf<String, MutableMap<String, MutableList<Bookmark>>>()

    // Flow exposing bookmarks for the current book
    private val _currentBookBookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val currentBookBookmarks: StateFlow<List<Bookmark>> = _currentBookBookmarks.asStateFlow()

    private var currentBookId: String? = null

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        val newUserId = auth.currentUser?.uid ?: ANONYMOUS_USER_ID
        switchUser(newUserId)
    }

    init {
        firebaseAuth.addAuthStateListener(authListener)
    }

    /**
     * Set the current book to load bookmarks for.
     */
    suspend fun setCurrentBook(bookId: String) {
        currentBookId = bookId
        loadBookmarksForBook(bookId)
    }

    /**
     * Add a new bookmark at the current position.
     */
    suspend fun addBookmark(bookId: String, positionMs: Long, label: String = ""): Bookmark? {
        if (bookId.isBlank() || positionMs < 0) return null

        val bookmark = Bookmark(
            id = UUID.randomUUID().toString(),
            bookId = bookId,
            positionMs = positionMs,
            label = label.ifBlank { "Bookmark at ${formatTime(positionMs)}" },
            createdAt = System.currentTimeMillis()
        )

        // Update local cache
        val userBookmarks = bookmarksByUser.getOrPut(activeUserId) { mutableMapOf() }
        val bookBookmarks = userBookmarks.getOrPut(bookId) { mutableListOf() }
        bookBookmarks.add(bookmark)
        bookBookmarks.sortBy { it.positionMs }

        if (bookId == currentBookId) {
            _currentBookBookmarks.value = bookBookmarks.toList()
        }

        // Persist to Firestore for authenticated users
        if (activeUserId != ANONYMOUS_USER_ID) {
            try {
                firestore.collection(COLLECTION_USERS)
                    .document(activeUserId)
                    .collection(COLLECTION_BOOKMARKS)
                    .document(bookmark.id)
                    .set(bookmark)
                    .await()
                Log.d(TAG, "Saved bookmark for $bookId at ${positionMs}ms")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save bookmark to Firestore", e)
            }
        }

        return bookmark
    }

    /**
     * Delete a bookmark.
     */
    suspend fun deleteBookmark(bookmark: Bookmark) {
        // Update local cache
        val userBookmarks = bookmarksByUser[activeUserId] ?: return
        val bookBookmarks = userBookmarks[bookmark.bookId] ?: return
        bookBookmarks.removeAll { it.id == bookmark.id }

        if (bookmark.bookId == currentBookId) {
            _currentBookBookmarks.value = bookBookmarks.toList()
        }

        // Delete from Firestore
        if (activeUserId != ANONYMOUS_USER_ID) {
            try {
                firestore.collection(COLLECTION_USERS)
                    .document(activeUserId)
                    .collection(COLLECTION_BOOKMARKS)
                    .document(bookmark.id)
                    .delete()
                    .await()
                Log.d(TAG, "Deleted bookmark ${bookmark.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete bookmark from Firestore", e)
            }
        }
    }

    /**
     * Get all bookmarks for a specific book.
     */
    fun getBookmarksForBook(bookId: String): List<Bookmark> {
        val userBookmarks = bookmarksByUser[activeUserId] ?: return emptyList()
        return userBookmarks[bookId]?.toList() ?: emptyList()
    }

    /**
     * Load bookmarks from Firestore for a specific book.
     */
    private suspend fun loadBookmarksForBook(bookId: String) {
        val userBookmarks = bookmarksByUser.getOrPut(activeUserId) { mutableMapOf() }
        
        if (activeUserId != ANONYMOUS_USER_ID) {
            try {
                val snapshot = firestore.collection(COLLECTION_USERS)
                    .document(activeUserId)
                    .collection(COLLECTION_BOOKMARKS)
                    .whereEqualTo("bookId", bookId)
                    .orderBy("positionMs", Query.Direction.ASCENDING)
                    .get()
                    .await()

                val bookmarks = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Bookmark::class.java)
                }.toMutableList()

                userBookmarks[bookId] = bookmarks
                _currentBookBookmarks.value = bookmarks.toList()
                Log.d(TAG, "Loaded ${bookmarks.size} bookmarks for book $bookId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load bookmarks from Firestore", e)
                _currentBookBookmarks.value = userBookmarks[bookId]?.toList() ?: emptyList()
            }
        } else {
            _currentBookBookmarks.value = userBookmarks[bookId]?.toList() ?: emptyList()
        }
    }

    private fun switchUser(newUserId: String) {
        if (newUserId == activeUserId) return
        activeUserId = newUserId
        currentBookId?.let { bookId ->
            _currentBookBookmarks.value = getBookmarksForBook(bookId)
        }
    }

    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}
