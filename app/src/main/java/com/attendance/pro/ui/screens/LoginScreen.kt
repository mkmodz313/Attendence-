package com.attendance.pro.ui.screens

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.attendance.pro.data.Role
import com.attendance.pro.ui.AuthState
import com.attendance.pro.ui.components.GlassCard
import com.attendance.pro.ui.components.GradientButton
import com.attendance.pro.ui.theme.*

@Composable
fun LoginScreen(
    state: AuthState.LoggedOut,
    onSignIn: (String, String, Role) -> Unit,
    onReset: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val role = Role.ADMIN
    var id by rememberSaveable { mutableStateOf("") }
    var pw by rememberSaveable { mutableStateOf("") }
    var show by rememberSaveable { mutableStateOf(false) }
    var resetOpen by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(BackgroundBrush).statusBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.AccessTime, null, tint = SilverBright, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(12.dp))
        Text("Admin Panel", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = SilverBright)
        Text("Password protected - admin only", color = Silver.copy(alpha = 0.7f))
        Spacer(Modifier.height(28.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = id, onValueChange = { id = it }, singleLine = true,
                label = { Text(if (role == Role.ADMIN) "Admin email / username" else "Worker email / username") },
                leadingIcon = { Icon(Icons.Filled.Person, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(), colors = fieldColors()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = pw, onValueChange = { pw = it }, singleLine = true, label = { Text("Password") },
                leadingIcon = { Icon(Icons.Filled.Lock, null) },
                trailingIcon = {
                    IconButton({ show = !show }) {
                        Icon(if (show) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Toggle password")
                    }
                },
                visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(), colors = fieldColors()
            )
            Text(
                "Forgot password?", color = Accent, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.End).padding(top = 8.dp).clickable { resetOpen = true }
            )
            state.error?.let { Text(it, color = LateRed, modifier = Modifier.padding(top = 10.dp)) }
            state.info?.let { Text(it, color = OkGreen, modifier = Modifier.padding(top = 10.dp)) }
            Spacer(Modifier.height(18.dp))
            GradientButton("SIGN IN", loading = state.busy) { onSignIn(id, pw, role) }
            TextButton(onBack, Modifier.align(Alignment.CenterHorizontally)) { Text("Back to Attendance", color = Silver) }
        }
    }

    if (resetOpen) {
        var email by remember { mutableStateOf(if ("@" in id) id else "") }
        AlertDialog(
            onDismissRequest = { resetOpen = false },
            title = { Text("Reset password") },
            text = {
                OutlinedTextField(email, { email = it }, singleLine = true, label = { Text("Email") }, colors = fieldColors())
            },
            confirmButton = { TextButton({ onReset(email); resetOpen = false }) { Text("Send link") } },
            dismissButton = { TextButton({ resetOpen = false }) { Text("Cancel") } },
            containerColor = Graphite
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Silver, unfocusedBorderColor = Slate,
    focusedLabelColor = Silver, unfocusedLabelColor = Silver.copy(alpha = 0.6f),
    cursorColor = Silver, focusedTextColor = SilverBright, unfocusedTextColor = SilverBright
)
