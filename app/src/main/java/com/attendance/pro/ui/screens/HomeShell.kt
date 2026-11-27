package com.attendance.pro.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.attendance.pro.ui.SheetsViewModel
import com.attendance.pro.ui.theme.*

/** Password-protected admin area: create sheets, add / remove names, mark attendance. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanel(vm: SheetsViewModel, onExit: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) { vm.message?.let { snackbar.showSnackbar(it); vm.consumeMessage() } }
    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel", fontWeight = FontWeight.Black) },
                actions = { IconButton(onExit) { Icon(Icons.AutoMirrored.Filled.Logout, "Exit admin") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, titleContentColor = SilverBright, actionIconContentColor = Silver
                )
            )
        }
    ) { pad -> Box(Modifier.fillMaxSize().padding(pad)) { SheetsScreen(vm, editable = true) } }
}
