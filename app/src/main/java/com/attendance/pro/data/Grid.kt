package com.attendance.pro.data

import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

/** One spreadsheet cell. Missing cells in the map are simply empty / unformatted. */
data class GridCell(
    val t: String = "",        // text (may contain a line break)
    val bg: String = "",       // fill colour "#RRGGBB" or "" for none
    val fg: String = "",       // text colour "#RRGGBB" or "" for default
    val b: Boolean = false,    // bold
    val al: Int = 1,           // 0 = left, 1 = centre, 2 = right
    val mu: Boolean = false,   // merged with the cell above (vertical merge)
    val fs: Int = 0            // font size in sp, 0 = default (12)
)

/**
 * A fully editable table (like Excel / a Word table). One document per sheet per month:
 * doc id = "{sheetId}_{yyyy-MM}". Rows and columns are ordered id lists, so inserting or deleting
 * a row/column never disturbs other cells. Cell key = "{rowId}_{colId}".
 */
data class SheetGrid(
    val sheetId: String = "",
    val ym: String = "",
    val colIds: List<String> = emptyList(),
    val rowIds: List<String> = emptyList(),
    val colW: List<Int> = emptyList(),
    val rowH: List<Int> = emptyList(),
    val cells: Map<String, GridCell> = emptyMap()
)

/** What the screen receives from Firestore for the open sheet + month. */
data class GridSnap(
    val docId: String = "",
    val grid: SheetGrid? = null,
    val fromCache: Boolean = true,
    val loaded: Boolean = false
)

fun colLetter(i: Int): String {
    var n = i; val sb = StringBuilder()
    do { sb.insert(0, ('A' + n % 26)); n = n / 26 - 1 } while (n >= 0)
    return sb.toString()
}

object GridOps {
    const val HEAD_BG = "#E2DED2"
    const val SUNDAY_BG = "#8C8C8C"
    private const val ROW_H = 28
    private const val COL_W = 60

    private fun newId() = UUID.randomUUID().toString().replace("-", "").take(10)
    private fun key(g: SheetGrid, r: Int, c: Int) = g.rowIds[r] + "_" + g.colIds[c]
    private fun put(m: Map<String, GridCell>, k: String, v: GridCell) = if (v == GridCell()) m - k else m + (k to v)

    fun cell(g: SheetGrid, r: Int, c: Int): GridCell =
        if (r in g.rowIds.indices && c in g.colIds.indices) g.cells[key(g, r, c)] ?: GridCell() else GridCell()

    fun updateCell(g: SheetGrid, r: Int, c: Int, f: (GridCell) -> GridCell): SheetGrid {
        if (r !in g.rowIds.indices || c !in g.colIds.indices) return g
        return g.copy(cells = put(g.cells, key(g, r, c), f(cell(g, r, c))))
    }

    fun updateRow(g: SheetGrid, r: Int, f: (GridCell) -> GridCell): SheetGrid =
        g.colIds.indices.fold(g) { acc, c -> updateCell(acc, r, c, f) }

    fun updateCol(g: SheetGrid, c: Int, f: (GridCell) -> GridCell): SheetGrid =
        g.rowIds.indices.fold(g) { acc, r -> updateCell(acc, r, c, f) }

    /** Fill colour a new cell in column [c] should get, copied from row [ref] (or the Sunday rule for the first rows). */
    private fun bgFor(g: SheetGrid, c: Int, ref: Int): String =
        if (ref >= 1 && ref < g.rowIds.size) cell(g, ref, c).bg
        else if (g.rowIds.isNotEmpty() && cell(g, 0, c).bg == SUNDAY_BG) SUNDAY_BG else ""

    fun insertRow(g: SheetGrid, at: Int, h: Int = ROW_H): SheetGrid {
        val pos = at.coerceIn(0, g.rowIds.size)
        val id = newId()
        val ref = if (pos - 1 >= 1) pos - 1 else pos
        var cells = g.cells
        for (c in g.colIds.indices) {
            val mu = pos > 0 && pos < g.rowIds.size && cell(g, pos, c).mu   // inside a merged block => stay merged
            cells = put(cells, id + "_" + g.colIds[c], GridCell(bg = bgFor(g, c, ref), mu = mu))
        }
        return g.copy(
            rowIds = g.rowIds.toMutableList().apply { add(pos, id) },
            rowH = g.rowH.toMutableList().apply { add(pos, h) },
            cells = cells
        )
    }

    fun deleteRow(g: SheetGrid, r: Int): SheetGrid {
        if (g.rowIds.size <= 1 || r !in g.rowIds.indices) return g
        var cells = g.cells
        for (c in g.colIds.indices) {
            val head = cell(g, r, c)
            if (!head.mu && r + 1 < g.rowIds.size && cell(g, r + 1, c).mu) cells = put(cells, key(g, r + 1, c), head)
            cells = cells - key(g, r, c)
        }
        return g.copy(
            rowIds = g.rowIds.toMutableList().apply { removeAt(r) },
            rowH = g.rowH.toMutableList().apply { if (r < size) removeAt(r) },
            cells = cells
        )
    }

    fun insertCol(g: SheetGrid, at: Int, w: Int = COL_W): SheetGrid {
        val pos = at.coerceIn(0, g.colIds.size)
        val id = newId()
        val ref = if (pos - 1 >= 0) pos - 1 else pos
        var cells = g.cells
        for (r in g.rowIds.indices) {
            val bg = if (ref < g.colIds.size) cell(g, r, ref).bg else ""
            cells = put(cells, g.rowIds[r] + "_" + id, GridCell(bg = bg))
        }
        return g.copy(
            colIds = g.colIds.toMutableList().apply { add(pos, id) },
            colW = g.colW.toMutableList().apply { add(pos, w) },
            cells = cells
        )
    }

