package com.attendance.pro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendance.pro.data.*
import com.attendance.pro.ui.AdminViewModel
import com.attendance.pro.ui.components.*
import com.attendance.pro.ui.theme.*

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Silver, unfocusedBorderColor = Slate,
    focusedLabelColor = Silver, unfocusedLabelColor = Silver.copy(alpha = 0.6f),
    cursorColor = Silver, focusedTextColor = SilverBright, unfocusedTextColor = SilverBright
)

// ---------------------------------------------------------------- Dashboard
@Composable
fun AdminDashboard(vm: AdminViewModel) {
    val all by vm.allEmployees.collectAsStateWithLifecycle()
    val today by vm.todayRecords.collectAsStateWithLifecycle()
    val active = all.filter { it.active && !it.deleted }
    val activeUids = active.map { it.uid }.toSet()
    val recs = today.filter { it.employeeUid in activeUids }
    val present = recs.count { it.status in Status.presentLike }
    val stats = listOf(
        "Total Employees" to active.size.toString(),
        "Present Today" to present.toString(),
        "Absent Today" to recs.count { it.status == Status.ABSENT }.toString(),
        "Late Today" to recs.count { it.status == Status.LATE }.toString(),
        "On-Time" to recs.count { it.status == Status.PRESENT }.toString(),
        "On Leave" to recs.count { it.status == Status.LEAVE }.toString(),
        "Left Early" to recs.count { it.earlyLeaveMinutes > 0 }.toString(),
        "Not Marked Yet" to (active.size - recs.size).coerceAtLeast(0).toString()
    )
    val colors = mapOf("Late Today" to LateRed, "Present Today" to OkGreen, "On-Time" to OkGreen, "Left Early" to Amber)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Today - ${prettyDate(vm.today)}", color = Silver, fontWeight = FontWeight.Bold)
        stats.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (label, value) ->
                    GlassCard(Modifier.weight(1f)) {
                        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = colors[label] ?: SilverBright)
                        Text(label, color = Silver, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Total Working Hours Today", color = Silver, style = MaterialTheme.typography.labelMedium)
            Text(fmtHours(recs.sumOf { it.workMinutes }), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = SilverBright)
        }
        val early = recs.filter { it.earlyLeaveMinutes > 0 }
        if (early.isNotEmpty()) {
            GlassCard(Modifier.fillMaxWidth(), accent = Amber) {
                Text("Left before closing time", fontWeight = FontWeight.Bold, color = Amber)
                early.forEach {
                    Spacer(Modifier.height(8.dp))
                    Text("${it.employeeName} - ${it.earlyLeaveMinutes} min early", color = SilverBright, fontWeight = FontWeight.SemiBold)
                    Text("Reason: ${it.earlyReason}", color = Silver, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Employees
@Composable
fun EmployeesScreen(vm: AdminViewModel) {
    val all by vm.allEmployees.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Employee?>(null) }
    var deleting by remember { mutableStateOf<Employee?>(null) }
    val list = all.filter { !it.deleted }.sortedBy { it.employeeId }.filter {
        query.isBlank() || it.fullName.contains(query, true) || it.employeeId.contains(query, true) ||
            it.department.contains(query, true)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(query, { query = it }, singleLine = true, label = { Text("Search name / ID / department") },
                leadingIcon = { Icon(Icons.Filled.Search, null) }, modifier = Modifier.weight(1f), colors = fieldColors())
            FilledIconButton({ editing = null; showForm = true }) { Icon(Icons.Filled.Add, "Add employee") }
        }
        if (list.isEmpty()) {
            Text(if (all.isEmpty()) "No employees yet. Tap + to add the first one." else "No match.", color = Silver, modifier = Modifier.padding(top = 24.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(list, key = { it.uid }) { e ->
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(e.fullName, fontWeight = FontWeight.Bold, color = SilverBright)
                            Text("${e.employeeId}  |  ${e.department.ifBlank { "-" }}  |  ${e.designation.ifBlank { "-" }}", color = Silver, style = MaterialTheme.typography.bodySmall)
                            Text(e.email, color = Silver.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = e.active, onCheckedChange = { vm.setActive(e, it) })
                    }
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        if (!e.active) StatusBadge("INACTIVE", Amber)
                        IconButton({ editing = e; showForm = true }) { Icon(Icons.Filled.Edit, "Edit", tint = Silver) }
                        IconButton({ deleting = e }) { Icon(Icons.Filled.Delete, "Delete", tint = LateRed) }
                    }
                }
            }
        }
    }

    if (showForm) EmployeeForm(editing, vm.busy, { showForm = false }) { form, loginId, pw ->
        vm.saveEmployee(editing, form, loginId, pw) { showForm = false }
    }
    deleting?.let { e ->
        AlertDialog(
            onDismissRequest = { deleting = null }, containerColor = Graphite,
            title = { Text("Delete ${e.fullName}?") },
            text = { Text("The worker can no longer sign in. All attendance history is kept.") },
            confirmButton = { TextButton({ vm.deleteEmployee(e); deleting = null }) { Text("Delete", color = LateRed) } },
            dismissButton = { TextButton({ deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun EmployeeForm(existing: Employee?, busy: Boolean, onDismiss: () -> Unit, onSave: (Employee, String, String) -> Unit) {
    var name by remember { mutableStateOf(existing?.fullName.orEmpty()) }
    var login by remember { mutableStateOf(existing?.email.orEmpty()) }
    var pw by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(existing?.phone.orEmpty()) }
    var dept by remember { mutableStateOf(existing?.department.orEmpty()) }
    var desig by remember { mutableStateOf(existing?.designation.orEmpty()) }
    var join by remember { mutableStateOf(existing?.joiningDate ?: java.time.LocalDate.now().toString()) }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() }, containerColor = Graphite,
        title = { Text(if (existing == null) "Add Employee" else "Edit Employee") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Full name") }, singleLine = true, colors = fieldColors())
                OutlinedTextField(login, { login = it }, label = { Text("Login email / username") }, singleLine = true,
                    enabled = existing == null, colors = fieldColors())
                if (existing == null) OutlinedTextField(pw, { pw = it }, label = { Text("Password (min 6)") }, singleLine = true, colors = fieldColors())
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), colors = fieldColors())
                OutlinedTextField(dept, { dept = it }, label = { Text("Department") }, singleLine = true, colors = fieldColors())
                OutlinedTextField(desig, { desig = it }, label = { Text("Designation") }, singleLine = true, colors = fieldColors())
                DateField(join) { join = it }
                err?.let { Text(it, color = LateRed) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                err = when {
                    name.isBlank() -> "Enter the full name."
                    existing == null && login.isBlank() -> "Enter a login email or username."
                    existing == null && pw.length < 6 -> "Password must be at least 6 characters."
                    else -> null
                }
                if (err == null) onSave(
                    Employee(fullName = name.trim(), phone = phone.trim(), department = dept.trim(),
                        designation = desig.trim(), joiningDate = join), login, pw
                )
            }) { if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } }
    )
}

// ---------------------------------------------------------------- Attendance
@Composable
fun AttendanceScreen(vm: AdminViewModel) {
    val date by vm.date.collectAsStateWithLifecycle()
    val records by vm.records.collectAsStateWithLifecycle()
    val all by vm.allEmployees.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Employee?>(null) }
    val active = all.filter { it.active && !it.deleted }.sortedBy { it.employeeId }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton({ vm.shiftDate(-1) }) { Icon(Icons.Filled.ChevronLeft, "Previous day", tint = Silver) }
            DateField(date) { vm.setDate(it) }
            Text(dayName(date), color = Silver)
            IconButton({ vm.shiftDate(1) }) { Icon(Icons.Filled.ChevronRight, "Next day", tint = Silver) }
        }
        if (active.isEmpty()) Text("Add employees first (Employees tab).", color = Silver, modifier = Modifier.padding(top = 16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(active, key = { it.uid }) { e ->
                val r = records.firstOrNull { it.employeeUid == e.uid }
                GlassCard(Modifier.fillMaxWidth().clickable { editing = e }, accent = if (r?.status == Status.LATE) LateRed else null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(e.fullName, fontWeight = FontWeight.Bold, color = SilverBright)
                            Text(e.employeeId, color = Silver, style = MaterialTheme.typography.bodySmall)
                        }
                        if (r == null) StatusBadge("NOT MARKED", Silver) else StatusBadge(r.status, statusColor(r.status))
                    }
                    if (r != null) {
                        Spacer(Modifier.height(6.dp))
                        Text("IN ${r.checkIn.ifBlank { "--:--" }}   OUT ${r.checkOut.ifBlank { "--:--" }}   ${fmtHours(r.workMinutes)}", color = Silver)
                        if (r.lateMinutes > 0) Text("Late ${r.lateMinutes} min (${r.lateBeyondGrace} beyond grace)", color = LateRed, fontWeight = FontWeight.SemiBold)
                        if (r.earlyLeaveMinutes > 0) Text("Left ${r.earlyLeaveMinutes} min early - ${r.earlyReason}", color = Amber)
                        if (r.remarks.isNotBlank()) Text("Remarks: ${r.remarks}", color = Silver.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    editing?.let { emp ->
        AttendanceDialog(
            emp = emp, rec = records.firstOrNull { it.employeeUid == emp.uid }, settings = settings ?: OfficeSettings(),
            date = date, busy = vm.busy, onDismiss = { editing = null }
        ) { inT, outT, st, rem, reason -> vm.saveAttendance(emp, date, inT, outT, st, rem, reason) { editing = null } }
    }
}

@Composable
private fun AttendanceDialog(
    emp: Employee, rec: AttendanceRecord?, settings: OfficeSettings, date: String, busy: Boolean,
    onDismiss: () -> Unit, onSave: (String, String, String, String, String) -> Unit
) {
    var inT by remember { mutableStateOf(rec?.checkIn.orEmpty()) }
    var outT by remember { mutableStateOf(rec?.checkOut.orEmpty()) }
    var status by remember { mutableStateOf(rec?.status ?: Status.AUTO) }
    var remarks by remember { mutableStateOf(rec?.remarks.orEmpty()) }
    var reason by remember { mutableStateOf(rec?.earlyReason.orEmpty()) }
    var menu by remember { mutableStateOf(false) }

    val calc = AttendanceCalc.compute(settings, inT, outT, status)
    val needsReason = calc.earlyLeaveMinutes > 0 && calc.status in setOf(Status.PRESENT, Status.LATE)

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() }, containerColor = Graphite,
        title = { Text("${emp.fullName}\n${prettyDate(date)} (${dayName(date)})", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Office ${settings.startTime} - ${settings.endTime}, grace ${settings.graceMinutes} min", color = Silver, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeField("IN", inT, Modifier.weight(1f)) { inT = it }
                    TimeField("OUT", outT, Modifier.weight(1f)) { outT = it }
                }
                Row {
                    if (inT.isNotBlank()) TextButton({ inT = "" }) { Text("Clear IN") }
                    if (outT.isNotBlank()) TextButton({ outT = "" }) { Text("Clear OUT") }
                }
                Box {
                    OutlinedButton({ menu = true }) { Text("Status: $status", color = SilverBright) }
                    DropdownMenu(menu, { menu = false }) {
                        Status.options.forEach { o -> DropdownMenuItem(text = { Text(o) }, onClick = { status = o; menu = false }) }
                    }
                }
                Text("Result: ${calc.status}   Hours: ${fmtHours(calc.workMinutes)}", color = statusColor(calc.status), fontWeight = FontWeight.SemiBold)
                if (calc.lateMinutes > 0) Text("Late ${calc.lateMinutes} min (${calc.lateBeyondGrace} min beyond grace)", color = LateRed)
                if (calc.earlyLeaveMinutes > 0) Text("Left ${calc.earlyLeaveMinutes} min before closing time", color = Amber)
                if (needsReason || reason.isNotBlank()) {
                    OutlinedTextField(reason, { reason = it }, label = { Text("Reason for leaving early *") }, minLines = 2, colors = fieldColors(),
                        isError = needsReason && reason.isBlank(), modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(remarks, { remarks = it }, label = { Text("Remarks (optional)") }, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = { onSave(inT, outT, status, remarks, reason) }) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } }
    )
}

// ---------------------------------------------------------------- Settings
@Composable
fun AdminSettingsScreen(vm: AdminViewModel) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val cur = s
    if (cur == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Silver) }
        return
    }
    var name by remember(cur) { mutableStateOf(cur.officeName) }
    var start by remember(cur) { mutableStateOf(cur.startTime) }
    var end by remember(cur) { mutableStateOf(cur.endTime) }
    var grace by remember(cur) { mutableStateOf(cur.graceMinutes) }
    var custom by remember(cur) { mutableStateOf(cur.graceMinutes !in listOf(5, 10, 15, 20, 30)) }
    var err by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Office", fontWeight = FontWeight.Bold, color = SilverBright)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(name, { name = it }, label = { Text("Office name") }, singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeField("Start", start, Modifier.weight(1f)) { start = it }
                TimeField("Close", end, Modifier.weight(1f)) { end = it }
            }
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Late grace period", fontWeight = FontWeight.Bold, color = SilverBright)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15, 20, 30).forEach { v ->
                    FilterChip(selected = !custom && grace == v, onClick = { custom = false; grace = v }, label = { Text("$v min") })
                }
                FilterChip(selected = custom, onClick = { custom = true }, label = { Text("Custom") })
            }
            if (custom) {
                OutlinedTextField(grace.toString(), { grace = it.filter(Char::isDigit).take(3).toIntOrNull() ?: 0 },
                    label = { Text("Custom minutes") }, singleLine = true, colors = fieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.padding(top = 8.dp))
            }
            Text("Example: start $start + $grace min grace => arriving after ${minutesToHm((AttendanceCalc.toMinutes(start) ?: 540) + grace)} is LATE.",
                color = Silver, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            Text("Changing this affects new entries only. Past records keep the grace they were saved with.", color = Silver.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
        }
        err?.let { Text(it, color = LateRed) }
        GradientButton("SAVE SETTINGS", loading = vm.busy) {
            err = when {
                name.isBlank() -> "Enter the office name."
                AttendanceCalc.toMinutes(start) == null || AttendanceCalc.toMinutes(end) == null -> "Pick valid start and closing times."
                grace !in 0..120 -> "Grace period must be 0-120 minutes."
                else -> null
            }
            if (err == null) vm.saveSettings(OfficeSettings(name.trim(), start, end, grace))
        }
    }
}

private fun minutesToHm(m: Int): String = "%02d:%02d".format((m / 60) % 24, m % 60)
