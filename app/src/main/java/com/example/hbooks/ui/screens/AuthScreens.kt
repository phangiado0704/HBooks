package com.example.hbooks.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hbooks.R
import com.example.hbooks.ui.theme.HBooksFieldBorder
import com.example.hbooks.ui.theme.HBooksPink
import com.example.hbooks.ui.theme.HBooksPrimary
import com.example.hbooks.ui.theme.HBooksSoftPink
import com.example.hbooks.ui.theme.HBooksTextGray
import com.example.hbooks.ui.viewmodels.AuthUiState
import com.example.hbooks.ui.viewmodels.AuthViewModel

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onSignIn: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPassword: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    AuthScreenContainer(modifier = modifier) {
        AuthLogo()

        AuthHeader(
            title = "Welcome Back",
            subtitle = "Sign In to continue",
        )

        AuthTextField(
            value = uiState.email,
            onValueChange = { viewModel.updateEmail(it) },
            placeholder = "Email Address",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
        AuthTextField(
            value = uiState.password,
            onValueChange = { viewModel.updatePassword(it) },
            placeholder = "Password",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { viewModel.login(onSuccess = onSignIn) }),
            isPassword = true
        )
        AuthFeedback(uiState)
        ForgotPasswordLink(
            enabled = !uiState.isLoading,
            onClick = onForgotPassword
        )

        Spacer(modifier = Modifier.height(24.dp))
        AuthPrimaryButton(
            label = if (uiState.isLoading) "Signing In..." else "SIGN IN",
            onClick = { viewModel.login(onSuccess = onSignIn) },
            enabled = !uiState.isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))
        AuthBottomAction(
            text = "Don't have an account?",
            actionLabel = "Register",
            onActionClick = onRegisterClick
        )
    }
}

@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier,
    onRegister: () -> Unit,
    onSignInClick: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    AuthScreenContainer(modifier = modifier) {
        AuthLogo()

        AuthHeader(
            title = "Create an Account",
            subtitle = "Register to continue",
        )

        AuthTextField(
            value = uiState.name,
            onValueChange = { viewModel.updateName(it) },
            placeholder = "User Name",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        AuthTextField(
            value = uiState.email,
            onValueChange = { viewModel.updateEmail(it) },
            placeholder = "Email Address",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        AuthTextField(
            value = uiState.password,
            onValueChange = { viewModel.updatePassword(it) },
            placeholder = "Password",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            isPassword = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        AuthTextField(
            value = uiState.confirmPassword,
            onValueChange = { viewModel.updateConfirmPassword(it) },
            placeholder = "Retype password",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { viewModel.register(onSuccess = onRegister) }),
            isPassword = true
        )
        AuthFeedback(uiState)
        Spacer(modifier = Modifier.height(24.dp))
        AuthPrimaryButton(
            label = if (uiState.isLoading) "Registering..." else "REGISTER",
            onClick = { viewModel.register(onSuccess = onRegister) },
            enabled = !uiState.isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))
        AuthBottomAction(
            text = "Already have an account?",
            actionLabel = "Sign In",
            onActionClick = onSignInClick
        )
    }
}

@Composable
fun AuthScreenContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        FloatingTriangles()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp)
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun AuthLogo() {
    Image(
        painter = painterResource(id = R.drawable.audiobooks),
        contentDescription = "HBooks logo",
        modifier = Modifier
            .size(120.dp)
            .padding(top = 12.dp)
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "HBooks",
        color = HBooksPink,
        style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
    )
    Spacer(modifier = Modifier.height(28.dp))
}

@Composable
fun AuthHeader(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            color = Color(0xFF1F1F1F),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = HBooksTextGray,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp)
        )
    }
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isPassword: Boolean = false
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        textStyle = TextStyle(
            color = Color(0xFF2E2E2E),
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        cursorBrush = SolidColor(HBooksPrimary),
        decorationBox = { innerTextField ->
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = HBooksTextGray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    innerTextField()
                }
                HorizontalDivider(color = HBooksFieldBorder, thickness = 1.dp)
            }
        }
    )
}

@Composable
fun AuthPrimaryButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HBooksPrimary,
            contentColor = Color.White
        ),
        enabled = enabled,
        shape = RoundedCornerShape(32.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
        )
    }
}

@Composable
fun AuthBottomAction(
    text: String,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = Color(0xFF2B2B2B),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = actionLabel,
            color = HBooksPrimary,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.clickable { onActionClick() }
        )
    }
}

@Composable
private fun FloatingTriangles() {
    val triangleColor = HBooksSoftPink
    Box(modifier = Modifier.fillMaxSize()) {
        TriangleShape(
            modifier = Modifier
                .size(68.dp)
                .offset(x = 12.dp, y = 22.dp)
                .graphicsLayer(rotationZ = 10f),
            color = triangleColor
        )
        TriangleShape(
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.CenterStart)
                .offset(x = (-6).dp)
                .graphicsLayer(rotationZ = -8f),
            color = triangleColor
        )
        TriangleShape(
            modifier = Modifier
                .size(70.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 12.dp)
                .graphicsLayer(rotationZ = 8f),
            color = triangleColor
        )
        TriangleShape(
            modifier = Modifier
                .size(58.dp)
                .align(Alignment.TopEnd)
                .offset(x = (-10).dp, y = 36.dp)
                .graphicsLayer(rotationZ = -12f),
            color = triangleColor
        )
        TriangleShape(
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.BottomEnd)
                .offset(x = (-6).dp, y = (-16).dp)
                .graphicsLayer(rotationZ = 16f),
            color = triangleColor
        )
    }
}

@Composable
private fun TriangleShape(
    modifier: Modifier = Modifier,
    color: Color
) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width / 2f, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path = path, color = color)
    }
}

@Composable
private fun ForgotPasswordLink(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            text = "Forget Password?",
            color = if (enabled) HBooksPrimary else HBooksTextGray,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline
            ),
            modifier = Modifier.clickable(enabled = enabled) { onClick() }
        )
    }
}

@Composable
fun AuthFeedback(uiState: AuthUiState) {
    uiState.errorMessage?.let { message ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = Color(0xFFD32F2F),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )
    }
    uiState.successMessage?.let { message ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = Color(0xFF2E7D32),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )
    }
}