    fun deleteCol(g: SheetGrid, c: Int): SheetGrid {
        if (g.colIds.size <= 1 || c !in g.colIds.indices) return g
        var cells = g.cells
        for (r in g.rowIds.indices) cells = cells - key(g, r, c)
        return g.copy(
            colIds = g.colIds.toMutableList().apply { removeAt(c) },
            colW = g.colW.toMutableList().apply { if (c < size) removeAt(c) },
            cells = cells
        )
    }

    fun setColW(g: SheetGrid, c: Int, w: Int): SheetGrid =
        if (c !in g.colW.indices) g else g.copy(colW = g.colW.toMutableList().apply { this[c] = w.coerceIn(24, 320) })

    fun setRowH(g: SheetGrid, r: Int, h: Int): SheetGrid =
        if (r !in g.rowH.indices) g else g.copy(rowH = g.rowH.toMutableList().apply { this[r] = h.coerceIn(18, 240) })

    fun resizeCol(g: SheetGrid, c: Int, d: Int): SheetGrid =
        if (c !in g.colW.indices) g else g.copy(colW = g.colW.toMutableList().apply { this[c] = (this[c] + d).coerceIn(24, 320) })

    fun resizeRow(g: SheetGrid, r: Int, d: Int): SheetGrid =
        if (r !in g.rowH.indices) g else g.copy(rowH = g.rowH.toMutableList().apply { this[r] = (this[r] + d).coerceIn(18, 240) })

    /** Merge this cell into the one above it (or split it again). */
    fun toggleMergeUp(g: SheetGrid, r: Int, c: Int): SheetGrid =
        if (r < 1) g else updateCell(g, r, c) { if (it.mu) it.copy(mu = false) else it.copy(mu = true, t = "") }

    /** Adds a person at the bottom: two rows (time in / time out) with the # and name cells merged. */
    fun addName(g: SheetGrid, name: String): SheetGrid {
        if (g.colIds.isEmpty()) return g
        val nameCol = if (g.colIds.size > 1) 1 else 0
        var n = insertRow(g, g.rowIds.size)
        n = insertRow(n, n.rowIds.size)
        val r1 = n.rowIds.size - 2
        val r2 = r1 + 1
        val serial = (1 until r1).count { !cell(n, it, nameCol).mu } + 1
        if (nameCol == 1) {
            n = updateCell(n, r1, 0) { it.copy(t = serial.toString()) }
            n = updateCell(n, r2, 0) { it.copy(mu = true) }
        }
        n = updateCell(n, r1, nameCol) { it.copy(t = name, b = true) }
        n = updateCell(n, r2, nameCol) { it.copy(mu = true) }
        return n
    }

    fun namesOf(g: SheetGrid): List<String> {
        val nameCol = if (g.colIds.size > 1) 1 else 0
        return (1 until g.rowIds.size).map { cell(g, it, nameCol) }.filter { !it.mu && it.t.isNotBlank() }.map { it.t }
    }

    /** The blank paper form: header row 01..31 with weekday, Sundays grey, then the names. */
    fun newMonth(sheetId: String, ym: YearMonth, names: List<String>): SheetGrid {
        val days = ym.lengthOfMonth()
        val colIds = List(2 + days) { newId() }
        val rowId = newId()
        var cells = emptyMap<String, GridCell>()
        cells = put(cells, rowId + "_" + colIds[0], GridCell(t = "#", bg = HEAD_BG, b = true))
        cells = put(cells, rowId + "_" + colIds[1], GridCell(t = "Employee Name", bg = HEAD_BG, b = true))
        for (d in 1..days) {
            val dt = ym.atDay(d)
            val sun = dt.dayOfWeek == DayOfWeek.SUNDAY
            cells = put(
                cells, rowId + "_" + colIds[1 + d],
                GridCell(
                    t = "%02d\n%s".format(d, dt.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)),
                    bg = if (sun) SUNDAY_BG else HEAD_BG, fg = if (sun) "#FFFFFF" else "", b = true
                )
            )
        }
        var g = SheetGrid(
            sheetId = sheetId, ym = ym.toString(), colIds = colIds, rowIds = listOf(rowId),
            colW = listOf(32, 120) + List(days) { 46 }, rowH = listOf(40), cells = cells
        )
        names.forEach { g = addName(g, it) }
        return g
    }

    fun report(g: SheetGrid, sheetName: String, group: String): String {
        val sb = StringBuilder()
        sb.append(sheetName).append(if (group.isBlank()) "" else " ($group)").append('\n')
        sb.append("Month ").append(g.ym).append("\n\n")
        val nameCol = if (g.colIds.size > 1) 1 else 0
        var i = 0
        for (r in 1 until g.rowIds.size) {
            val nm = cell(g, r, nameCol)
            if (nm.mu || nm.t.isBlank()) continue
            i++
            var p = 0; var l = 0; var a = 0
            for (c in (nameCol + 1) until g.colIds.size) {
                val t = cell(g, r, c).t.trim().uppercase()
                when {
                    t == "L" || t == "LEAVE" -> l++
                    t == "A" || t == "ABSENT" -> a++
                    t.contains(":") -> p++
                }
            }
            sb.append("$i. ${nm.t}: Present $p | Leave $l | Absent $a").append('\n')
        }
        return sb.toString().trim()
    }
}
