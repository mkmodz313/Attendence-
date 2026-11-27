package com.attendance.pro.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class Employee(
    val uid: String = "",
    val employeeId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val department: String = "",
    val designation: String = "",
    val joiningDate: String = "",
    val active: Boolean = true,
    val deleted: Boolean = false
)

data class OfficeSettings(
    val officeName: String = "My Office",
    val startTime: String = "09:00",
    val endTime: String = "17:00",
    val graceMinutes: Int = 10
)

/** One document per employee per day. Doc id = "{employeeUid}_{yyyy-MM-dd}" => no duplicates possible. */
data class AttendanceRecord(
    val id: String = "",
    val employeeUid: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val date: String = "",
    val checkIn: String = "",
    val checkOut: String = "",
    val status: String = "ABSENT",
    val workMinutes: Int = 0,
    val lateMinutes: Int = 0,          // actual delay after office start (only when beyond grace)
    val lateBeyondGrace: Int = 0,      // minutes beyond the grace allowance
    val earlyLeaveMinutes: Int = 0,    // minutes left before closing time
    val earlyReason: String = "",      // reason for leaving early
    val remarks: String = "",
    val graceUsed: Int = 0,            // settings used at save time (history is preserved)
    val startUsed: String = ""
)

object Status {
    const val AUTO = "AUTO"
    const val PRESENT = "PRESENT"
    const val ABSENT = "ABSENT"
    const val LATE = "LATE"
    const val HALF_DAY = "HALF DAY"
    const val LEAVE = "LEAVE"
    const val HOLIDAY = "HOLIDAY"
    const val WEEKLY_OFF = "WEEKLY OFF"
    val options = listOf(AUTO, PRESENT, LATE, ABSENT, HALF_DAY, LEAVE, HOLIDAY, WEEKLY_OFF)
    val nonWorking = setOf(ABSENT, LEAVE, HOLIDAY, WEEKLY_OFF)
    val presentLike = setOf(PRESENT, LATE, HALF_DAY)
}

object AttendanceCalc {
    data class Result(
        val status: String, val workMinutes: Int, val lateMinutes: Int,
        val lateBeyondGrace: Int, val earlyLeaveMinutes: Int
    )

    fun toMinutes(t: String): Int? {
        val p = t.trim().split(":")
        if (p.size != 2) return null
        val h = p[0].toIntOrNull() ?: return null
        val m = p[1].toIntOrNull() ?: return null
        return if (h in 0..23 && m in 0..59) h * 60 + m else null
    }

    private fun isNightShift(s: OfficeSettings): Boolean =
        (toMinutes(s.endTime) ?: 1020) <= (toMinutes(s.startTime) ?: 540)

    fun compute(s: OfficeSettings, checkIn: String, checkOut: String, requested: String): Result {
        if (requested in Status.nonWorking) return Result(requested, 0, 0, 0, 0)
        val inM = toMinutes(checkIn)
        val outM = toMinutes(checkOut)
        val start = toMinutes(s.startTime) ?: 540
        var end = toMinutes(s.endTime) ?: 1020
        if (end <= start) end += 1440

        if (inM == null) { // missing check-in is never counted as late
            val st = if (requested in setOf(Status.AUTO, Status.PRESENT, Status.LATE)) Status.ABSENT else requested
            return Result(st, 0, 0, 0, 0)
        }
        val delay = maxOf(0, inM - start)
        val isLate = delay > s.graceMinutes
        var work = 0
        var early = 0
        if (outM != null) { // missing check-out => 0 hours, never a wrong total
            val effOut = if (outM < inM) outM + 1440 else outM
            work = effOut - inM
            early = maxOf(0, end - effOut)
        }
        val status = if (requested in setOf(Status.AUTO, Status.PRESENT, Status.LATE))
            (if (isLate) Status.LATE else Status.PRESENT) else requested
        return Result(status, work, if (isLate) delay else 0, if (isLate) delay - s.graceMinutes else 0, early)
    }

    /** Returns an error message, or null when the entry can be saved. */
    fun validate(s: OfficeSettings, checkIn: String, checkOut: String, requested: String, reason: String): String? {
        if (requested in Status.nonWorking) return null
        val inM = toMinutes(checkIn)
        val outM = toMinutes(checkOut)
        if (checkIn.isNotBlank() && inM == null) return "Invalid check-in time."
        if (checkOut.isNotBlank() && outM == null) return "Invalid check-out time."
        if (outM != null && inM == null) return "Enter check-in time first."
        if (inM != null && outM != null && outM < inM && !isNightShift(s)) return "Check-out is before check-in."
        val r = compute(s, checkIn, checkOut, requested)
        if (r.earlyLeaveMinutes > 0 && r.status in setOf(Status.PRESENT, Status.LATE) && reason.isBlank())
            return "Reason required: left ${r.earlyLeaveMinutes} min before closing time."
        return null
    }
}

fun fmtHours(min: Int): String = "${min / 60}h ${"%02d".format(min % 60)}m"

fun dayName(date: String): String = runCatching {
    LocalDate.parse(date).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
}.getOrDefault("")

fun prettyDate(date: String): String = runCatching {
    LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
}.getOrDefault(date)

// ------------------------------------------------------------------ Sheets (name-list attendance)
/** A sheet = one page (e.g. "Morning Batch") that belongs to a group (e.g. "Group A"). */
data class SheetDef(
    val id: String = "",
    val name: String = "",
    val groupName: String = "",
    val order: Long = 0L
)

/** A person added to a sheet just by typing the name. */
data class SheetMember(
    val id: String = "",
    val sheetId: String = "",
    val name: String = "",
    val order: Long = 0L
)

/** One mark per member per day. Doc id = "{memberId}_{yyyy-MM-dd}" => no duplicates possible. */
data class SheetMark(
    val id: String = "",
    val sheetId: String = "",
    val memberId: String = "",
    val date: String = "",
    val status: String = "",      // "" = normal day with times | LEAVE ("L") | ABSENT ("A") | legacy PRESENT / LATE
    val timeIn: String = "",      // top cell of the day column on the paper sheet
    val timeOut: String = ""      // bottom cell of the day column on the paper sheet
)

object SheetStatus {
    const val PRESENT = "PRESENT"
    const val ABSENT = "ABSENT"
    const val LATE = "LATE"
    const val LEAVE = "LEAVE"
}
