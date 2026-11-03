package com.example.tabletool

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tabletool.io.Exporter
import com.example.tabletool.io.Importer
import com.example.tabletool.model.ImportedFile
import com.example.tabletool.model.Sheet
import com.example.tabletool.model.id
import com.example.tabletool.ops.CombineOp
import com.example.tabletool.ops.Condition
import com.example.tabletool.ops.CrossMode
import com.example.tabletool.ops.MatchType
import com.example.tabletool.ops.MergeMode
import com.example.tabletool.ops.crossSheetFilter
import com.example.tabletool.ops.filter
import com.example.tabletool.ops.mergeAppend
import com.example.tabletool.ops.mergeByKey
import com.example.tabletool.ops.FilterRule
import com.example.tabletool.ops.splitByColumn
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

private val Brand = Color(0xFFF57C00)
private val SoftBg = Color(0xFFF5F5F5)
private val HeadBg = Color(0xFFFBE9D0)
private val CellBorder = Color(0xFFE0E0E0)
private val TextMain = Color(0xFF212121)
private val TextMuted = Color(0xFF757575)

private enum class Mode(
    val label: String,
    val multiSheet: Boolean = false,
    val needsKeyCol: Boolean = false
) {
    FILTER("筛选（单表）"),
    SPLIT("按列拆分（单表）"),
    MERGE_APPEND("合并：纵向拼接", multiSheet = true),
    MERGE_BY_KEY("合并：按关键列", multiSheet = true, needsKeyCol = true),
    CROSS_INTERSECT("重合筛选：交集", multiSheet = true, needsKeyCol = true),
    CROSS_UNION("重合筛选：并集", multiSheet = true),
    BATCH_FILTER("批量：对所有表筛选"),
    BATCH_SPLIT("批量：对所有表拆分")
}

private val MATCH_LABELS = mapOf(
    MatchType.CONTAINS to "包含",
    MatchType.EQUALS to "等于",
    MatchType.STARTS to "开头是",
    MatchType.ENDS to "结尾是",
    MatchType.REGEX to "正则",
    MatchType.NOT_CONTAINS to "不包含"
)

