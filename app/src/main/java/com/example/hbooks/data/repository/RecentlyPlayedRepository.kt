package com.example.hbooks.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object RecentlyPlayedRepository {

    private const val TAG = "RecentlyPlayedRepo"
    private const val RECENT_LIMIT = 5
    private const val ANONYMOUS_USER_ID = "anonymous"
    private const val COLLECTION_USERS = "users"
    private const val DOCUMENT_RECENTLY_PLAYED = "recentlyPlayed"
    private const val FIELD_BOOK_IDS = "bookIds"
    private const val FIELD_UPDATED_AT = "updatedAt"

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var activeUserId = firebaseAuth.currentUser?.uid ?: ANONYMOUS_USER_ID
    private val recentlyPlayedByUser = mutableMapOf<String, List<String>>()

    private val recentlyPlayed = MutableStateFlow<List<String>>(emptyList())

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        val newUserId = auth.currentUser?.uid ?: ANONYMOUS_USER_ID
        switchUser(newUserId)
    }

    init {
        firebaseAuth.addAuthStateListener(authListener)
        // Load from Firestore on init
        scope.launch {
            loadFromFirestore(activeUserId)
        }
    }

    fun recentlyPlayedFlow(): StateFlow<List<String>> = recentlyPlayed.asStateFlow()

    fun markPlayed(bookId: String) {
        if (bookId.isBlank()) return
        val updated = listOf(bookId) + currentListForUser(activeUserId).filterNot { it == bookId }
        val trimmed = updated.take(RECENT_LIMIT)
        recentlyPlayedByUser[activeUserId] = trimmed
        recentlyPlayed.value = trimmed

        // Persist to Firestore
        scope.launch {
            saveToFirestore(activeUserId, trimmed)
        }
    }

    private fun currentListForUser(userId: String): List<String> =
        recentlyPlayedByUser.getOrPut(userId) { emptyList() }

    private fun switchUser(newUserId: String) {
        if (newUserId == activeUserId) return
        activeUserId = newUserId
        // Load data for new user from Firestore
        scope.launch {
            loadFromFirestore(newUserId)
        }
    }

    private suspend fun loadFromFirestore(userId: String) {
        if (userId == ANONYMOUS_USER_ID) {
            recentlyPlayed.value = currentListForUser(userId)
            return
        }

        try {
            val doc = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection("userData")
                .document(DOCUMENT_RECENTLY_PLAYED)
                .get()
                .await()

            if (doc.exists()) {
                @Suppress("UNCHECKED_CAST")
                val bookIds = doc.get(FIELD_BOOK_IDS) as? List<String> ?: emptyList()
                recentlyPlayedByUser[userId] = bookIds
                if (userId == activeUserId) {
                    recentlyPlayed.value = bookIds
                }
                Log.d(TAG, "Loaded ${bookIds.size} recently played books from Firestore")
            } else {
                recentlyPlayed.value = emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load recently played from Firestore", e)
            recentlyPlayed.value = currentListForUser(userId)
        }
    }

    private suspend fun saveToFirestore(userId: String, bookIds: List<String>) {
        if (userId == ANONYMOUS_USER_ID) return

        try {
            val data = mapOf(
                FIELD_BOOK_IDS to bookIds,
                FIELD_UPDATED_AT to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection("userData")
                .document(DOCUMENT_RECENTLY_PLAYED)
                .set(data)
                .await()
            Log.d(TAG, "Saved ${bookIds.size} recently played books to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save recently played to Firestore", e)
        }
    }
}
