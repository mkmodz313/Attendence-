package com.attendance.pro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendance.pro.data.*
import com.attendance.pro.ui.WorkerViewModel
import com.attendance.pro.ui.components.*
import com.attendance.pro.ui.theme.*

@Composable
private fun RecordCard(r: AttendanceRecord) {
    GlassCard(Modifier.fillMaxWidth(), accent = if (r.status == Status.LATE) LateRed else null) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${prettyDate(r.date)}  (${dayName(r.date)})", fontWeight = FontWeight.Bold, color = SilverBright)
            StatusBadge(r.status, statusColor(r.status))
        }
        Spacer(Modifier.height(6.dp))
        Text("IN ${r.checkIn.ifBlank { "--:--" }}   OUT ${r.checkOut.ifBlank { "--:--" }}   ${fmtHours(r.workMinutes)}", color = Silver)
        if (r.lateMinutes > 0) Text("Late ${r.lateMinutes} min", color = LateRed, fontWeight = FontWeight.SemiBold)
        if (r.earlyLeaveMinutes > 0) Text("Left ${r.earlyLeaveMinutes} min early - ${r.earlyReason}", color = Amber)
        if (r.remarks.isNotBlank()) Text("Remarks: ${r.remarks}", color = Silver.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun RecordList(list: List<AttendanceRecord>, empty: String) {
    if (list.isEmpty()) {
        Text(empty, color = Silver, modifier = Modifier.padding(20.dp))
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(list, key = { it.id }) { RecordCard(it) }
    }
}

@Composable
fun WorkerHome(vm: WorkerViewModel, name: String) {
    val recs by vm.records.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val todayRec = recs.firstOrNull { it.date == vm.today }
    val month = vm.today.take(7)
    val mine = recs.filter { it.date.startsWith(month) }
    val worked = mine.count { it.status in Status.presentLike }
    val marked = mine.count { it.status !in setOf(Status.HOLIDAY, Status.WEEKLY_OFF) }
    val pct = if (marked == 0) 0 else worked * 100 / marked

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Welcome, ${profile?.fullName ?: name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = SilverBright)
            Text("ID: ${profile?.employeeId ?: "-"}   |   ${prettyDate(vm.today)}", color = Silver)
        }
        if (todayRec == null) {
            GlassCard(Modifier.fillMaxWidth()) { Text("Today's attendance: not marked yet", color = Silver) }
        } else RecordCard(todayRec)
        Text("This month", color = Silver, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Present", worked.toString(), OkGreen, Modifier.weight(1f))
            Stat("Absent", mine.count { it.status == Status.ABSENT }.toString(), SilverBright, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Late", mine.count { it.status == Status.LATE }.toString(), LateRed, Modifier.weight(1f))
            Stat("Attendance %", "$pct%", SilverBright, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    GlassCard(modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = color)
        Text(label, color = Silver, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun WorkerAttendance(vm: WorkerViewModel) {
    val recs by vm.records.collectAsStateWithLifecycle()
    RecordList(recs, "No attendance records yet.")
}

@Composable
fun WorkerLate(vm: WorkerViewModel) {
    val recs by vm.records.collectAsStateWithLifecycle()
    val late = recs.filter { it.status == Status.LATE }
    Column(Modifier.fillMaxSize()) {
        GlassCard(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Total late days: ${late.size}", fontWeight = FontWeight.Bold, color = LateRed)
            Text("Total late minutes: ${late.sumOf { it.lateMinutes }}", color = Silver)
        }
        RecordList(late, "No late arrivals. Great!")
    }
}

@Composable
fun WorkerMonthly(vm: WorkerViewModel) {
    val recs by vm.records.collectAsStateWithLifecycle()
    val months = recs.groupBy { it.date.take(7) }.toSortedMap(compareByDescending<String> { it })
    if (months.isEmpty()) { Text("No attendance records yet.", color = Silver, modifier = Modifier.padding(20.dp)); return }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(months.entries.toList(), key = { it.key }) { (m, list) ->
            GlassCard(Modifier.fillMaxWidth()) {
                Text(m, fontWeight = FontWeight.Bold, color = SilverBright)
                Spacer(Modifier.height(4.dp))
                Text("Present ${list.count { it.status in Status.presentLike }}  |  Absent ${list.count { it.status == Status.ABSENT }}  |  Leave ${list.count { it.status == Status.LEAVE }}", color = Silver)
                Text("Late days ${list.count { it.status == Status.LATE }}  |  Late min ${list.sumOf { it.lateMinutes }}", color = LateRed)
                Text("Hours ${fmtHours(list.sumOf { it.workMinutes })}", color = Silver)
            }
        }
    }
}

@Composable
fun WorkerProfile(vm: WorkerViewModel) {
    val p by vm.profile.collectAsStateWithLifecycle()
    val e = p
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        GlassCard(Modifier.fillMaxWidth()) {
            if (e == null) Text("Loading profile...", color = Silver) else {
                Text(e.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = SilverBright)
                Spacer(Modifier.height(8.dp))
                listOf("Employee ID" to e.employeeId, "Email / login" to e.email, "Phone" to e.phone,
                    "Department" to e.department, "Designation" to e.designation, "Joined" to e.joiningDate).forEach { (k, v) ->
                    Text("$k: ${v.ifBlank { "-" }}", color = Silver, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}
