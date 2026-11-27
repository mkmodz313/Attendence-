package com.attendance.pro.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.pro.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModel(private val repo: DataRepository = DataRepository()) : ViewModel() {
    private val sharing = SharingStarted.WhileSubscribed(5000)

    /** Includes soft-deleted employees (UI filters them) so employee IDs are never reused. */
    val allEmployees = repo.employees().stateIn(viewModelScope, sharing, emptyList())
    val settings = repo.settings().stateIn(viewModelScope, sharing, null)

    val today: String = LocalDate.now().toString()
    val todayRecords = repo.attendanceForDate(today).stateIn(viewModelScope, sharing, emptyList())

    val date = MutableStateFlow(today)
    val records = date.flatMapLatest { repo.attendanceForDate(it) }.stateIn(viewModelScope, sharing, emptyList())

    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)

    fun shiftDate(days: Long) { date.value = LocalDate.parse(date.value).plusDays(days).toString() }
    fun setDate(d: String) { date.value = d }

    fun saveEmployee(existing: Employee?, form: Employee, loginId: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            busy = true
            try {
                val synced: Boolean
                if (existing == null) {
                    val id = loginId.trim()
                    val email = if ("@" in id) id else "${id.lowercase()}@attendance.app"
                    val uid = repo.createWorkerAccount(email, password)
                    val n = (allEmployees.value.mapNotNull { it.employeeId.removePrefix("EMP-").toIntOrNull() }
                        .maxOrNull() ?: 0) + 1
                    synced = repo.setEmployee(
                        form.copy(uid = uid, employeeId = "EMP-%04d".format(n), email = email, active = true, deleted = false)
                    )
                } else {
                    synced = repo.setEmployee(
                        form.copy(uid = existing.uid, employeeId = existing.employeeId, email = existing.email,
                            active = existing.active, deleted = false)
                    )
                }
                message = if (synced) "Employee saved" else "Saved offline - will sync when online"
                onSuccess()
            } catch (e: Exception) {
                message = friendly(e)
            } finally { busy = false }
        }
    }

    fun setActive(e: Employee, active: Boolean) = writeSafely {
        repo.setEmployee(e.copy(active = active))
        message = if (active) "${e.fullName} activated" else "${e.fullName} deactivated"
    }

    /** Soft delete: login is blocked, but all attendance history is kept. */
    fun deleteEmployee(e: Employee) = writeSafely {
        repo.setEmployee(e.copy(active = false, deleted = true))
        message = "${e.fullName} deleted (attendance history kept)"
    }

    fun saveSettings(s: OfficeSettings) = writeSafely {
        val ok = repo.setSettings(s)
        message = if (ok) "Settings saved" else "Saved offline - will sync when online"
    }

    fun saveAttendance(
        emp: Employee, date: String, checkIn: String, checkOut: String,
        requested: String, remarks: String, reason: String, onSuccess: () -> Unit
    ) {
        val s = settings.value ?: run { message = "Office settings are still loading. Try again."; return }
        AttendanceCalc.validate(s, checkIn, checkOut, requested, reason)?.let { message = it; return }
        val r = AttendanceCalc.compute(s, checkIn, checkOut, requested)
        val rec = AttendanceRecord(
            id = "${emp.uid}_$date", employeeUid = emp.uid, employeeId = emp.employeeId,
            employeeName = emp.fullName, date = date, checkIn = checkIn, checkOut = checkOut,
            status = r.status, workMinutes = r.workMinutes, lateMinutes = r.lateMinutes,
            lateBeyondGrace = r.lateBeyondGrace, earlyLeaveMinutes = r.earlyLeaveMinutes,
            earlyReason = if (r.earlyLeaveMinutes > 0) reason.trim() else "",
            remarks = remarks.trim(), graceUsed = s.graceMinutes, startUsed = s.startTime
        )
        writeSafely {
            val ok = repo.setAttendance(rec)
            message = if (ok) "Attendance saved" else "Saved offline - will sync when online"
            onSuccess()
        }
    }

    fun consumeMessage() { message = null }

    private fun writeSafely(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy = true
            try { block() } catch (e: Exception) { message = friendly(e) } finally { busy = false }
        }
    }

    private fun friendly(e: Exception): String {
        val m = e.message.orEmpty()
        return when {
            m.contains("already in use", true) -> "This login is already used by another account."
            m.contains("badly formatted", true) -> "Invalid email / username."
            m.contains("PERMISSION_DENIED", true) || m.contains("permission", true) ->
                "Permission denied. Check that the Firestore rules are published and this account is an admin."
            m.contains("network", true) -> "No internet connection."
            m.contains("password", true) -> "Password must be at least 6 characters."
            else -> m.ifBlank { "Something went wrong." }
        }
    }
}
