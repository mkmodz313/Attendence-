package com.attendance.pro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.pro.data.DataRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/** Reads ONLY this worker's own records (the Firestore rules enforce the same on the server). */
class WorkerViewModel(uid: String, repo: DataRepository = DataRepository()) : ViewModel() {
    private val sharing = SharingStarted.WhileSubscribed(5000)
    val today: String = LocalDate.now().toString()
    val records = repo.attendanceForEmployee(uid)
        .map { list -> list.sortedByDescending { it.date } }
        .stateIn(viewModelScope, sharing, emptyList())
    val profile = repo.employee(uid).stateIn(viewModelScope, sharing, null)
}
