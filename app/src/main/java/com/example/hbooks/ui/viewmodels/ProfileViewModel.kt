package com.example.hbooks.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hbooks.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val displayName: String = "Listener",
    val email: String = "",
    val isEditingName: Boolean = false,
    val nameInput: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        val user = authRepository.currentUser
        _uiState.update {
            it.copy(
                displayName = user?.displayName ?: "User",
                email = user?.email.orEmpty(),
                nameInput = user?.displayName.orEmpty()
            )
        }
    }

    fun startEditingName() {
        _uiState.update { it.copy(isEditingName = true, nameInput = it.displayName) }
    }

    fun updateNameInput(value: String) {
        _uiState.update { it.copy(nameInput = value, errorMessage = null, successMessage = null) }
    }

    fun saveDisplayName(onComplete: () -> Unit = {}) {
        val newName = _uiState.value.nameInput.trim()
        if (newName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Name cannot be empty.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
            val result = authRepository.updateDisplayName(newName)
            _uiState.update { current ->
                if (result.isSuccess) {
                    onComplete()
                    current.copy(
                        displayName = newName,
                        isEditingName = false,
                        isSubmitting = false,
                        successMessage = "Name updated."
                    )
                } else {
                    current.copy(
                        isSubmitting = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Unable to update name."
                    )
                }
            }
        }
    }

    fun changePassword(newPassword: String, onComplete: () -> Unit = {}) {
        if (newPassword.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
            val result = authRepository.updatePassword(newPassword)
            _uiState.update { current ->
                if (result.isSuccess) {
                    onComplete()
                    current.copy(isSubmitting = false, successMessage = "Password updated.")
                } else {
                    current.copy(
                        isSubmitting = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Unable to change password."
                    )
                }
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        authRepository.logout()
        onComplete()
    }

    fun dismissMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
