package com.example.hbooks

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.hbooks.data.repository.BookRepository
import com.google.firebase.FirebaseApp
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation helper that can be triggered to seed Firestore with the full list of books.
 *
 * Run with:
 * ./gradlew connectedAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.example.hbooks.UploadSeedBooksInstrumentedTest \
 *   -Pandroid.testInstrumentationRunnerArguments.seedBooks=true
 *
 * Remove this file after seeding to avoid accidental re-runs.
 */
@RunWith(AndroidJUnit4::class)
class UploadSeedBooksInstrumentedTest {

    @Test
    fun uploadSeedBooks() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        val shouldSeed = args.getString("seedBooks")?.toBoolean() == true
        assumeTrue("Pass -Pandroid.testInstrumentationRunnerArguments.seedBooks=true to run", shouldSeed)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        val result = BookRepository().uploadInitialBooks()
        assertTrue(result.isSuccess, result.exceptionOrNull()?.message ?: "Seeding failed")
    }
}
