package com.attendance.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendance.pro.ui.AuthState
import com.attendance.pro.ui.AuthViewModel
import com.attendance.pro.ui.screens.HomeShell
import com.attendance.pro.ui.screens.LoginScreen
import com.attendance.pro.ui.screens.SplashScreen
import com.attendance.pro.ui.theme.AttendanceTheme

class MainActivity : ComponentActivity() {
    private val vm: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AttendanceTheme {
                val state by vm.state.collectAsStateWithLifecycle()
                when (val s = state) {
                    is AuthState.Splash -> SplashScreen()
                    is AuthState.LoggedOut -> LoginScreen(s, vm::signIn, vm::resetPassword)
                    is AuthState.LoggedIn -> HomeShell(s.session, vm::signOut)
                }
            }
        }
    }
}
