package com.example.hbooks.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hbooks.data.models.Book
import com.example.hbooks.data.repository.BookRepository
import com.example.hbooks.data.repository.PlaybackPosition
import com.example.hbooks.data.repository.PlaybackPositionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookListUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    val playbackPositions: StateFlow<Map<String, PlaybackPosition>> = PlaybackPositionRepository.positions

    init {
        refreshBooks()
        // Load positions from Firestore
        viewModelScope.launch {
            PlaybackPositionRepository.loadFromFirestore()
        }
    }

    fun refreshBooks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val previousSelection = _uiState.value.selectedCategory
            bookRepository.getBooks()
                .onSuccess { books ->
                    val categories = extractCategories(books)
                    val activeSelection = previousSelection?.takeIf { selection ->
                        categories.any { it.equals(selection, ignoreCase = true) }
                    }
                    val filteredBooks = filterBooks(books, activeSelection)
                    _uiState.value = BookListUiState(
                        isLoading = false,
                        books = books,
                        filteredBooks = filteredBooks,
                        categories = categories,
                        selectedCategory = activeSelection
                    )
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Unable to load books"
                        )
                    }
                }
        }
    }

    fun onCategorySelected(category: String?) {
        _uiState.update { state ->
            val normalizedSelection = when {
                category.isNullOrBlank() -> null
                state.selectedCategory == category -> null
                else -> category
            }
            val filteredBooks = filterBooks(state.books, normalizedSelection)
            state.copy(
                filteredBooks = filteredBooks,
                selectedCategory = normalizedSelection,
                errorMessage = null
            )
        }
    }

    private fun filterBooks(books: List<Book>, category: String?): List<Book> {
        if (category.isNullOrBlank()) return books
        return books.filter { book ->
            book.categories.any { it.equals(category, ignoreCase = true) }
        }
    }

    private fun extractCategories(books: List<Book>): List<String> {
        return books
            .flatMap { it.categories }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }
}
