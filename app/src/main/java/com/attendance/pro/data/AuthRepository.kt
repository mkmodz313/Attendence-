package com.attendance.pro.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.tasks.await

enum class Role { ADMIN, WORKER }

data class Session(val uid: String, val role: Role, val name: String)

/**
 * Role is ALWAYS resolved from Firestore (admins/{uid} or employees/{uid}),
 * never from anything the client supplies. Firestore rules enforce the same split server-side.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun restoreSession(): Session? {
        val user = auth.currentUser ?: return null
        val server = runCatching { withTimeout(4000) { resolve(user.uid, user.email, Source.DEFAULT) } }
        // The server answered: either access is confirmed, or it is really gone => sign out.
        if (server.isSuccess) return server.getOrNull() ?: run { auth.signOut(); null }
        // Offline / slow connection: trust the role saved on the phone so the admin can keep working.
        return runCatching { resolve(user.uid, user.email, Source.CACHE) }.getOrNull()
    }

    suspend fun signIn(identifier: String, password: String): Result<Session> = runCatching {
        val id = identifier.trim()
        require(id.isNotEmpty() && password.isNotEmpty()) { "Enter your email/username and password." }
        val email = if ("@" in id) id else "${id.lowercase()}@attendance.app"
        val user = auth.signInWithEmailAndPassword(email, password).await().user
            ?: error("Sign-in failed.")
        resolve(user.uid, email, Source.DEFAULT) ?: run {
            auth.signOut()
            error("This account has no access (deactivated or not registered).")
        }
    }.recoverCatching { throw Exception(friendly(it)) }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        require(email.contains("@")) { "Enter a valid email address." }
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }.recoverCatching { throw Exception(friendly(it)) }

    fun signOut() = auth.signOut()

    private suspend fun resolve(uid: String, email: String?, source: Source): Session? {
        val admin = db.collection("admins").document(uid).get(source).await()
        if (admin.exists()) {
            return Session(uid, Role.ADMIN, admin.getString("name") ?: email ?: "Admin")
        }
        val emp = db.collection("employees").document(uid).get(source).await()
        if (emp.exists() && emp.getBoolean("active") != false) {
            return Session(uid, Role.WORKER, emp.getString("fullName") ?: email ?: "Employee")
        }
        return null
    }

    private fun friendly(t: Throwable): String {
        val m = t.message.orEmpty()
        return when {
            t is IllegalArgumentException || t is IllegalStateException -> m.ifBlank { "Invalid input." }
            m.contains("network", true) -> "No internet connection. Please try again."
            m.contains("password", true) || m.contains("credential", true) ||
                m.contains("no user", true) -> "Incorrect email/username or password."
            else -> m.ifBlank { "Something went wrong." }
        }
    }
}
