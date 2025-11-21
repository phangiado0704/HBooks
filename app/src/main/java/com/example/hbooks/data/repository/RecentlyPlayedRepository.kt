package com.example.hbooks.data.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RecentlyPlayedRepository {

    private const val RECENT_LIMIT = 5
    private const val ANONYMOUS_USER_ID = "anonymous"

    private val firebaseAuth = FirebaseAuth.getInstance()
    private var activeUserId = firebaseAuth.currentUser?.uid ?: ANONYMOUS_USER_ID
    private val recentlyPlayedByUser = mutableMapOf<String, List<String>>()

    private val recentlyPlayed = MutableStateFlow<List<String>>(emptyList())

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        val newUserId = auth.currentUser?.uid ?: ANONYMOUS_USER_ID
        switchUser(newUserId)
    }

    init {
        firebaseAuth.addAuthStateListener(authListener)
        recentlyPlayed.value = currentListForUser(activeUserId)
    }

    fun recentlyPlayedFlow(): StateFlow<List<String>> = recentlyPlayed.asStateFlow()

    fun markPlayed(bookId: String) {
        if (bookId.isBlank()) return
        val updated = listOf(bookId) + currentListForUser(activeUserId).filterNot { it == bookId }
        val trimmed = updated.take(RECENT_LIMIT)
        recentlyPlayedByUser[activeUserId] = trimmed
        recentlyPlayed.value = trimmed
    }

    private fun currentListForUser(userId: String): List<String> =
        recentlyPlayedByUser.getOrPut(userId) { emptyList() }

    private fun switchUser(newUserId: String) {
        if (newUserId == activeUserId) return
        activeUserId = newUserId
        recentlyPlayed.value = currentListForUser(newUserId)
    }
}
