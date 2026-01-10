package com.example.hbooks.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hbooks.data.models.Book
import com.example.hbooks.ui.components.BookItem
import com.example.hbooks.ui.components.ErrorState
import com.example.hbooks.ui.components.LoadingState
import com.example.hbooks.ui.components.SectionTitle
import com.example.hbooks.ui.viewmodels.HomeViewModel

@Composable
fun HomeScreen(onBookClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val viewModel: HomeViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(modifier = modifier) {
        item {
            when {
                uiState.isLoading -> LoadingState(modifier = Modifier.fillMaxWidth())
                uiState.errorMessage != null -> ErrorState(
                    message = uiState.errorMessage!!,
                    onRetry = viewModel::refreshBooks,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (!uiState.isLoading && uiState.errorMessage == null) {
            if (uiState.categories.isNotEmpty()) {
                item {
                    SectionTitle(title = "Category")
                    CategorySection(
                        categories = uiState.categories,
                        selectedCategory = uiState.selectedCategory,
                        onCategorySelected = viewModel::onCategorySelected
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            val displayedBooks = uiState.filteredBooks
            if (displayedBooks.isEmpty()) {
                item {
                    EmptyCategoryState(
                        selectedCategory = uiState.selectedCategory,
                        onReset = viewModel::onCategorySelected,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                item {
                    SectionTitle(title = "New Releases Book")
                    NewReleasesSection(books = displayedBooks, onBookClick = onBookClick)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    SectionTitle(title = "Featured Books")
                }
                item {
                    FeaturedBooksSection(
                        books = displayedBooks,
                        onBookClick = onBookClick,
                        modifier = Modifier.height(400.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NewReleasesSection(books: List<Book>, onBookClick: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(books) { book ->
            BookItem(
                book = book,
                onBookClick = onBookClick,
                modifier = Modifier.width(120.dp),
                cardElevation = 4.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
fun CategorySection(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            CategoryChip(
                text = "All",
                isSelected = selectedCategory == null,
                onClick = { onCategorySelected(null) }
            )
        }
        items(categories) { category ->
            CategoryChip(
                text = category,
                isSelected = selectedCategory.equals(category, ignoreCase = true),
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
fun CategoryChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = if (isSelected) primaryColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
    val borderColor = if (isSelected) primaryColor else Color.LightGray.copy(alpha = 0.5f)
    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, borderColor),
        onClick = onClick
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun FeaturedBooksSection(books: List<Book>, onBookClick: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(books.reversed()) { book ->
            BookItem(book = book, onBookClick = onBookClick, cardElevation = 4.dp)
        }
    }
}

@Composable
fun EmptyCategoryState(
    selectedCategory: String?,
    onReset: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val message = if (selectedCategory.isNullOrBlank()) {
        "No books available yet. Pull to refresh or check back soon."
    } else {
        "No books found in \"$selectedCategory\". Try another category."
    }
    Column(
        modifier = modifier
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        if (!selectedCategory.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Clear filter",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onReset(null) }
            )
        }
    }
}
