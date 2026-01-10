package com.example.hbooks.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hbooks.ui.viewmodels.AuthViewModel

@Composable
fun ForgotPasswordScreen(
    modifier: Modifier = Modifier,
    onEmailSent: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    AuthScreenContainer(modifier = modifier) {
        AuthLogo()

        AuthHeader(
            title = "Reset Password",
            subtitle = "Enter your email to receive a password reset link.",
        )

        AuthTextField(
            value = uiState.email,
            onValueChange = { viewModel.updateEmail(it) },
            placeholder = "Email Address",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { viewModel.resetPassword(onSuccess = onEmailSent) })
        )
        AuthFeedback(uiState)
        Spacer(modifier = Modifier.height(24.dp))
        AuthPrimaryButton(
            label = if (uiState.isLoading) "Sending..." else "SEND EMAIL",
            onClick = { viewModel.resetPassword(onSuccess = onEmailSent) },
            enabled = !uiState.isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))
        AuthBottomAction(
            text = "Remembered your password?",
            actionLabel = "Sign In",
            onActionClick = onBackClick
        )
    }
}
