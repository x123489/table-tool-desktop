package com.example.tabletool

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tabletool.model.Sheet
import com.example.tabletool.model.id
import com.example.tabletool.ops.CombineOp
import com.example.tabletool.ops.CrossMode
import com.example.tabletool.ops.MatchType

@Composable
fun ColumnScope.OperationPanel(
    mode: Mode,
    onModeChange: (Mode) -> Unit,
    conditions: List<com.example.tabletool.ops.Condition>,
    onConditionsChange: (List<com.example.tabletool.ops.Condition>) -> Unit,
    combine: CombineOp,
    onCombineChange: (CombineOp) -> Unit,
    splitCol: Int,
    onSplitColChange: (Int) -> Unit,
    splitMode: Int,
    onSplitModeChange: (Int) -> Unit,
    splitSelectedValues: Set<String>,
    onSplitSelectedValuesChange: (Set<String>) -> Unit,
    mergeKeyCol: Int,
    onMergeKeyColChange: (Int) -> Unit,
    crossKeyCol: Int,
    onCrossKeyColChange: (Int) -> Unit,
    crossMode: CrossMode,
    onCrossModeChange: (CrossMode) -> Unit,
    selectedIds: Set<String>,
    onToggleSelectedId: (String) -> Unit,
    allSheets: List<Sheet>,
    selectedSheet: Sheet?,
    targetSheets: List<Sheet>
) {
    LabeledDropdown(
        label = "操作模式",
        options = Mode.values().map { it.label },
        selected = Mode.values().indexOf(mode),
        onSelected = { onModeChange(Mode.values()[it]) }
    )
    Spacer(Modifier.height(10.dp))

    when (mode) {
        Mode.FILTER, Mode.BATCH_FILTER -> FilterPanel(conditions, combine, selectedSheet ?: targetSheets.firstOrNull(), onConditionsChange, onCombineChange)
        Mode.SPLIT -> {
            Text("拆分依据列", fontSize = 12.sp, color = TextMuted)
            val s = selectedSheet
            if (s != null) ColumnSelector(s, splitCol, onSplitColChange, allowAny = false)
            else Text("请选择一张表", color = TextMuted, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Text("拆分方式", fontSize = 12.sp, color = TextMuted)
            Row {
                listOf("按全部值拆" to 0, "按选定值拆" to 1, "选定值合一" to 2).forEach { (t, v) ->
                    OutlinedButton(onClick = { onSplitModeChange(v) }, Modifier.padding(end = 4.dp), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = if (splitMode == v) Brand else TextMuted)) { Text(t, fontSize = 11.sp) }
                }
            }
            if (splitMode != 0 && s != null) {
                Spacer(Modifier.height(6.dp))
                val vals = s.rows.map { it.getOrNull(splitCol) ?: "" }.distinct().sorted()
                Text("勾选要处理的值（共 ${vals.size} 个，可滚动）", fontSize = 11.sp, color = TextMuted)
                Column(Modifier.fillMaxWidth().height(150.dp).verticalScroll(rememberScrollState())) {
                    vals.forEach { v ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = v in splitSelectedValues, onCheckedChange = {
                                onSplitSelectedValuesChange(if (it) splitSelectedValues + v else splitSelectedValues - v)
                            })
                            Text(v.ifBlank { "(空值)" }, fontSize = 12.sp, color = TextMain, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }
                    }
                }
                if (splitMode == 1) Text("→ 每个勾选值各生成一张表（如：财务部/人事部/销售部各一张）", fontSize = 10.sp, color = TextMuted)
                else Text("→ 勾选值对应的行合并为一张表（如：只抽出人事部）", fontSize = 10.sp, color = TextMuted)
            }
        }
        Mode.BATCH_SPLIT -> {
            Text("拆分依据列", fontSize = 12.sp, color = TextMuted)
            val s = selectedSheet ?: targetSheets.firstOrNull()
            if (s != null) ColumnSelector(s, splitCol, onSplitColChange, allowAny = false)
            else Text("请选择一张表", color = TextMuted, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text("将对每一张导入的表按该列全部值拆分。", fontSize = 11.sp, color = TextMuted)
        }
        Mode.MERGE_APPEND, Mode.MERGE_BY_KEY, Mode.CROSS_INTERSECT, Mode.CROSS_UNION -> {
            Text("选择参与的多表（不勾选=全部）", fontSize = 12.sp, color = TextMuted)
            Spacer(Modifier.height(4.dp))
            Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                allSheets.forEach { s ->
                    val sid = s.id()
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = sid in selectedIds, onCheckedChange = { onToggleSelectedId(sid) })
                        Text(s.id(), fontSize = 11.sp, color = TextMain, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }
            }
            if (mode.needsKeyCol) {
                Spacer(Modifier.height(8.dp))
                Text("关键列（用于对齐/关联，如“姓名”）", fontSize = 12.sp, color = TextMuted)
                val s = targetSheets.firstOrNull() ?: selectedSheet
                if (s != null) ColumnSelector(s, if (mode == Mode.MERGE_BY_KEY) mergeKeyCol else crossKeyCol, {
                    if (mode == Mode.MERGE_BY_KEY) onMergeKeyColChange(it) else onCrossKeyColChange(it)
                }, allowAny = false)
            }
            if (mode == Mode.CROSS_INTERSECT || mode == Mode.CROSS_UNION) {
                Spacer(Modifier.height(8.dp))
                Row {
                    listOf(CrossMode.INTERSECT to "交集", CrossMode.UNION to "并集").forEach { (m, t) ->
                        OutlinedButton(onClick = { onCrossModeChange(m) }, Modifier.padding(end = 6.dp), colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = if (crossMode == m) Brand else TextMuted)) { Text(t, fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}
