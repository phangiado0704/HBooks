package com.example.hbooks.ui.viewmodels

import com.example.hbooks.data.models.Book

data class BookListUiState(
    val isLoading: Boolean = false,
    val books: List<Book> = emptyList(),
    val filteredBooks: List<Book> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val errorMessage: String? = null
)
