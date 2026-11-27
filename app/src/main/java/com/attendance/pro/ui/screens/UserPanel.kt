package com.attendance.pro.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.attendance.pro.ui.SheetsViewModel
import com.attendance.pro.ui.theme.*

/** Opens directly (no login). Shows only the sheets and names the admin created. The 3-dot menu leads to the Admin Panel. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPanel(vm: SheetsViewModel, onAdmin: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Attendance", fontWeight = FontWeight.Black) },
                actions = {
                    Box {
                        IconButton({ menu = true }) { Icon(Icons.Filled.MoreVert, "Menu") }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("Admin Panel") },
                                leadingIcon = { Icon(Icons.Filled.AdminPanelSettings, null) },
                                onClick = { menu = false; onAdmin() }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, titleContentColor = SilverBright, actionIconContentColor = Silver
                )
            )
        }
    ) { pad -> Box(Modifier.fillMaxSize().padding(pad)) { SheetsScreen(vm, editable = false) } }
}
