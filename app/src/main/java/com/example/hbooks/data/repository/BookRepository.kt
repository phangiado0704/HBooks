package com.example.hbooks.data.repository

import com.example.hbooks.data.models.Book
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

private const val PROJECT_ID = "hbooks-b6c94"
private const val LEGACY_BUCKET_HOST = "$PROJECT_ID.appspot.com"
private const val CURRENT_BUCKET_HOST = "$PROJECT_ID.firebasestorage.app"
private const val LEGACY_COVER_PREFIX = "https://firebasestorage.googleapis.com/v0/b/$LEGACY_BUCKET_HOST/o/"

private data class SeedBook(
    val id: String,
    val title: String,
    val author: String,
    val categories: List<String>
)

private fun seedBooks(): List<SeedBook> = listOf(
    SeedBook(
        id = "book001",
        title = "Atomic Habits",
        author = "James Clear",
        categories = listOf("Self-Help", "Productivity")
    ),
    SeedBook(
        id = "book002",
        title = "Thinking, Fast and Slow",
        author = "Daniel Kahneman",
        categories = listOf("Psychology", "Behavioral Science")
    ),
    SeedBook(
        id = "book003",
        title = "Same as Ever",
        author = "Morgan Housel",
        categories = listOf("Finance", "Self-Help")
    ),
    SeedBook(
        id = "book004",
        title = "Deep Work",
        author = "Cal Newport",
        categories = listOf("Productivity", "Career")
    ),
    SeedBook(
        id = "book005",
        title = "12 Rules for Life",
        author = "Jordan B. Peterson",
        categories = listOf("Self-Help", "Philosophy")
    ),
    SeedBook(
        id = "book006",
        title = "Do It Today",
        author = "Darius Foroux",
        categories = listOf("Self-Help", "Productivity")
    ),
    SeedBook(
        id = "book007",
        title = "How to Fail at Almost Everything and Still Win Big",
        author = "Scott Adams",
        categories = listOf("Motivation", "Entrepreneurship")
    ),
    SeedBook(
        id = "book008",
        title = "Buy Back Your Time",
        author = "Dan Martell",
        categories = listOf("Entrepreneurship", "Productivity")
    ),
    SeedBook(
        id = "book009",
        title = "How to Finish Everything You Start",
        author = "Jan Yager",
        categories = listOf("Productivity", "Self-Help")
    ),
    SeedBook(
        id = "book010",
        title = "How to Stop Worrying and Start Living",
        author = "Dale Carnegie",
        categories = listOf("Self-Help", "Mental Health")
    ),
    SeedBook(
        id = "book011",
        title = "Flow: The Psychology of Optimal Experience",
        author = "Mihaly Csikszentmihalyi",
        categories = listOf("Psychology", "Self-Help")
    ),
    SeedBook(
        id = "book012",
        title = "Can't Hurt Me",
        author = "David Goggins",
        categories = listOf("Memoir", "Motivation")
    ),
    SeedBook(
        id = "book013",
        title = "Don't Trust Your Gut",
        author = "Seth Stephens-Davidowitz",
        categories = listOf("Data Science", "Psychology")
    ),
    SeedBook(
        id = "book014",
        title = "Living 365 Days a Year",
        author = "Nguyen Hien Le",
        categories = listOf("Lifestyle", "Inspiration")
    ),
    SeedBook(
        id = "book015",
        title = "Hyperfocus",
        author = "Chris Bailey",
        categories = listOf("Productivity", "Mindfulness")
    ),
    SeedBook(
        id = "book016",
        title = "Beyond Order",
        author = "Jordan B. Peterson",
        categories = listOf("Philosophy", "Self-Help")
    ),
    SeedBook(
        id = "book017",
        title = "Hidden Potential",
        author = "Adam Grant",
        categories = listOf("Psychology", "Business")
    ),
    SeedBook(
        id = "book018",
        title = "Do Hard Things",
        author = "Steve Magness",
        categories = listOf("Performance", "Motivation")
    ),
    SeedBook(
        id = "book019",
        title = "Feel-Good Productivity",
        author = "Ali Abdaal",
        categories = listOf("Productivity", "Wellness")
    ),
    SeedBook(
        id = "book020",
        title = "Manifest",
        author = "Roxie Nafousi",
        categories = listOf("Self-Help", "Spirituality")
    ),
    SeedBook(
        id = "book021",
        title = "Spark",
        author = "John J. Ratey",
        categories = listOf("Science", "Fitness")
    ),
    SeedBook(
        id = "book022",
        title = "Building a Second Brain",
        author = "Tiago Forte",
        categories = listOf("Productivity", "Technology")
    ),
    SeedBook(
        id = "book023",
        title = "Body by Science",
        author = "Doug McGuff & John Little",
        categories = listOf("Fitness", "Science")
    ),
    SeedBook(
        id = "book024",
        title = "Organizing Family Affairs",
        author = "Nguyen Hien Le",
        categories = listOf("Family", "Lifestyle")
    ),
    SeedBook(
        id = "book025",
        title = "Deep Nutrition",
        author = "Catherine Shanahan",
        categories = listOf("Nutrition", "Science")
    ),
    SeedBook(
        id = "book026",
        title = "Bullshit Jobs",
        author = "David Graeber",
        categories = listOf("Economics", "Sociology")
    ),
    SeedBook(
        id = "book027",
        title = "The Science of Sleep",
        author = "Heather Darwall-Smith",
        categories = listOf("Health", "Science")
    ),
    SeedBook(
        id = "book028",
        title = "The Urban Monk",
        author = "Pedram Shojai",
        categories = listOf("Mindfulness", "Spirituality")
    ),
    SeedBook(
        id = "book029",
        title = "The High 5 Habit",
        author = "Mel Robbins",
        categories = listOf("Motivation", "Self-Help")
    ),
    SeedBook(
        id = "book030",
        title = "The Secret Pulse of Time",
        author = "Stefan Klein",
        categories = listOf("Science", "Philosophy")
    ),
    SeedBook(
        id = "book031",
        title = "Stop Overthinking",
        author = "Nick Trenton",
        categories = listOf("Mental Health", "Self-Help")
    ),
    SeedBook(
        id = "book032",
        title = "The Let Them Theory",
        author = "Mel Robbins",
        categories = listOf("Relationships", "Self-Help")
    ),
    SeedBook(
        id = "book033",
        title = "Stolen Focus",
        author = "Johann Hari",
        categories = listOf("Productivity", "Culture")
    ),
    SeedBook(
        id = "book034",
        title = "The Courage to Be Disliked",
        author = "Ichiro Kishimi & Fumitake Koga",
        categories = listOf("Philosophy", "Psychology")
    ),
    SeedBook(
        id = "book035",
        title = "Nonviolent Communication",
        author = "Marshall Rosenberg",
        categories = listOf("Communication", "Relationships")
    ),
    SeedBook(
        id = "book036",
        title = "Neuro Habits",
        author = "Peter Hollins",
        categories = listOf("Neuroscience", "Self-Help")
    )
)

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

    suspend fun uploadInitialBooks(): Result<Unit> = runCatching {
        val booksToUpload = seedBooks()

        for (book in booksToUpload) {
            val coverPath = "covers%2F${book.id}.jpg"
            val audioStorageUrl = "gs://$CURRENT_BUCKET_HOST/audios/${book.id}.mp3"
            val fullBookData = hashMapOf<String, Any>(
                "id" to book.id,
                "title" to book.title,
                "author" to book.author,
                "coverImageUrl" to "https://firebasestorage.googleapis.com/v0/b/$CURRENT_BUCKET_HOST/o/${coverPath}?alt=media",
                "audioUrl" to audioStorageUrl,
                "categories" to book.categories
            )

            booksCollection.document(book.id).set(fullBookData).await()
        }
    }

    suspend fun updateBookCategories(): Result<Unit> = runCatching {
        seedBooks().forEach { book ->
            booksCollection.document(book.id)
                .update("categories", book.categories)
                .await()
        }
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
