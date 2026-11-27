package com.attendance.pro.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.attendance.pro.data.Role
import com.attendance.pro.data.Session
import com.attendance.pro.ui.AdminViewModel
import com.attendance.pro.ui.WorkerViewModel
import com.attendance.pro.ui.components.GlassCard
import com.attendance.pro.ui.theme.*

private data class Tab(val label: String, val icon: ImageVector)

private val adminTabs = listOf(
    Tab("Dashboard", Icons.Filled.Dashboard), Tab("Employees", Icons.Filled.People),
    Tab("Attendance", Icons.Filled.EventAvailable), Tab("Reports", Icons.Filled.Assessment),
    Tab("Settings", Icons.Filled.Settings)
)
private val workerTabs = listOf(
    Tab("Home", Icons.Filled.Home), Tab("My Attendance", Icons.Filled.EventAvailable),
    Tab("Monthly", Icons.Filled.CalendarMonth), Tab("Late", Icons.Filled.History),
    Tab("Profile", Icons.Filled.Person)
)

@Composable
fun HomeShell(session: Session, onLogout: () -> Unit) {
    if (session.role == Role.ADMIN) AdminHost(session, onLogout) else WorkerHost(session, onLogout)
}

@Composable
private fun AdminHost(session: Session, onLogout: () -> Unit) {
    val vm: AdminViewModel = viewModel(key = "admin-${session.uid}")
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) { vm.message?.let { snackbar.showSnackbar(it); vm.consumeMessage() } }
    ShellScaffold(adminTabs, snackbar, onLogout) { tab ->
        when (tab) {
            0 -> AdminDashboard(vm)
            1 -> EmployeesScreen(vm)
            2 -> AttendanceScreen(vm)
            3 -> Box(Modifier.padding(20.dp)) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("Reports", fontWeight = FontWeight.Bold, color = SilverBright)
                    Text("Monthly and yearly late reports, charts and PDF/Excel export arrive in the next phase.", color = Silver)
                }
            }
            else -> AdminSettingsScreen(vm)
        }
    }
}

@Composable
private fun WorkerHost(session: Session, onLogout: () -> Unit) {
    val vm: WorkerViewModel = viewModel(
        key = "worker-${session.uid}",
        factory = viewModelFactory { initializer { WorkerViewModel(session.uid) } }
    )
    ShellScaffold(workerTabs, remember { SnackbarHostState() }, onLogout) { tab ->
        when (tab) {
            0 -> WorkerHome(vm, session.name)
            1 -> WorkerAttendance(vm)
            2 -> WorkerMonthly(vm)
            3 -> WorkerLate(vm)
            else -> WorkerProfile(vm)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShellScaffold(
    tabs: List<Tab>, snackbar: SnackbarHostState, onLogout: () -> Unit, content: @Composable (Int) -> Unit
) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = index != 0) { index = 0 }
    Scaffold(
        containerColor = Black,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(tabs[index].label, fontWeight = FontWeight.Bold) },
                actions = { IconButton(onLogout) { Icon(Icons.AutoMirrored.Filled.Logout, "Logout") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Charcoal, titleContentColor = SilverBright, actionIconContentColor = Silver
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Charcoal) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = index == i, onClick = { index = i },
                        icon = { Icon(t.icon, t.label) },
                        label = { Text(t.label, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Black, selectedTextColor = SilverBright,
                            indicatorColor = Silver, unselectedIconColor = Silver, unselectedTextColor = Silver
                        )
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().background(BackgroundBrush).padding(pad)) { content(index) }
    }
}
