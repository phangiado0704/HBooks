package com.example.hbooks.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hbooks.data.repository.BookRepository
import com.example.hbooks.data.repository.PlaylistRepository
import com.example.hbooks.data.repository.RecentlyPlayedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookRepository: BookRepository
) : ViewModel() {

    private val playlistRepository = PlaylistRepository
    private val recentlyPlayedRepository = RecentlyPlayedRepository

    private val _uiState = MutableStateFlow(BookListUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()
    val playlists = playlistRepository.playlistsFlow()
    val recentlyPlayed = recentlyPlayedRepository.recentlyPlayedFlow()

    init {
        refreshBooks()
    }

    fun refreshBooks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            bookRepository.getBooks()
                .onSuccess { books ->
                    _uiState.value = BookListUiState(books = books)
                }
                .onFailure { error ->
                    _uiState.value = BookListUiState(
                        errorMessage = error.localizedMessage ?: "Unable to load books"
                    )
                }
        }
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        playlistRepository.renamePlaylist(playlistId, newName)
    }

    fun deletePlaylist(playlistId: String) {
        playlistRepository.deletePlaylist(playlistId)
    }

    fun removeBookFromPlaylist(playlistId: String, bookId: String) {
        playlistRepository.removeBookFromPlaylist(playlistId, bookId)
    }
}
