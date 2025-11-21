package com.example.hbooks.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hbooks.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value, errorMessage = null, successMessage = null) }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null, successMessage = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null, successMessage = null) }
    }

    fun updateConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null, successMessage = null) }
    }

    fun login(onSuccess: () -> Unit) {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        if (!validateEmail(email) || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email and password.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = authRepository.login(email = email, password = password)
            _uiState.update { current ->
                if (result.isSuccess) {
                    onSuccess()
                    current.copy(isLoading = false, successMessage = "Signed in successfully.")
                } else {
                    current.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Unable to sign in."
                    )
                }
            }
        }
    }

    fun register(onSuccess: () -> Unit) {
        val name = _uiState.value.name
        val trimmedName = name.trim()
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        val confirm = _uiState.value.confirmPassword
        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your name.") }
            return
        }
        if (!validateEmail(email)) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address.") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
            return
        }
        if (password != confirm) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = authRepository.register(displayName = name, email = email, password = password)
            _uiState.update { current ->
                if (result.isSuccess) {
                    onSuccess()
                    current.copy(isLoading = false, successMessage = "Account created.")
                } else {
                    current.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Unable to register."
                    )
                }
            }
        }
    }

    fun resetPassword(onSuccess: () -> Unit = {}) {
        val email = _uiState.value.email.trim()
        if (!validateEmail(email)) {
            _uiState.update { it.copy(errorMessage = "Enter a valid email to reset your password.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = authRepository.sendPasswordReset(email = email)
            _uiState.update { current ->
                if (result.isSuccess) {
                    onSuccess()
                    current.copy(
                        isLoading = false,
                        successMessage = "Reset email sent. Check your inbox."
                    )
                } else {
                    current.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Unable to send reset email."
                    )
                }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private fun validateEmail(email: String): Boolean =
        email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
