package com.example.tabletool

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
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
import com.example.tabletool.ops.crossSheetFilter
import com.example.tabletool.ops.filter
import com.example.tabletool.ops.mergeAppend
import com.example.tabletool.ops.mergeByKey
import com.example.tabletool.ops.FilterRule
import com.example.tabletool.ops.filterByColumnValues
import com.example.tabletool.ops.splitByColumn
import com.example.tabletool.ops.splitByColumnValues
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

internal enum class Mode(
    val label: String,
    val multiSheet: Boolean = false,
    val needsKeyCol: Boolean = false
) {
    FILTER("筛选（单表）"),
    SPLIT("按列拆分（单表）"),
    MERGE_APPEND("合并：纵向拼接（多表/多Sheet）", multiSheet = true),
    MERGE_BY_KEY("合并：按关键列关联（多表/多Sheet）", multiSheet = true, needsKeyCol = true),
    CROSS_INTERSECT("重合筛选：交集（多表）", multiSheet = true, needsKeyCol = true),
    CROSS_UNION("重合筛选：并集（多表）", multiSheet = true),
    BATCH_FILTER("批量：对所有表筛选"),
    BATCH_SPLIT("批量：对所有表拆分")
}

internal val MATCH_LABELS = mapOf(
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
    var splitMode by remember { mutableStateOf(0) }
    var splitSelectedValues by remember { mutableStateOf(setOf<String>()) }
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
    val currentFile: ImportedFile? = files.firstOrNull { it.sheets.any { s -> s.id() == selectedId } }
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

    fun removeFile(f: ImportedFile) {
        val newFiles = files.filter { it !== f }
        files = newFiles
        val remaining = newFiles.flatMap { it.sheets }.map { it.id() }.toSet()
        if (selectedId != null && selectedId !in remaining) selectedId = null
        selectedIds = selectedIds.intersect(remaining)
        if (showResult && selectedSheet == null) showResult = false
        status = "已移除：${f.fileName}（如需重导，点右上角“导入”重新选择）"
    }

    fun clearAll() {
        files = emptyList()
        selectedId = null
        selectedIds = emptySet()
        results = emptyList()
        showResult = false
        status = "已清空所有已导入的表。"
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
                    val col = splitCol.coerceIn(0, (s.headers.size - 1).coerceAtLeast(0))
                    when (splitMode) {
                        0 -> splitByColumn(s, col)
                        1 -> {
                            if (splitSelectedValues.isEmpty()) throw IllegalStateException("请在右侧勾选要拆分的值")
                            splitByColumnValues(s, col, splitSelectedValues.toList())
                        }
                        else -> {
                            if (splitSelectedValues.isEmpty()) throw IllegalStateException("请在右侧勾选要抽取的值")
                            listOf(filterByColumnValues(s, col, splitSelectedValues))
                        }
                    }
                }
                Mode.MERGE_APPEND -> listOf(mergeAppend(targetSheets))
                Mode.MERGE_BY_KEY -> {
                    val keyName = targetSheets.firstOrNull()?.headers?.getOrNull(mergeKeyCol) ?: ""
                    listOf(mergeByKey(targetSheets, keyName))
                }
                Mode.CROSS_INTERSECT -> {
                    val keyName = targetSheets.firstOrNull()?.headers?.getOrNull(crossKeyCol) ?: ""
                    listOf(crossSheetFilter(targetSheets, keyName, CrossMode.INTERSECT))
                }
                Mode.CROSS_UNION -> {
                    val keyName = targetSheets.firstOrNull()?.headers?.getOrNull(crossKeyCol) ?: ""
                    listOf(crossSheetFilter(targetSheets, keyName, CrossMode.UNION))
                }
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
                    Button(onClick = { pickFiles() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Brand)) { Text("导入") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { export() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Text("导出") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { clearAll() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Text("清空") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxSize().weight(1f)) {
                // 左侧：文件 / Sheet 列表
                Card(
                    Modifier.fillMaxHeight().width(220.dp).padding(8.dp),
                    shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    FileList(files, selectedId, { selectedId = it; showResult = false }, { removeFile(it) })
                }

                // 中间：预览
                Card(
                    Modifier.fillMaxHeight().weight(1f).padding(8.dp),
                    shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    val sheet = if (showResult && results.isNotEmpty()) results.first() else selectedSheet
                    PreviewPane(
                        sheet = sheet,
                        currentFile = currentFile,
                        selectedId = selectedId,
                        showResult = showResult,
                        resultCount = results.size,
                        onShowOriginal = { showResult = false },
                        onSelectSheet = { selectedId = it; showResult = false }
                    )
                }

                // 右侧：操作面板
                Card(
                    Modifier.fillMaxHeight().width(310.dp).padding(8.dp),
                    shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                        OperationPanel(
                            mode = mode,
                            onModeChange = { mode = it },
                            conditions = conditions,
                            onConditionsChange = { conditions = it },
                            combine = combine,
                            onCombineChange = { combine = it },
                            splitCol = splitCol,
                            onSplitColChange = { splitCol = it },
                            splitMode = splitMode,
                            onSplitModeChange = { splitMode = it },
                            splitSelectedValues = splitSelectedValues,
                            onSplitSelectedValuesChange = { splitSelectedValues = it },
                            mergeKeyCol = mergeKeyCol,
                            onMergeKeyColChange = { mergeKeyCol = it },
                            crossKeyCol = crossKeyCol,
                            onCrossKeyColChange = { crossKeyCol = it },
                            crossMode = crossMode,
                            onCrossModeChange = { crossMode = it },
                            selectedIds = selectedIds,
                            onToggleSelectedId = { sid -> selectedIds = if (sid in selectedIds) selectedIds - sid else selectedIds + sid },
                            allSheets = allSheets,
                            selectedSheet = selectedSheet,
                            targetSheets = targetSheets
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = { apply() }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text("应用 / 生成结果", color = Color.White) }
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
