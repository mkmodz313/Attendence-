package com.attendance.pro.ui.screens

import androidx.compose.runtime.saveable.rememberSaveable
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendance.pro.data.*
import com.attendance.pro.ui.SheetsViewModel
import com.attendance.pro.ui.components.*
import com.attendance.pro.ui.theme.*

@Composable
private fun sheetFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Cyan, unfocusedBorderColor = Slate,
    focusedLabelColor = Cyan, unfocusedLabelColor = Silver.copy(alpha = 0.6f),
    cursorColor = Cyan, focusedTextColor = SilverBright, unfocusedTextColor = SilverBright,
    focusedContainerColor = Color.White.copy(alpha = 0.04f), unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
)


@Composable
fun SheetsScreen(vm: SheetsViewModel, editable: Boolean) {
    val sheets by vm.sheets.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val current = sheets.firstOrNull { it.id == selectedId }

    LaunchedEffect(current?.id) { vm.setActiveSheet(current?.id) }
    BackHandler(enabled = current != null) { selectedId = null }

    if (current == null) SheetList(vm, editable, onOpen = { selectedId = it })
    else SheetDetail(vm, current, editable, onBack = { selectedId = null }, onDeleted = { selectedId = null })
}

// ------------------------------------------------------------------ list of sheets (groups)
@Composable
private fun SheetList(vm: SheetsViewModel, editable: Boolean, onOpen: (String) -> Unit) {
    val sheets by vm.sheets.collectAsStateWithLifecycle()
    var showForm by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Sheets", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = SilverBright)
            if (editable) Box(
                Modifier.size(44.dp).shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = Cyan, spotColor = Cyan)
                    .background(Brush.linearGradient(listOf(Neon, Violet)), RoundedCornerShape(14.dp))
                    .clickable { showForm = true },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Add, "Create sheet", tint = Color.White) }
        }

        if (sheets.isEmpty()) {
            Raised3DCard(Modifier.fillMaxWidth().padding(top = 8.dp), accent = Neon) {
                Text(
                    if (editable) "Create your first sheet" else "No sheets yet",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = SilverBright
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (editable) "Tap the + button, enter a sheet name and a group name. Create one sheet per group."
                    else "The admin has not created any sheet yet.",
                    color = Silver, style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(sheets, key = { it.id }) { s ->
                Raised3DCard(Modifier.fillMaxWidth().clickable { onOpen(s.id) }, accent = Neon) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(52.dp).shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = Violet, spotColor = Violet)
                                .background(Brush.linearGradient(listOf(Neon, Violet)), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.TableChart, null, tint = Color.White) }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = SilverBright, maxLines = 1)
                            Spacer(Modifier.height(4.dp))
                            GlowChip(if (s.groupName.isBlank()) "No group" else s.groupName, Violet)
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = Silver)
                    }
                }
            }
        }
    }

    if (showForm) SheetForm(
        existing = null,
        onDismiss = { showForm = false },
        onSave = { name, group -> vm.addSheet(name, group); showForm = false }
    )
}

// ------------------------------------------------------------------ one sheet: editable paper form (Excel / Word-table style)
private val Paper = Color(0xFFF6F3EA)
private val PaperHead = Color(0xFFE2DED2)
private val PaperLine = Color(0xFF8E9399)
private val Ink = Color(0xFF1B2A8A)
private val SelBlue = Color(0x333F51B5)
private val RowNumW = 34.dp
private val LetterH = 22.dp

private val Palette = listOf(
    "#FFFFFF", "#FFF59D", "#FFCC80", "#EF9A9A", "#F48FB1", "#CE93D8",
    "#9FA8DA", "#90CAF9", "#80DEEA", "#A5D6A7", "#C5E1A5", "#E2DED2",
    "#BDBDBD", "#8C8C8C", "#424242", "#000000", "#1B2A8A", "#C62828"
)

/** kind: 0 = one cell, 1 = whole row, 2 = whole column */
private data class Sel(val kind: Int, val r: Int, val c: Int)

