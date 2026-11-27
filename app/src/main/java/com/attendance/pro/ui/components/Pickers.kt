package com.attendance.pro.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.attendance.pro.data.Status
import com.attendance.pro.ui.theme.*
import java.time.LocalDate

@Composable
fun TimeField(label: String, value: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    val ctx = LocalContext.current
    OutlinedButton(
        onClick = {
            val p = value.split(":")
            val h = p.getOrNull(0)?.toIntOrNull() ?: 9
            val m = p.getOrNull(1)?.toIntOrNull() ?: 0
            TimePickerDialog(ctx, { _, hh, mm -> onChange("%02d:%02d".format(hh, mm)) }, h, m, true).show()
        },
        modifier = modifier, shape = RoundedCornerShape(12.dp)
    ) { Text("$label: ${value.ifBlank { "--:--" }}", color = SilverBright) }
}

@Composable
fun DateField(date: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    val ctx = LocalContext.current
    OutlinedButton(
        onClick = {
            val d = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
            DatePickerDialog(ctx, { _, y, m, day -> onChange(LocalDate.of(y, m + 1, day).toString()) },
                d.year, d.monthValue - 1, d.dayOfMonth).show()
        },
        modifier = modifier, shape = RoundedCornerShape(12.dp)
    ) { Text(com.attendance.pro.data.prettyDate(date), color = SilverBright) }
}

fun statusColor(s: String): Color = when (s) {
    Status.PRESENT -> OkGreen
    Status.LATE -> LateRed
    Status.ABSENT -> Color(0xFFFF8A65)
    Status.HALF_DAY -> Amber
    else -> Accent
}
