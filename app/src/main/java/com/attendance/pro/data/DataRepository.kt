package com.attendance.pro.data

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
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

    // ---------------------------------------------------------------- Sheets
    fun newId(collection: String): String = db.collection(collection).document().id

    fun sheets(): Flow<List<SheetDef>> =
        db.collection("sheets").listen { d -> d.toObject(SheetDef::class.java)?.copy(id = d.id) }

    fun sheetMembers(): Flow<List<SheetMember>> =
        db.collection("sheet_members").listen { d -> d.toObject(SheetMember::class.java)?.copy(id = d.id) }

    fun sheetMarksForDate(date: String): Flow<List<SheetMark>> =
        db.collection("sheet_marks").whereEqualTo("date", date)
            .listen { d -> d.toObject(SheetMark::class.java)?.copy(id = d.id) }

    /** All marks of one month ("yyyy-MM"). Range filter on one field => no composite index needed. */
    fun sheetMarksForMonth(ym: String): Flow<List<SheetMark>> =
        db.collection("sheet_marks")
            .whereGreaterThanOrEqualTo("date", "$ym-01").whereLessThanOrEqualTo("date", "$ym-31")
            .listen { d -> d.toObject(SheetMark::class.java)?.copy(id = d.id) }

    // ---- editable grids (one doc per sheet per month)
    fun sheetGrid(docId: String): Flow<GridSnap> = callbackFlow<GridSnap> {
        val reg = db.collection("sheet_grids").document(docId).addSnapshotListener { s, e ->
            if (e != null) close(e)
            else trySend(GridSnap(docId, s?.toObject(SheetGrid::class.java), s?.metadata?.isFromCache ?: true, true))
        }
        awaitClose { reg.remove() }
    }.catch { emit(GridSnap(docId, null, true, true)) }

    /** Never throws: tries the server briefly, then the copy saved on the phone (works offline). */
    suspend fun getSheetGrid(docId: String): SheetGrid? {
        val ref = db.collection("sheet_grids").document(docId)
        val online = withTimeoutOrNull(3000) { runCatching { ref.get().await() }.getOrNull() }
        val snap = online ?: runCatching { ref.get(Source.CACHE).await() }.getOrNull()
        return snap?.toObject(SheetGrid::class.java)
    }

    suspend fun setSheetGrid(g: SheetGrid): Boolean =
        db.collection("sheet_grids").document(g.sheetId + "_" + g.ym).set(g).confirmed()

    suspend fun deleteGridsOfSheet(sheetId: String) {
        val snap = db.collection("sheet_grids").whereEqualTo("sheetId", sheetId).get().await()
        snap.documents.forEach { it.reference.delete() }
    }

    suspend fun setSheet(s: SheetDef): Boolean = db.collection("sheets").document(s.id).set(s).confirmed()
    suspend fun deleteSheet(id: String): Boolean = db.collection("sheets").document(id).delete().confirmed()

    suspend fun setSheetMember(m: SheetMember): Boolean =
        db.collection("sheet_members").document(m.id).set(m).confirmed()
    suspend fun deleteSheetMember(id: String): Boolean =
        db.collection("sheet_members").document(id).delete().confirmed()

    suspend fun setSheetMark(m: SheetMark): Boolean =
        db.collection("sheet_marks").document(m.id).set(m).confirmed()
    suspend fun deleteSheetMark(id: String): Boolean =
        db.collection("sheet_marks").document(id).delete().confirmed()

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
