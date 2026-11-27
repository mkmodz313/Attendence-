package com.attendance.pro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.pro.data.AuthRepository
import com.attendance.pro.data.Role
import com.attendance.pro.data.Session
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object Splash : AuthState
    data class LoggedOut(val error: String? = null, val busy: Boolean = false, val info: String? = null) : AuthState
    data class LoggedIn(val session: Session) : AuthState
}

class AuthViewModel(private val repo: AuthRepository = AuthRepository()) : ViewModel() {
    private val _state = MutableStateFlow<AuthState>(AuthState.Splash)
    val state: StateFlow<AuthState> = _state

    init {
        viewModelScope.launch {
            val started = System.currentTimeMillis()
            val session = repo.restoreSession()
            val remaining = 1400 - (System.currentTimeMillis() - started)
            if (remaining > 0) delay(remaining)
            _state.value = session?.let { AuthState.LoggedIn(it) } ?: AuthState.LoggedOut()
        }
    }

    /** [expected] is the tab the user chose; the real role still comes from the server. */
    fun signIn(identifier: String, password: String, expected: Role) {
        _state.value = AuthState.LoggedOut(busy = true)
        viewModelScope.launch {
            repo.signIn(identifier, password).fold(
                onSuccess = { s ->
                    if (s.role != expected) {
                        repo.signOut()
                        _state.value = AuthState.LoggedOut(
                            error = if (expected == Role.ADMIN) "This account is not an Admin account."
                            else "Admins must use the Admin tab."
                        )
                    } else _state.value = AuthState.LoggedIn(s)
                },
                onFailure = { _state.value = AuthState.LoggedOut(error = it.message) }
            )
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            repo.sendPasswordReset(email).fold(
                onSuccess = { _state.value = AuthState.LoggedOut(info = "Password reset email sent.") },
                onFailure = { _state.value = AuthState.LoggedOut(error = it.message) }
            )
        }
    }

    fun signOut() {
        repo.signOut()
        _state.value = AuthState.LoggedOut()
    }
}
