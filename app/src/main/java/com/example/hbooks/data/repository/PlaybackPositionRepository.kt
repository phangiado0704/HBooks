package com.example.hbooks.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class PlaybackPosition(
    val bookId: String = "",
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

object PlaybackPositionRepository {

    private const val TAG = "PlaybackPositionRepo"
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_PLAYBACK_POSITIONS = "playbackPositions"
    private const val ANONYMOUS_USER_ID = "anonymous"

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private var activeUserId = firebaseAuth.currentUser?.uid ?: ANONYMOUS_USER_ID

    // In-memory cache of positions per user
    private val positionsByUser = mutableMapOf<String, MutableMap<String, PlaybackPosition>>()
    
    // Flow exposing all positions for the current user
    private val _positions = MutableStateFlow<Map<String, PlaybackPosition>>(emptyMap())
    val positions: StateFlow<Map<String, PlaybackPosition>> = _positions.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        val newUserId = auth.currentUser?.uid ?: ANONYMOUS_USER_ID
        switchUser(newUserId)
    }

    init {
        firebaseAuth.addAuthStateListener(authListener)
        _positions.value = getPositionsForUser(activeUserId)
    }

    /**
     * Save playback position for a book. Persists to Firestore for logged-in users.
     */
    suspend fun savePosition(bookId: String, positionMs: Long, durationMs: Long) {
        if (bookId.isBlank() || positionMs < 0) return

        val position = PlaybackPosition(
            bookId = bookId,
            positionMs = positionMs,
            durationMs = durationMs,
            updatedAt = System.currentTimeMillis()
        )

        // Update local cache
        val userPositions = positionsByUser.getOrPut(activeUserId) { mutableMapOf() }
        userPositions[bookId] = position
        _positions.value = userPositions.toMap()

        // Persist to Firestore for authenticated users
        if (activeUserId != ANONYMOUS_USER_ID) {
            try {
                firestore.collection(COLLECTION_USERS)
                    .document(activeUserId)
                    .collection(COLLECTION_PLAYBACK_POSITIONS)
                    .document(bookId)
                    .set(position)
                    .await()
                Log.d(TAG, "Saved position for $bookId: ${positionMs}ms")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save position to Firestore", e)
            }
        }
    }

    /**
     * Get saved position for a specific book.
     */
    fun getPosition(bookId: String): PlaybackPosition? {
        return getPositionsForUser(activeUserId)[bookId]
    }

    /**
     * Get saved position in milliseconds for a specific book, or 0 if none.
     */
    fun getPositionMs(bookId: String): Long {
        return getPosition(bookId)?.positionMs ?: 0L
    }

    /**
     * Load all positions from Firestore for the current user.
     */
    suspend fun loadFromFirestore() {
        if (activeUserId == ANONYMOUS_USER_ID) return

        try {
            val snapshot = firestore.collection(COLLECTION_USERS)
                .document(activeUserId)
                .collection(COLLECTION_PLAYBACK_POSITIONS)
                .get()
                .await()

            val userPositions = positionsByUser.getOrPut(activeUserId) { mutableMapOf() }
            for (doc in snapshot.documents) {
                val position = doc.toObject(PlaybackPosition::class.java)
                if (position != null) {
                    userPositions[position.bookId] = position
                }
            }
            _positions.value = userPositions.toMap()
            Log.d(TAG, "Loaded ${snapshot.size()} positions from Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load positions from Firestore", e)
        }
    }

    private fun getPositionsForUser(userId: String): Map<String, PlaybackPosition> {
        return positionsByUser.getOrPut(userId) { mutableMapOf() }
    }

    private fun switchUser(newUserId: String) {
        if (newUserId == activeUserId) return
        activeUserId = newUserId
        _positions.value = getPositionsForUser(newUserId)
    }
}
