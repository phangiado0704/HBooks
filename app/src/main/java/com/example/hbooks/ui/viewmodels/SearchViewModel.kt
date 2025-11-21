package com.example.hbooks.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hbooks.data.models.Book
import com.example.hbooks.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<Book> = emptyList(),
    val recentQueries: List<String> = emptyList(),
    val errorMessage: String? = null
)

class SearchViewModel(
    private val bookRepository: BookRepository = BookRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private var cachedBooks: List<Book> = emptyList()

    init {
        refreshBooks()
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query, errorMessage = null) }
        filterBooks(query)
    }

    fun onSearchAction() {
        addRecentQuery(_uiState.value.query)
        filterBooks()
    }

    fun onRecentQuerySelected(query: String) {
        _uiState.update { it.copy(query = query, errorMessage = null) }
        addRecentQuery(query)
        filterBooks(query)
    }

    fun refreshBooks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            bookRepository.getBooks()
                .onSuccess { books ->
                    cachedBooks = books
                    val filtered = filterBooks(_uiState.value.query, emitState = false)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            results = filtered,
                            errorMessage = null
                        )
                    }
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

    private fun filterBooks(query: String = _uiState.value.query, emitState: Boolean = true): List<Book> {
        val normalizedQuery = query.trim()
        val filtered = if (normalizedQuery.isEmpty()) {
            cachedBooks
        } else {
            cachedBooks.filter { book ->
                book.title.contains(normalizedQuery, ignoreCase = true) ||
                    book.author.contains(normalizedQuery, ignoreCase = true) ||
                    book.categories.any { it.contains(normalizedQuery, ignoreCase = true) }
            }
        }

        if (emitState) {
            _uiState.update { it.copy(results = filtered) }
        }
        return filtered
    }

    private fun addRecentQuery(query: String) {
        val normalized = query.trim()
        if (normalized.isEmpty()) return
        _uiState.update { state ->
            val updated = listOf(normalized) + state.recentQueries.filterNot { it.equals(normalized, ignoreCase = true) }
            state.copy(recentQueries = updated.take(5))
        }
    }
}
