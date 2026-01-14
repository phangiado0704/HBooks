package com.example.hbooks.data.repository

import com.example.hbooks.data.models.Book
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

private const val PROJECT_ID = "hbooks-b6c94"
private const val LEGACY_BUCKET_HOST = "$PROJECT_ID.appspot.com"
private const val CURRENT_BUCKET_HOST = "$PROJECT_ID.firebasestorage.app"
private const val LEGACY_COVER_PREFIX = "https://firebasestorage.googleapis.com/v0/b/$LEGACY_BUCKET_HOST/o/"

class BookRepository {
    private val db = FirebaseFirestore.getInstance()
    private val booksCollection = db.collection("books")

    suspend fun getBooks(): Result<List<Book>> = runCatching {
        booksCollection
            .get(Source.SERVER)
            .await()
            .toObjects(Book::class.java)
            .map { normalizeCoverUrl(it) }
    }

    suspend fun getBook(bookId: String): Result<Book?> = runCatching {
        booksCollection
            .document(bookId)
            .get(Source.SERVER)
            .await()
            .toObject(Book::class.java)
            ?.let { normalizeCoverUrl(it) }
    }

    /**
     * Upload a single book to Firestore.
     * 
     * @param id Unique book ID (e.g., "book001")
     * @param title Book title
     * @param author Author name
     * @param categories List of categories (e.g., listOf("Self-Help", "Productivity"))
     * 
     * Note: Cover image should be at: covers/{id}.jpg in Firebase Storage
     *       Audio file should be at: audios/{id}.mp3 in Firebase Storage
     */
    suspend fun uploadBook(
        id: String,
        title: String,
        author: String,
        categories: List<String>
    ): Result<Unit> = runCatching {
        val coverPath = "covers%2F${id}.jpg"
        val audioStorageUrl = "gs://$CURRENT_BUCKET_HOST/audios/${id}.mp3"
        val bookData = hashMapOf<String, Any>(
            "id" to id,
            "title" to title,
            "author" to author,
            "coverImageUrl" to "https://firebasestorage.googleapis.com/v0/b/$CURRENT_BUCKET_HOST/o/${coverPath}?alt=media",
            "audioUrl" to audioStorageUrl,
            "categories" to categories
        )
        booksCollection.document(id).set(bookData).await()
    }

    /**
     * Upload multiple books to Firestore.
     * 
     * @param books List of Book objects to upload
     */
    suspend fun uploadBooks(books: List<Book>): Result<Unit> = runCatching {
        for (book in books) {
            val coverPath = "covers%2F${book.id}.jpg"
            val audioStorageUrl = "gs://$CURRENT_BUCKET_HOST/audios/${book.id}.mp3"
            val bookData = hashMapOf<String, Any>(
                "id" to book.id,
                "title" to book.title,
                "author" to book.author,
                "coverImageUrl" to "https://firebasestorage.googleapis.com/v0/b/$CURRENT_BUCKET_HOST/o/${coverPath}?alt=media",
                "audioUrl" to audioStorageUrl,
                "categories" to book.categories
            )
            booksCollection.document(book.id).set(bookData).await()
        }
    }

    /**
     * Delete a book from Firestore.
     */
    suspend fun deleteBook(bookId: String): Result<Unit> = runCatching {
        booksCollection.document(bookId).delete().await()
    }

    private fun normalizeCoverUrl(book: Book): Book {
        val url = book.coverImageUrl
        if (url.isBlank()) return book
        val normalized = when {
            url.contains(LEGACY_BUCKET_HOST) -> url.replace(LEGACY_BUCKET_HOST, CURRENT_BUCKET_HOST)
            url.startsWith(LEGACY_COVER_PREFIX) -> url.replace(LEGACY_BUCKET_HOST, CURRENT_BUCKET_HOST)
            else -> url
        }
        return if (normalized == url) book else book.copy(coverImageUrl = normalized)
    }
}
