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
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class SheetsViewModel(private val repo: DataRepository = DataRepository()) : ViewModel() {
    private val sharing = SharingStarted.WhileSubscribed(5000)

    val sheets = repo.sheets().map { l -> l.sortedBy { it.order } }.stateIn(viewModelScope, sharing, emptyList())
    val members = repo.sheetMembers().map { l -> l.sortedBy { it.order } }.stateIn(viewModelScope, sharing, emptyList())

    val date = MutableStateFlow(LocalDate.now().toString())
    /** Every mark of the selected month (used for the per-name monthly summary). */
    val monthMarks = date.map { it.take(7) }.distinctUntilChanged()
        .flatMapLatest { repo.sheetMarksForMonth(it) }.stateIn(viewModelScope, sharing, emptyList())
    /** Marks of the selected day only. */
    val marks = combine(date, monthMarks) { d, l -> l.filter { it.date == d } }
        .stateIn(viewModelScope, sharing, emptyList())

    // ---- editable grid of the open sheet for the selected month
    private val activeSheet = MutableStateFlow<String?>(null)
    val gridSnap: StateFlow<GridSnap> =
        combine(activeSheet, date.map { it.take(7) }.distinctUntilChanged()) { s, ym -> s to ym }
            .flatMapLatest { (s, ym) -> if (s == null) flowOf(GridSnap()) else repo.sheetGrid("${s}_$ym") }
            .stateIn(viewModelScope, sharing, GridSnap())
    private val undoStack = ArrayList<SheetGrid>()
    var canUndo by mutableStateOf(false)

    fun setActiveSheet(id: String?) { activeSheet.value = id; undoStack.clear(); canUndo = false }

    /** Every edit (cell text, colours, rows, columns ...) goes through here, so Undo works for all of them. */
    fun editGrid(f: (SheetGrid) -> SheetGrid) {
        val g = gridSnap.value.grid ?: return
        val n = f(g)
        if (n == g) return
        undoStack.add(g); if (undoStack.size > 30) undoStack.removeAt(0)
        canUndo = true
        launchSafely { repo.setSheetGrid(n) }
    }

    fun undo() {
        val cur = gridSnap.value.grid ?: return
        val i = undoStack.indexOfLast { it.sheetId == cur.sheetId && it.ym == cur.ym }
        if (i < 0) return
        val prev = undoStack.removeAt(i)
        canUndo = undoStack.isNotEmpty()
        launchSafely { repo.setSheetGrid(prev) }
    }

    /** Creates the blank form for the selected month; names are carried over from the previous month. */
    fun createMonth(sheetId: String) {
        val ym = YearMonth.parse(date.value.take(7))
        launchSafely {
            val prev = repo.getSheetGrid("${sheetId}_${ym.minusMonths(1)}")
            val names = prev?.let { GridOps.namesOf(it) }.orEmpty()
            repo.setSheetGrid(GridOps.newMonth(sheetId, ym, names))
        }
    }

    var message by mutableStateOf<String?>(null)

    fun shiftDate(days: Long) { date.value = LocalDate.parse(date.value).plusDays(days).toString() }
    fun setDate(d: String) { date.value = d }
    fun shiftMonth(n: Long) { date.value = LocalDate.parse(date.value).plusMonths(n).toString() }
    fun consumeMessage() { message = null }

    fun addSheet(name: String, group: String) {
        val n = name.trim(); val g = group.trim()
        if (n.isEmpty()) { message = "Enter the sheet name."; return }
        launchSafely {
            repo.setSheet(SheetDef(id = repo.newId("sheets"), name = n, groupName = g, order = System.currentTimeMillis()))
        }
    }

    fun updateSheet(s: SheetDef, name: String, group: String) {
        val n = name.trim()
        if (n.isEmpty()) { message = "Enter the sheet name."; return }
        launchSafely { repo.setSheet(s.copy(name = n, groupName = group.trim())) }
    }

    /** Deletes the sheet first (so it always disappears), then cleans up its names and month tables. */
    fun deleteSheet(s: SheetDef) = launchSafely {
        val sheetMembers = members.value.filter { it.sheetId == s.id }
        repo.deleteSheet(s.id)
        message = "Sheet deleted"
        runCatching { repo.deleteGridsOfSheet(s.id) }
        sheetMembers.forEach { m -> runCatching { repo.deleteSheetMember(m.id) } }
    }

    fun addMember(sheetId: String, name: String): Boolean {
        val n = name.trim().replace(Regex("\\s+"), " ")
        if (n.isEmpty()) return false
        if (members.value.any { it.sheetId == sheetId && it.name.equals(n, ignoreCase = true) }) {
            message = "\"$n\" is already in this sheet."
            return false
        }
        launchSafely {
            repo.setSheetMember(SheetMember(id = repo.newId("sheet_members"), sheetId = sheetId, name = n, order = System.currentTimeMillis()))
        }
        return true
    }

    fun deleteMember(m: SheetMember) = launchSafely {
        repo.deleteSheetMember(m.id)
        message = "${m.name} removed"
    }

    /** Tap a status to set it; tap the same status again to clear it. */
    fun mark(m: SheetMember, status: String) {
        val d = date.value
        val id = "${m.id}_$d"
        val existing = marks.value.firstOrNull { it.id == id }
        launchSafely {
            if (existing?.status == status) repo.deleteSheetMark(id)
            else repo.setSheetMark(SheetMark(id = id, sheetId = m.sheetId, memberId = m.id, date = d, status = status))
        }
    }

    /** Writes one day-cell of the paper sheet: time in (top), time out (bottom) or an L / A letter. */
    fun setCell(m: SheetMember, d: String, timeIn: String, timeOut: String, status: String) {
        val id = "${m.id}_$d"
        launchSafely {
            if (timeIn.isBlank() && timeOut.isBlank() && status.isBlank()) repo.deleteSheetMark(id)
            else repo.setSheetMark(
                SheetMark(id = id, sheetId = m.sheetId, memberId = m.id, date = d,
                    status = status, timeIn = timeIn, timeOut = timeOut)
            )
        }
    }

    fun markAll(sheetId: String, status: String) {
        val d = date.value
        val list = members.value.filter { it.sheetId == sheetId }
        launchSafely {
            list.forEach { m ->
                repo.setSheetMark(SheetMark(id = "${m.id}_$d", sheetId = sheetId, memberId = m.id, date = d, status = status))
            }
        }
    }

    private fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() } catch (e: Exception) {
                val m = e.message.orEmpty()
                message = if (m.contains("PERMISSION_DENIED", true) || m.contains("permission", true))
                    "Permission denied. Publish the updated firestore.rules and sign in as admin."
                else m.ifBlank { "Something went wrong." }
            }
        }
    }
}