@Composable
fun App() {
    var files by remember { mutableStateOf(listOf<ImportedFile>()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var mode by remember { mutableStateOf(Mode.FILTER) }

    var conditions by remember { mutableStateOf(listOf<Condition>()) }
    var combine by remember { mutableStateOf(CombineOp.AND) }
    var splitCol by remember { mutableStateOf(0) }
    var mergeKeyCol by remember { mutableStateOf(0) }
    var crossKeyCol by remember { mutableStateOf(0) }
    var crossMode by remember { mutableStateOf(CrossMode.INTERSECT) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    var results by remember { mutableStateOf(listOf<Sheet>()) }
    var showResult by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("请先导入表格文件。") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val allSheets: List<Sheet> = files.flatMap { it.sheets }
    val selectedSheet: Sheet? = allSheets.firstOrNull { it.id() == selectedId }
    val targetSheets: List<Sheet> =
        if (selectedIds.isEmpty()) allSheets else allSheets.filter { it.id() in selectedIds }

    fun pickFiles() {
        val fd = FileDialog(null as Frame?, "选择表格文件（可多选）", FileDialog.LOAD)
        fd.isMultipleMode = true
        fd.setFilenameFilter { _, n -> n.lowercase().let { it.endsWith(".xlsx") || it.endsWith(".xls") || it.endsWith(".csv") } }
        fd.isVisible = true
        val picked = (fd.files?.toList() ?: emptyList()).filter { it.exists() }
        if (picked.isEmpty()) return
        val imported = picked.mapNotNull { f ->
            try { Importer.import(f) } catch (e: Exception) { errorMsg = "导入失败：${f.name} -> ${e.message}"; null }
        }
        files = files + imported
        if (selectedId == null) imported.firstOrNull()?.sheets?.firstOrNull()?.let { selectedId = it.id() }
        status = "已导入 ${files.size} 个文件，共 ${allSheets.size} 个表。"
    }

    fun apply() {
        try {
            val out = when (mode) {
                Mode.FILTER -> {
                    val s = selectedSheet ?: throw IllegalStateException("请先在左侧选择一张表")
                    listOf(s.filter(FilterRule(conditions, combine)))
                }
                Mode.SPLIT -> {
                    val s = selectedSheet ?: throw IllegalStateException("请先在左侧选择一张表")
                    splitByColumn(s, splitCol.coerceIn(0, (s.headers.size - 1).coerceAtLeast(0)))
                }
                Mode.MERGE_APPEND -> listOf(mergeAppend(targetSheets))
                Mode.MERGE_BY_KEY -> listOf(mergeByKey(targetSheets, mergeKeyCol.coerceIn(0, (targetSheets.firstOrNull()?.headers?.size ?: 1) - 1)))
                Mode.CROSS_INTERSECT -> listOf(crossSheetFilter(targetSheets, crossKeyCol.coerceIn(0, (targetSheets.firstOrNull()?.headers?.size ?: 1) - 1), CrossMode.INTERSECT))
                Mode.CROSS_UNION -> listOf(crossSheetFilter(targetSheets, crossKeyCol.coerceIn(0, (targetSheets.firstOrNull()?.headers?.size ?: 1) - 1), CrossMode.UNION))
                Mode.BATCH_FILTER -> allSheets.map { it.filter(FilterRule(conditions, combine)) }
                Mode.BATCH_SPLIT -> allSheets.flatMap { splitByColumn(it, splitCol.coerceIn(0, (it.headers.size - 1).coerceAtLeast(0))) }
            }
            results = out
            showResult = true
            val total = out.sumOf { it.rows.size }
            status = "处理完成：${out.size} 个结果表，合计 $total 行。"
        } catch (e: Exception) {
            errorMsg = "处理出错：${e.message}"
        }
    }

    fun export() {
        val src = if (showResult && results.isNotEmpty()) results else (selectedSheet?.let { listOf(it) } ?: emptyList())
        if (src.isEmpty()) { errorMsg = "没有可导出的内容。"; return }
        val fd = FileDialog(null as Frame?, "保存结果", FileDialog.SAVE)
        fd.setFilenameFilter { _, n -> n.lowercase().let { it.endsWith(".xlsx") || it.endsWith(".csv") } }
        fd.isVisible = true
        val f = fd.file ?: return
        var name = f
        if (!name.lowercase().endsWith(".xlsx") && !name.lowercase().endsWith(".csv")) name += ".xlsx"
        val out = File(fd.directory ?: System.getProperty("user.home"), name)
        try {
            Exporter.export(src, out)
            status = "已导出到：${out.absolutePath}"
        } catch (e: Exception) {
            errorMsg = "导出失败：${e.message}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Brand),
                title = { Text("表格处理工具 · 单机版", color = Color.White, fontWeight = FontWeight.Bold) },
                actions = {
                    Button(onClick = { pickFiles() }, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Brand)) { Text("导入") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { export() }, colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Text("导出") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxSize().weight(1f)) {
                // 左侧：文件 / Sheet 列表
                Card(
                    Modifier.fillMaxHeight().width(240.dp).padding(8.dp),
                    shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp)) {
                        Text("导入的表（${allSheets.size}）", fontWeight = FontWeight.Bold, color = TextMain, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        if (allSheets.isEmpty()) Text("暂无，点右上角“导入”。", color = TextMuted, fontSize = 12.sp)
                        files.forEach { f ->
                            Text(f.fileName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Brand, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            f.sheets.forEach { s ->
                                val sid = s.id()
                                val sel = sid == selectedId
                                Row(
                                    Modifier.fillMaxWidth().padding(start = 8.dp)
                                        .background(if (sel) HeadBg else Color.Transparent)
                                        .clickable { selectedId = sid; showResult = false },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(s.sheetName, fontSize = 12.sp, color = if (sel) Brand else TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    Text("${s.rows.size}", fontSize = 10.sp, color = TextMuted)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }

                // 中间：预览
                Card(
                    Modifier.fillMaxHeight().weight(1f).padding(8.dp),
                    shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    val sheet = if (showResult && results.isNotEmpty()) results.first() else selectedSheet
                    Column(Modifier.fillMaxSize().padding(10.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (showResult) "结果预览（共 ${results.size} 个结果表，显示第一个）" else "原表预览", fontWeight = FontWeight.Bold, color = TextMain, fontSize = 13.sp)
                            Spacer(Modifier.weight(1f))
                            if (showResult) TextButton(onClick = { showResult = false }) { Text("看原表", color = Brand, fontSize = 12.sp) }
                        }
                        Spacer(Modifier.height(6.dp))
                        if (sheet == null) Text("请选择左侧的表进行预览。", color = TextMuted)
                        else DataTable(sheet)
                    }
                }

                // 右侧：操作面板
                Card(
                    Modifier.fillMaxHeight().width(320.dp).padding(8.dp),
                    shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                        LabeledDropdown(
                            label = "操作模式",
                            options = Mode.values().map { it.label },
                            selected = Mode.values().indexOf(mode),
                            onSelected = { mode = Mode.values()[it] }
                        )
                        Spacer(Modifier.height(10.dp))

                        when (mode) {
                            Mode.FILTER, Mode.BATCH_FILTER -> FilterPanel(conditions, combine, selectedSheet ?: targetSheets.firstOrNull(), { conditions = it }, { combine = it })
                            Mode.SPLIT, Mode.BATCH_SPLIT -> {
                                Text("拆分依据列", fontSize = 12.sp, color = TextMuted)
                                val s = selectedSheet ?: targetSheets.firstOrNull()
                                if (s != null) ColumnSelector(s, splitCol) { splitCol = it }
                                else Text("请选择一张表", color = TextMuted, fontSize = 12.sp)
                            }
                            Mode.MERGE_APPEND, Mode.MERGE_BY_KEY, Mode.CROSS_INTERSECT, Mode.CROSS_UNION -> {
                                Text("选择参与的多表（不勾选=全部）", fontSize = 12.sp, color = TextMuted)
                                Spacer(Modifier.height(4.dp))
                                Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                                    allSheets.forEach { s ->
                                        val sid = s.id()
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                            Checkbox(checked = sid in selectedIds, onCheckedChange = {
                                                selectedIds = if (it) selectedIds + sid else selectedIds - sid
                                            })
                                            Text(s.id(), fontSize = 11.sp, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                                if (mode.needsKeyCol) {
                                    Spacer(Modifier.height(8.dp))
                                    Text("关键列", fontSize = 12.sp, color = TextMuted)
                                    val s = targetSheets.firstOrNull() ?: selectedSheet
                                    if (s != null) ColumnSelector(s, if (mode == Mode.MERGE_BY_KEY) mergeKeyCol else crossKeyCol) {
                                        if (mode == Mode.MERGE_BY_KEY) mergeKeyCol = it else crossKeyCol = it
                                    }
                                }
                                if (mode == Mode.CROSS_INTERSECT || mode == Mode.CROSS_UNION) {
                                    Spacer(Modifier.height(8.dp))
                                    Row {
                                        listOf(CrossMode.INTERSECT to "交集", CrossMode.UNION to "并集").forEach { (m, t) ->
                                            OutlinedButton(onClick = { crossMode = m }, Modifier.padding(end = 6.dp), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = if (crossMode == m) Brand else TextMuted)) { Text(t, fontSize = 12.sp) }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Button(onClick = { apply() }, Modifier.fillMaxWidth(), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Brand)) { Text("应用 / 生成结果", color = Color.White) }
                    }
                }
            }
            // 底部状态栏
            HorizontalDivider()
            Text(status, fontSize = 12.sp, color = TextMuted, modifier = Modifier.fillMaxWidth().padding(8.dp))
        }
    }

    if (errorMsg != null) {
        AlertDialog(
            onDismissRequest = { errorMsg = null },
            confirmButton = { TextButton(onClick = { errorMsg = null }) { Text("知道了") } },
            title = { Text("提示") },
            text = { Text(errorMsg ?: "") }
        )
    }
}

@Composable
private fun FilterPanel(
    conditions: List<Condition>,
    combine: CombineOp,
    sheet: Sheet?,
    onConditions: (List<Condition>) -> Unit,
    onCombine: (CombineOp) -> Unit
) {
    Text("筛选条件（列 + 匹配 + 关键字）", fontSize = 12.sp, color = TextMuted)
    Spacer(Modifier.height(4.dp))
    conditions.forEachIndexed { idx, c ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            var ci by remember(c, sheet) { mutableStateOf(c.colIndex) }
            var mi by remember(c) { mutableStateOf(MatchType.values().indexOf(c.match)) }
            var kw by remember(c) { mutableStateOf(c.keyword) }
            if (sheet != null) ColumnSelector(sheet, ci) { ci = it; onConditions(conditions.mapIndexed { i, x -> if (i == idx) x.copy(colIndex = it) else x }) }
            LabeledDropdown(label = "匹配", options = MatchType.values().map { MATCH_LABELS[it]!! }, selected = mi) {
                mi = it; onConditions(conditions.mapIndexed { i, x -> if (i == idx) x.copy(match = MatchType.values()[it]) else x })
            }
            OutlinedTextField(value = kw, onValueChange = { kw = it; onConditions(conditions.mapIndexed { i, x -> if (i == idx) x.copy(keyword = it) else x }) }, singleLine = true, modifier = Modifier.weight(1f).padding(start = 4.dp), placeholder = { Text("关键字") }, textStyle = TextStyle(fontSize = 12.sp))
            TextButton(onClick = { onConditions(conditions.filterIndexed { i, _ -> i != idx }) }) { Text("×", color = Color(0xFFD32F2F)) }
        }
        Spacer(Modifier.height(4.dp))
    }
    Button(onClick = {
        val next = Condition(0, MatchType.CONTAINS, "")
        onConditions(conditions + next)
    }, Modifier.fillMaxWidth()) { Text("+ 添加条件") }

    Spacer(Modifier.height(8.dp))
    Text("多条件关系", fontSize = 12.sp, color = TextMuted)
    Row {
        OutlinedButton(onClick = { onCombine(CombineOp.AND) }, Modifier.padding(end = 6.dp), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = if (combine == CombineOp.AND) Brand else TextMuted)) { Text("并且 AND", fontSize = 12.sp) }
        OutlinedButton(onClick = { onCombine(CombineOp.OR) }, colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = if (combine == CombineOp.OR) Brand else TextMuted)) { Text("或者 OR", fontSize = 12.sp) }
    }
}

@Composable
private fun DataTable(sheet: Sheet, maxRows: Int = 500) {
    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()
    val shown = if (sheet.rows.size > maxRows) sheet.rows.take(maxRows) else sheet.rows
    Column(Modifier.fillMaxSize().verticalScroll(vScroll).horizontalScroll(hScroll).padding(4.dp)) {
        Row(Modifier.background(HeadBg)) {
            sheet.headers.forEach { h ->
                Box(Modifier.width(120.dp).border(0.5.dp, CellBorder).padding(6.dp)) {
                    Text(h.ifBlank { "　" }, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        shown.forEach { row ->
            Row(Modifier.background(SoftBg)) {
                sheet.headers.indices.forEach { i ->
                    Box(Modifier.width(120.dp).border(0.5.dp, CellBorder).padding(6.dp)) {
                        Text((row.getOrNull(i) ?: "").ifBlank { "　" }, fontSize = 12.sp, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (sheet.rows.size > maxRows) {
            Spacer(Modifier.height(6.dp))
            Text("（仅预览前 $maxRows / 共 ${sheet.rows.size} 行）", fontSize = 11.sp, color = TextMuted)
        }
    }
}

@Composable
private fun ColumnSelector(sheet: Sheet, value: Int, onPick: (Int) -> Unit) {
    val options = listOf("(任意列)") + sheet.headers
    val selected = if (value < 0) 0 else value + 1
    LabeledDropdown(label = "列", options = options, selected = selected) { onPick(if (it == 0) -1 else it - 1) }
}

@Composable
private fun LabeledDropdown(label: String, options: List<String>, selected: Int, onSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val safeSel = selected.coerceIn(0, (options.size - 1).coerceAtLeast(0))
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        TextField(
            value = options.getOrNull(safeSel) ?: "",
            onValueChange = {}, readOnly = true, label = { Text(label, fontSize = 12.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, o -> DropdownMenuItem(text = { Text(o, fontSize = 12.sp) }, onClick = { onSelected(i); expanded = false }) }
        }
    }
}
