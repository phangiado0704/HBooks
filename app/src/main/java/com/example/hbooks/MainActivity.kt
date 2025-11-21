package com.example.hbooks

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.hbooks.data.repository.BookRepository
import com.example.hbooks.ui.navigation.AppNavigation
import com.example.hbooks.ui.navigation.BottomNavigationBar
import com.example.hbooks.ui.theme.HBooksTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val bookRepository = BookRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        seedBooksIfNeeded()

        enableEdgeToEdge()
        setContent {
            HBooksTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val bottomBarRoutes = setOf("home", "search", "library", "profile")
                val showBottomBar = currentRoute in bottomBarRoutes

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            BottomNavigationBar(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    AppNavigation(navController = navController, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    private fun seedBooksIfNeeded() {
        lifecycleScope.launch {
            bookRepository.getBooks()
                .onSuccess { books ->
                    if (books.size < 6) {
                        val uploadResult = bookRepository.uploadInitialBooks()
                        if (uploadResult.isSuccess) {
                            Log.d(TAG, "Seeded initial catalog into Firestore.")
                        } else {
                            Log.e(TAG, "Unable to upload seeded books", uploadResult.exceptionOrNull())
                        }
                    } else {
                        Log.d(TAG, "Firestore already contains ${books.size} books. Skipping seed.")
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "Unable to check existing books", error)
                }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