private fun hex(s: String): Color? =
    if (s.isBlank()) null else runCatching { Color(android.graphics.Color.parseColor(s)) }.getOrNull()

private fun styled(g: SheetGrid, s: Sel, f: (GridCell) -> GridCell): SheetGrid = when (s.kind) {
    0 -> GridOps.updateCell(g, s.r, s.c, f)
    1 -> GridOps.updateRow(g, s.r, f)
    else -> GridOps.updateCol(g, s.c, f)
}

@Composable
private fun ToolChip(label: String, color: Color = Cyan, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        label, color = if (enabled) color else Silver.copy(alpha = 0.35f), fontWeight = FontWeight.Bold, fontSize = 12.sp,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(10.dp))
            .border(1.dp, color.copy(alpha = if (enabled) 0.45f else 0.15f), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    )
}

@Composable
private fun SheetDetail(
    vm: SheetsViewModel, current: SheetDef, editable: Boolean, onBack: () -> Unit, onDeleted: () -> Unit
) {
    val snap by vm.gridSnap.collectAsStateWithLifecycle()
    val date by vm.date.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val density = LocalDensity.current

    val ym = YearMonth.from(LocalDate.parse(date))
    val expectedId = current.id + "_" + ym.toString()
    val valid = snap.docId == expectedId
    val g: SheetGrid? = if (valid) snap.grid else null
    val monthTitle = ym.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + ym.year

    var sel by remember(expectedId) { mutableStateOf<Sel?>(null) }
    var editingText by remember { mutableStateOf(false) }
    var colorFor by remember { mutableStateOf<String?>(null) }
    var addingName by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Sel?>(null) }
    var editingSheet by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val hs = rememberScrollState()
    val vs = rememberScrollState()

    val s: Sel? = sel?.takeIf { g != null && it.r < g.rowIds.size && it.c < g.colIds.size }
    val primary: GridCell? = if (g != null && s != null)
        GridOps.cell(g, if (s.kind == 2) 0 else s.r, if (s.kind == 1) 0 else s.c) else null

    LaunchedEffect(expectedId, g != null) {
        if (g == null) return@LaunchedEffect
        val today = LocalDate.now()
        val target = if (YearMonth.from(today) == ym) maxOf(0, today.dayOfMonth - 2) else 0
        var px = 0
        for (i in 0 until minOf(target + 1, g.colW.size)) px += with(density) { g.colW[i].dp.roundToPx() }
        delay(150)
        hs.scrollTo(px)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = Silver) }
            Text("All sheets", color = Silver, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(
                onClick = {
                    val text = GridOps.report(g ?: return@IconButton, current.name, current.groupName)
                    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
                    ctx.startActivity(Intent.createChooser(send, "Share report"))
                },
                enabled = g != null
            ) { Icon(Icons.Filled.Share, "Share report", tint = Cyan) }
            if (editable) {
                IconButton({ editingSheet = true }) { Icon(Icons.Filled.Edit, "Edit sheet", tint = Silver) }
                IconButton({ deleting = true }) { Icon(Icons.Filled.Delete, "Delete sheet", tint = LateRed) }
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton({ vm.shiftMonth(-1) }) { Icon(Icons.Filled.ChevronLeft, "Previous month", tint = Silver) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(monthTitle, color = SilverBright, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                Text(
                    current.name + if (current.groupName.isBlank()) "" else "  \u2022  " + current.groupName,
                    color = Silver, style = MaterialTheme.typography.labelMedium, maxLines = 1
                )
            }
            IconButton({ vm.shiftMonth(1) }) { Icon(Icons.Filled.ChevronRight, "Next month", tint = Silver) }
        }

        if (g == null) {
            Raised3DCard(Modifier.fillMaxWidth().padding(top = 8.dp), accent = Neon) {
                val ready = valid && snap.loaded && !snap.fromCache
                Text(
                    if (ready) "No sheet for $monthTitle yet" else "Loading...",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = SilverBright
                )
                if (ready) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (editable) "Creates the blank form with all dates and Sundays marked. Names are copied from last month."
                        else "The admin has not created this month yet.",
                        color = Silver, style = MaterialTheme.typography.bodyMedium
                    )
                    if (editable) {
                        Spacer(Modifier.height(10.dp))
                        Button({ vm.createMonth(current.id) }) { Text("Create sheet for $monthTitle") }
                    }
                }
            }
        } else {
            // ---------------- toolbar (admin only)
            if (editable) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    ToolChip("+ Name", OkGreen) { addingName = true }
                    ToolChip("Undo", Amber, enabled = vm.canUndo) { vm.undo() }
                    ToolChip("+ Row") { vm.editGrid { GridOps.insertRow(it, it.rowIds.size) } }
                    ToolChip("+ Column") { vm.editGrid { GridOps.insertCol(it, it.colIds.size) } }
                }
                if (s == null) {
                    Text(
                        "Tap a cell to select (tap again to type). Tap a row number or column letter to select the whole row or column. Drag the dark bar on a column letter or row number to resize it.",
                        color = Silver.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                } else {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            when (s.kind) { 0 -> "Cell ${colLetter(s.c)}${s.r + 1}"; 1 -> "Row ${s.r + 1}"; else -> "Column ${colLetter(s.c)}" },
                            color = SilverBright, fontWeight = FontWeight.Black, fontSize = 12.sp
                        )
                        if (s.kind == 0) ToolChip("Edit") { editingText = true }
                        ToolChip("Fill") { colorFor = "fill" }
                        ToolChip("Text color") { colorFor = "text" }
                        ToolChip(if (primary?.b == true) "Bold \u2713" else "Bold") {
                            val nb = primary?.b != true
                            vm.editGrid { styled(it, s) { c -> c.copy(b = nb) } }
                        }
                        ToolChip("A+") {
                            val nf = ((primary?.fs?.takeIf { it > 0 } ?: 12) + 1).coerceAtMost(30)
                            vm.editGrid { styled(it, s) { c -> c.copy(fs = nf) } }
                        }
                        ToolChip("A-") {
                            val nf = ((primary?.fs?.takeIf { it > 0 } ?: 12) - 1).coerceAtLeast(7)
                            vm.editGrid { styled(it, s) { c -> c.copy(fs = nf) } }
                        }
                        ToolChip("Align") {
                            val na = ((primary?.al ?: 1) + 1) % 3
                            vm.editGrid { styled(it, s) { c -> c.copy(al = na) } }
                        }
                        if (s.kind == 0) {
                            ToolChip(if (primary?.mu == true) "Unmerge" else "Merge up", enabled = s.r > 0) {
                                vm.editGrid { GridOps.toggleMergeUp(it, s.r, s.c) }
                            }
                            ToolChip("Row below") { vm.editGrid { GridOps.insertRow(it, s.r + 1) }; sel = null }
                            ToolChip("Column right") { vm.editGrid { GridOps.insertCol(it, s.c + 1) }; sel = null }
                        }
                        if (s.kind == 1) {
                            ToolChip("Insert above") { vm.editGrid { GridOps.insertRow(it, s.r) }; sel = null }
                            ToolChip("Insert below") { vm.editGrid { GridOps.insertRow(it, s.r + 1) }; sel = null }
                            ToolChip("Taller") { vm.editGrid { GridOps.resizeRow(it, s.r, 6) } }
                            ToolChip("Shorter") { vm.editGrid { GridOps.resizeRow(it, s.r, -6) } }
                            ToolChip("Delete row", LateRed) { confirmDelete = s }
                        }
                        if (s.kind == 2) {
                            ToolChip("Insert left") { vm.editGrid { GridOps.insertCol(it, s.c) }; sel = null }
                            ToolChip("Insert right") { vm.editGrid { GridOps.insertCol(it, s.c + 1) }; sel = null }
                            ToolChip("Wider") { vm.editGrid { GridOps.resizeCol(it, s.c, 8) } }
                            ToolChip("Narrower") { vm.editGrid { GridOps.resizeCol(it, s.c, -8) } }
                            ToolChip("Delete column", LateRed) { confirmDelete = s }
                        }
                        ToolChip("Clear", Silver) {
                            vm.editGrid { styled(it, s) { c -> GridCell(mu = c.mu) } }
                        }
                    }
                }
            }

            GridView(
                g = g, sel = s, monthTitle = monthTitle, hs = hs, vs = vs,
                onCell = if (editable) ({ r, c ->
                    if (s != null && s.kind == 0 && s.r == r && s.c == c) editingText = true else sel = Sel(0, r, c)
                }) else null,
                onRow = if (editable) ({ r -> sel = Sel(1, r, 0) }) else null,
                onCol = if (editable) ({ c -> sel = Sel(2, 0, c) }) else null,
                onResizeCol = if (editable) ({ c, w -> vm.editGrid { GridOps.setColW(it, c, w) } }) else null,
                onResizeRow = if (editable) ({ r, h -> vm.editGrid { GridOps.setRowH(it, r, h) } }) else null,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp, bottom = 8.dp)
            )
        }
    }

    // ---------------- dialogs
    if (editingText && g != null && s != null && s.kind == 0) {
        CellTextDialog(
            title = "${colLetter(s.c)}${s.r + 1}",
            initial = GridOps.cell(g, s.r, s.c).t,
            onDismiss = { editingText = false },
            onSave = { t -> vm.editGrid { GridOps.updateCell(it, s.r, s.c) { c -> c.copy(t = t) } }; editingText = false }
        )
    }

    colorFor?.let { which ->
        ColorDialog(
            title = if (which == "fill") "Fill colour" else "Text colour",
            onDismiss = { colorFor = null },
            onPick = { hexValue ->
                if (s != null) vm.editGrid { styled(it, s) { c -> if (which == "fill") c.copy(bg = hexValue) else c.copy(fg = hexValue) } }
                colorFor = null
            }
        )
    }

    if (addingName) AddNameDialog(
        onDismiss = { addingName = false },
        onAdd = { name -> vm.editGrid { GridOps.addName(it, name) } }
    )

    confirmDelete?.let { d ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null }, containerColor = Graphite,
            title = { Text(if (d.kind == 1) "Delete row ${d.r + 1}?" else "Delete column ${colLetter(d.c)}?") },
            text = { Text("Everything in it will be removed. You can Undo right after.") },
            confirmButton = {
                TextButton({
                    if (d.kind == 1) vm.editGrid { GridOps.deleteRow(it, d.r) } else vm.editGrid { GridOps.deleteCol(it, d.c) }
                    confirmDelete = null; sel = null
                }) { Text("Delete", color = LateRed) }
            },
            dismissButton = { TextButton({ confirmDelete = null }) { Text("Cancel") } }
        )
    }

    if (editingSheet) SheetForm(
        existing = current,
        onDismiss = { editingSheet = false },
        onSave = { name, group -> vm.updateSheet(current, name, group); editingSheet = false }
    )

    if (deleting) AlertDialog(
        onDismissRequest = { deleting = false }, containerColor = Graphite,
        title = { Text("Delete sheet \"${current.name}\"?") },
        text = { Text("The sheet and all its months will be removed.") },
        confirmButton = { TextButton({ vm.deleteSheet(current); deleting = false; onDeleted() }) { Text("Delete", color = LateRed) } },
        dismissButton = { TextButton({ deleting = false }) { Text("Cancel") } }
    )
}

