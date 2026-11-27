package com.attendance.pro.data

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * All data lives in Cloud Firestore (permanent, cloud-stored) and is cached on the phone
 * (Firestore offline persistence is on by default on Android), so screens update in real time
 * and writes made offline sync automatically later.
 */
class DataRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    /** true = confirmed by server, false = queued offline (will sync automatically). */
    private suspend fun <T> Task<T>.confirmed(): Boolean = withTimeoutOrNull(6000) { await(); true } ?: false

    private fun <T : Any> Query.listen(map: (DocumentSnapshot) -> T?): Flow<List<T>> = callbackFlow<List<T>> {
        val reg = addSnapshotListener { snap, err ->
            if (err != null) close(err) else trySend(snap?.documents?.mapNotNull(map).orEmpty())
        }
        awaitClose { reg.remove() }
    }.catch { emit(emptyList()) }

    fun employees(): Flow<List<Employee>> =
        db.collection("employees").listen { d -> d.toObject(Employee::class.java)?.copy(uid = d.id) }

    fun employee(uid: String): Flow<Employee?> = callbackFlow<Employee?> {
        val reg = db.collection("employees").document(uid).addSnapshotListener { s, e ->
            if (e != null) close(e) else trySend(s?.toObject(Employee::class.java)?.copy(uid = uid))
        }
        awaitClose { reg.remove() }
    }.catch { emit(null) }

    fun settings(): Flow<OfficeSettings> = callbackFlow<OfficeSettings> {
        val reg = db.collection("office_settings").document("main").addSnapshotListener { s, e ->
            if (e != null) close(e) else trySend(s?.toObject(OfficeSettings::class.java) ?: OfficeSettings())
        }
        awaitClose { reg.remove() }
    }.catch { }

    fun attendanceForDate(date: String): Flow<List<AttendanceRecord>> =
        db.collection("attendance").whereEqualTo("date", date)
            .listen { d -> d.toObject(AttendanceRecord::class.java)?.copy(id = d.id) }

    fun attendanceForEmployee(uid: String): Flow<List<AttendanceRecord>> =
        db.collection("attendance").whereEqualTo("employeeUid", uid)
            .listen { d -> d.toObject(AttendanceRecord::class.java)?.copy(id = d.id) }

    suspend fun setEmployee(e: Employee): Boolean =
        db.collection("employees").document(e.uid).set(e).confirmed()

    suspend fun setAttendance(r: AttendanceRecord): Boolean =
        db.collection("attendance").document(r.id).set(r).confirmed()

    suspend fun setSettings(s: OfficeSettings): Boolean =
        db.collection("office_settings").document("main").set(s).confirmed()

    /**
     * Creates the worker's login WITHOUT signing the admin out, by using a second
     * FirebaseApp instance just for account creation.
     */
    suspend fun createWorkerAccount(email: String, password: String): String {
        val primary = FirebaseApp.getInstance()
        val secondary = runCatching { FirebaseApp.getInstance("worker-creator") }.getOrNull()
            ?: FirebaseApp.initializeApp(primary.applicationContext, primary.options, "worker-creator")!!
        val auth = FirebaseAuth.getInstance(secondary)
        val uid = auth.createUserWithEmailAndPassword(email, password).await().user?.uid
            ?: error("Could not create the worker account.")
        auth.signOut()
        return uid
    }
}
