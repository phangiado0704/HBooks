package com.example.hbooks.util

import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

/**
 * Helper that turns a Firebase Storage `gs://` URL into a downloadable HTTPS URL.
 * The resolved URLs are cached in-memory so we do not request the token repeatedly.
 */
class FirebaseStorageFetcher(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    private val downloadUrlCache = ConcurrentHashMap<String, String>()

    suspend fun getDownloadUrl(storageUrl: String): String {
        if (!storageUrl.startsWith("gs://")) {
            return storageUrl
        }

        return downloadUrlCache[storageUrl]
            ?: fetchDownloadUrl(storageUrl).also { downloadUrlCache[storageUrl] = it }
    }

    private suspend fun fetchDownloadUrl(storageUrl: String): String {
        val reference = storage.getReferenceFromUrl(storageUrl)
        return reference.downloadUrl.await().toString()
    }
}