@Composable
private fun GridView(
    g: SheetGrid, sel: Sel?, monthTitle: String, hs: ScrollState, vs: ScrollState,
    onCell: ((Int, Int) -> Unit)?, onRow: ((Int) -> Unit)?, onCol: ((Int) -> Unit)?,
    onResizeCol: ((Int, Int) -> Unit)?, onResizeRow: ((Int, Int) -> Unit)?, modifier: Modifier
) {
    // live widths / heights while the finger is dragging a border (saved when the finger lifts)
    val colOv = remember { mutableStateMapOf<Int, Float>() }
    val rowOv = remember { mutableStateMapOf<Int, Float>() }
    val dens = LocalDensity.current.density
    fun colWf(c: Int): Float = colOv[c] ?: g.colW.getOrElse(c) { 60 }.toFloat()
    fun rowHf(r: Int): Float = rowOv[r] ?: g.rowH.getOrElse(r) { 28 }.toFloat()
    fun colW(c: Int) = colWf(c).dp
    fun rowH(r: Int) = rowHf(r)

    Column(modifier.background(Paper, RoundedCornerShape(8.dp)).border(1.dp, PaperLine, RoundedCornerShape(8.dp))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Attendance Sheet", color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("Month of $monthTitle", color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        // column letters A B C ... (tap = select column)
        Row(Modifier.fillMaxWidth().height(LetterH)) {
            Box(Modifier.width(RowNumW).fillMaxHeight().background(PaperHead).border(0.5.dp, PaperLine))
            Row(Modifier.weight(1f).horizontalScroll(hs)) {
                g.colIds.indices.forEach { c ->
                    val on = sel != null && sel.kind == 2 && sel.c == c
                    Box(
                        Modifier.width(colW(c)).fillMaxHeight()
                            .background(if (on) Color(0xFF9FA8DA) else PaperHead).border(0.5.dp, PaperLine)
                            .then(if (onCol != null) Modifier.clickable { onCol(c) } else Modifier),
                        Alignment.Center
                    ) {
                        Text(colLetter(c), color = Ink, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        if (onResizeCol != null) Box(
                            Modifier.align(Alignment.CenterEnd).width(18.dp).fillMaxHeight()
                                .pointerInput(c) {
                                    detectHorizontalDragGestures(
                                        onDragEnd = { colOv[c]?.let { w -> onResizeCol(c, w.toInt()) }; colOv.remove(c) },
                                        onDragCancel = { colOv.remove(c) }
                                    ) { change, dx ->
                                        change.consume()
                                        colOv[c] = (colWf(c) + dx / dens).coerceIn(24f, 320f)
                                    }
                                },
                            contentAlignment = Alignment.CenterEnd
                        ) { Box(Modifier.width(4.dp).fillMaxHeight().background(Ink.copy(alpha = 0.55f))) }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().weight(1f).verticalScroll(vs)) {
            // row numbers 1 2 3 ... (tap = select row)
            Column {
                g.rowIds.indices.forEach { r ->
                    val on = sel != null && sel.kind == 1 && sel.r == r
                    Box(
                        Modifier.width(RowNumW).height(rowH(r).dp)
                            .background(if (on) Color(0xFF9FA8DA) else PaperHead).border(0.5.dp, PaperLine)
                            .then(if (onRow != null) Modifier.clickable { onRow(r) } else Modifier),
                        Alignment.Center
                    ) {
                        Text("${r + 1}", color = Ink, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        if (onResizeRow != null) Box(
                            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(18.dp)
                                .pointerInput(r) {
                                    detectVerticalDragGestures(
                                        onDragEnd = { rowOv[r]?.let { h -> onResizeRow(r, h.toInt()) }; rowOv.remove(r) },
                                        onDragCancel = { rowOv.remove(r) }
                                    ) { change, dy ->
                                        change.consume()
                                        rowOv[r] = (rowHf(r) + dy / dens).coerceIn(18f, 240f)
                                    }
                                },
                            contentAlignment = Alignment.BottomCenter
                        ) { Box(Modifier.fillMaxWidth().height(4.dp).background(Ink.copy(alpha = 0.55f))) }
                    }
                }
            }
            Row(Modifier.weight(1f).horizontalScroll(hs)) {
                g.colIds.indices.forEach { c ->
                    Column(Modifier.width(colW(c))) {
                        var r = 0
                        while (r < g.rowIds.size) {
                            val cell = GridOps.cell(g, r, c)
                            var span = 1
                            while (r + span < g.rowIds.size && GridOps.cell(g, r + span, c).mu) span++
                            var h = 0f
                            for (k in r until span + r) h += rowH(k)
                            val head = r
                            val inSel = sel != null && when (sel.kind) {
                                0 -> sel.r == head && sel.c == c
                                1 -> sel.r >= head && sel.r < head + span
                                2 -> sel.c == c
                                else -> false
                            }
                            val strong = sel != null && sel.kind == 0 && inSel
                            Box(
                                Modifier.fillMaxWidth().height(h.dp)
                                    .background(hex(cell.bg) ?: Color.Transparent)
                                    .background(if (inSel) SelBlue else Color.Transparent)
                                    .border(if (strong) 2.dp else 0.5.dp, if (strong) Ink else PaperLine)
                                    .then(if (onCell != null) Modifier.clickable { onCell(head, c) } else Modifier)
                                    .padding(horizontal = 3.dp),
                                contentAlignment = when (cell.al) { 0 -> Alignment.CenterStart; 2 -> Alignment.CenterEnd; else -> Alignment.Center }
                            ) {
                                Text(
                                    cell.t, color = hex(cell.fg) ?: Ink,
                                    fontSize = (if (cell.fs > 0) cell.fs else 12).sp, lineHeight = ((if (cell.fs > 0) cell.fs else 12) + 1).sp,
                                    fontWeight = if (cell.b) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = when (cell.al) { 0 -> TextAlign.Start; 2 -> TextAlign.End; else -> TextAlign.Center }
                                )
                            }
                            r += span
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CellTextDialog(title: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Graphite,
        title = { Text("Edit $title", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    text, { text = it }, label = { Text("Cell text") }, maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = sheetFieldColors()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ onSave("L") }, Modifier.weight(1f)) { Text("L  Leave", color = Cyan) }
                    OutlinedButton({ onSave("A") }, Modifier.weight(1f)) { Text("A  Absent", color = LateRed) }
                }
            }
        },
        confirmButton = { TextButton({ onSave(text) }) { Text("Save") } },
        dismissButton = { TextButton({ onSave("") }) { Text("Empty it", color = Silver) } }
    )
}

@Composable
private fun ColorDialog(title: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Graphite,
        title = { Text(title, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Palette.chunked(6).forEach { rowColors ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowColors.forEach { h ->
                            Box(
                                Modifier.size(38.dp).background(hex(h) ?: Color.Gray, RoundedCornerShape(10.dp))
                                    .border(1.dp, Silver.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .clickable { onPick(h) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton({ onPick("") }) { Text("No colour") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddNameDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Graphite,
        title = { Text("Add name", fontWeight = FontWeight.Black) },
        text = {
            OutlinedTextField(
                name, { name = it }, label = { Text("Type a name") }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                colors = sheetFieldColors()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val n = name.trim().replace(Regex("\\s+"), " ")
                if (n.isNotEmpty()) onAdd(n)
                name = ""
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Done") } }
    )
}

@Composable
private fun SheetForm(existing: SheetDef?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var group by remember { mutableStateOf(existing?.groupName.orEmpty()) }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Graphite,
        title = { Text(if (existing == null) "New Sheet" else "Edit Sheet", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Sheet name") }, singleLine = true, colors = sheetFieldColors())
                OutlinedTextField(group, { group = it }, label = { Text("Group name") }, singleLine = true, colors = sheetFieldColors())
                err?.let { Text(it, color = LateRed) }
            }
        },
        confirmButton = {
            TextButton({ if (name.isBlank()) err = "Enter the sheet name." else onSave(name, group) }) { Text("Save") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}
