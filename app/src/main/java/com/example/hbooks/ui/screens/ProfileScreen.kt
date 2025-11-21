package com.example.hbooks.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hbooks.R
import com.example.hbooks.ui.theme.HBooksPrimary
import com.example.hbooks.ui.theme.HBooksSoftPink
import com.example.hbooks.ui.theme.HBooksTextGray
import com.example.hbooks.ui.viewmodels.ProfileUiState
import com.example.hbooks.ui.viewmodels.ProfileViewModel

@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showChangePassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(bottom = 16.dp)
    ) {
        ProfileHeader(
            name = uiState.displayName,
            onBackClick = onBackClick,
            onEditName = { viewModel.startEditingName() }
        )
        ProfileInfoCard(
            uiState = uiState,
            onNameChange = { viewModel.updateNameInput(it) },
            onEditName = { viewModel.startEditingName() },
            onSaveName = { viewModel.saveDisplayName() },
            onChangePassword = { showChangePassword = true }
        )
        LogoutButton(onLogout = { viewModel.logout(onLogout) })
    }

    if (showChangePassword) {
        ChangePasswordDialog(
            onDismiss = { showChangePassword = false },
            onConfirm = { newPass ->
                viewModel.changePassword(newPass) { showChangePassword = false }
            }
        )
    }
}

@Composable
private fun ProfileHeader(
    name: String,
    onBackClick: () -> Unit,
    onEditName: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = HBooksPrimary,
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
            )
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            .padding(top = 16.dp, bottom = 24.dp),
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = painterResource(id = R.drawable.user_image_default),
                contentDescription = "User avatar",
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = name.ifEmpty { "User" },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(6.dp))
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit name",
                    tint = Color.White,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onEditName)
                )
            }
        }
    }
}

@Composable
private fun ProfileInfoCard(
    uiState: ProfileUiState,
    onNameChange: (String) -> Unit,
    onEditName: () -> Unit,
    onSaveName: () -> Unit,
    onChangePassword: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        EditableNameField(
            name = uiState.nameInput.ifEmpty { uiState.displayName },
            isEditing = uiState.isEditingName,
            onNameChange = onNameChange,
            onEditStart = onEditName,
            onSave = onSaveName
        )
        Spacer(modifier = Modifier.height(12.dp))
        InfoRow(label = "Email", value = uiState.email)
        Spacer(modifier = Modifier.height(12.dp))
        ActionRow(
            label = "Change Password",
            onClick = onChangePassword
        )
        FeedbackRow(uiState)
    }
}

@Composable
private fun EditableNameField(
    name: String,
    isEditing: Boolean,
    onNameChange: (String) -> Unit,
    onEditStart: () -> Unit,
    onSave: () -> Unit
) {
    if (isEditing) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("User Name") },
            trailingIcon = {
                TextButton(onClick = onSave) {
                    Text("Save", color = HBooksPrimary)
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
    } else {
        InfoRow(label = "User Name", value = name, editable = true, onEdit = onEditStart)
    }
}

@Composable
private fun InfoRow(label: String, value: String, editable: Boolean = false, onEdit: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = HBooksPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                color = Color(0xFF5B5B5B),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End
            )
            if (editable && onEdit != null) {
                Spacer(modifier = Modifier.size(8.dp))
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = HBooksPrimary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onEdit)
                )
            }
        }
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = HBooksPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Icon(
            imageVector = Icons.Default.ArrowForwardIos,
            contentDescription = label,
            tint = Color(0xFF707070),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun FeedbackRow(uiState: ProfileUiState) {
    uiState.errorMessage?.let { msg ->
        Text(
            text = msg,
            color = Color(0xFFD32F2F),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
    uiState.successMessage?.let { msg ->
        Text(
            text = msg,
            color = Color(0xFF2E7D32),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(password) }) {
                Text("Change", color = HBooksPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = HBooksTextGray)
            }
        },
        title = { Text("Change Password", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Enter a new password (min 6 characters).", color = HBooksTextGray)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    label = { Text("New Password") }
                )
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun LogoutButton(onLogout: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        TextButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .background(HBooksSoftPink, RoundedCornerShape(40.dp))
        ) {
            Text(
                text = "Log out",
                color = HBooksPrimary,
                modifier = Modifier.padding(vertical = 8.dp),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
