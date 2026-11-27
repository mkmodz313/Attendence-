package com.attendance.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendance.pro.data.Role
import com.attendance.pro.ui.AuthState
import com.attendance.pro.ui.AuthViewModel
import com.attendance.pro.ui.SheetsViewModel
import com.attendance.pro.ui.screens.AdminPanel
import com.attendance.pro.ui.screens.LoginScreen
import com.attendance.pro.ui.screens.SplashScreen
import com.attendance.pro.ui.screens.UserPanel
import com.attendance.pro.ui.theme.AttendanceTheme
import com.attendance.pro.ui.theme.auroraBackground
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private val vm: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Admin access is always password protected: every app start begins signed out.
        if (savedInstanceState == null) runCatching { FirebaseAuth.getInstance().signOut() }
        enableEdgeToEdge()
        setContent {
            AttendanceTheme {
                val state by vm.state.collectAsStateWithLifecycle()
                if (state is AuthState.Splash) SplashScreen() else AppRoot(vm)
            }
        }
    }
}

@Composable
private fun AppRoot(authVm: AuthViewModel) {
    val sheetsVm: SheetsViewModel = viewModel()
    var adminMode by rememberSaveable { mutableStateOf(false) }
    val state by authVm.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().auroraBackground()) {
        if (!adminMode) {
            UserPanel(sheetsVm) { adminMode = true }
        } else {
            when (val s = state) {
                is AuthState.Splash -> Unit
                is AuthState.LoggedOut -> {
                    BackHandler { adminMode = false }
                    LoginScreen(s, authVm::signIn, authVm::resetPassword, onBack = { adminMode = false })
                }
                is AuthState.LoggedIn -> {
                    if (s.session.role == Role.ADMIN) {
                        BackHandler { authVm.signOut(); adminMode = false }
                        AdminPanel(sheetsVm) { authVm.signOut(); adminMode = false }
                    } else {
                        LaunchedEffect(s) { authVm.signOut() }
                    }
                }
            }
        }
    }
}
