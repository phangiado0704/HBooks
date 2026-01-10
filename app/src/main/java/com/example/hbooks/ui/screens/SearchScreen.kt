package com.example.hbooks.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.hbooks.data.models.Book
import com.example.hbooks.ui.components.ErrorState
import com.example.hbooks.ui.components.LoadingState
import com.example.hbooks.ui.viewmodels.SearchViewModel

@Composable
fun SearchScreen(onBackClick: () -> Unit, onBookClick: (String) -> Unit) {
    val viewModel: SearchViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = uiState.query,
            onQueryChange = viewModel::onQueryChanged,
            onSearch = viewModel::onSearchAction,
            onBackClick = onBackClick
        )
        Spacer(modifier = Modifier.height(8.dp))

        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.fillMaxWidth())
            uiState.errorMessage != null -> ErrorState(
                message = uiState.errorMessage!!,
                onRetry = viewModel::refreshBooks,
                modifier = Modifier.fillMaxWidth()
            )
            else -> {
                if (uiState.recentQueries.isNotEmpty()) {
                    RecentSearchesSection(
                        recentQueries = uiState.recentQueries,
                        onRecentClick = viewModel::onRecentQuerySelected
                    )
                }
                SearchResults(
                    books = uiState.results,
                    onBookClick = onBookClick,
                    query = uiState.query
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onBackClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search books, authors, categories") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            colors = TextFieldDefaults.colors(
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun RecentSearchesSection(
    recentQueries: List<String>,
    onRecentClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Searches",
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        recentQueries.forEach { recent ->
            RecentSearchItem(
                text = recent,
                onClick = { onRecentClick(recent) }
            )
        }
    }
}

@Composable
private fun RecentSearchItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.History, contentDescription = "Recent search", tint = MaterialTheme.colorScheme.primary)
        Text(
            text = text,
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SearchResults(books: List<Book>, onBookClick: (String) -> Unit, query: String) {
    if (books.isEmpty()) {
        EmptySearchState(query = query)
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(books) { book ->
            SearchResultItem(book = book, onBookClick = onBookClick)
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun SearchResultItem(book: Book, onBookClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onBookClick(book.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = book.coverImageUrl,
                contentDescription = "Book cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .width(70.dp)
                    .height(90.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (book.categories.isNotEmpty()) {
                    Text(
                        text = book.categories.joinToString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySearchState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val message = if (query.isBlank()) {
            "Start typing to search across your library."
        } else {
            "No results found for \"$query\""
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
