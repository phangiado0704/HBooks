package com.example.hbooks.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hbooks.data.models.Book
import com.example.hbooks.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val bookRepository: BookRepository = BookRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookListUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        refreshBooks()
    }

    fun refreshBooks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val previousSelection = _uiState.value.selectedCategory
            bookRepository.getBooks()
                .onSuccess { rawBooks ->
                    val books = ensureCategoriesPresent(rawBooks)
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

    private suspend fun ensureCategoriesPresent(books: List<Book>): List<Book> {
        if (books.none { it.categories.isEmpty() }) return books

        val updateResult = bookRepository.updateBookCategories()
        if (updateResult.isFailure) {
            Log.e(TAG, "Unable to update missing categories", updateResult.exceptionOrNull())
            return books
        }

        val refreshedResult = bookRepository.getBooks()
        if (refreshedResult.isFailure) {
            Log.e(TAG, "Unable to reload books after category update", refreshedResult.exceptionOrNull())
            return books
        }
        return refreshedResult.getOrThrow()
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

    companion object {
        private const val TAG = "HomeViewModel"
    }
}
