package com.example.hbooks.data.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    val currentUser get() = firebaseAuth.currentUser

    suspend fun login(email: String, password: String): Result<Unit> =
        awaitTask { firebaseAuth.signInWithEmailAndPassword(email, password) }

    suspend fun register(displayName: String, email: String, password: String): Result<Unit> {
        val createResult = awaitTask { firebaseAuth.createUserWithEmailAndPassword(email, password) }
        if (createResult.isFailure) return createResult
        if (displayName.isBlank()) return Result.success(Unit)
        return updateDisplayName(displayName)
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> =
        awaitTask { firebaseAuth.sendPasswordResetEmail(email) }

    suspend fun updateDisplayName(name: String): Result<Unit> {
        val user = currentUser ?: return Result.failure(IllegalStateException("No authenticated user."))
        return suspendCancellableCoroutine { continuation ->
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            val task = user.updateProfile(profileUpdates)
            task.addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Result.success(Unit))
            }.addOnFailureListener { error ->
                if (continuation.isActive) continuation.resume(Result.failure(error))
            }
        }
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> {
        val user = currentUser ?: return Result.failure(IllegalStateException("No authenticated user."))
        return awaitTask { user.updatePassword(newPassword) }
    }

    fun logout() {
        firebaseAuth.signOut()
    }

    private suspend fun awaitTask(block: () -> com.google.android.gms.tasks.Task<*>): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            val task = block()
            task.addOnSuccessListener {
                if (continuation.isActive) {
                    continuation.resume(Result.success(Unit))
                }
            }.addOnFailureListener { error ->
                if (continuation.isActive) {
                    continuation.resume(Result.failure(error))
                }
            }
        }
}
