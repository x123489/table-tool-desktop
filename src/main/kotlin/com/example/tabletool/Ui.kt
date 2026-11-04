package com.example.tabletool

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import com.example.tabletool.model.Sheet
import com.example.tabletool.ops.CombineOp
import com.example.tabletool.ops.Condition
import com.example.tabletool.ops.MatchType

val Brand = Color(0xFFF57C00)
val SoftBg = Color(0xFFF5F5F5)
val HeadBg = Color(0xFFFBE9D0)
val CellBorder = Color(0xFFE0E0E0)
val TextMain = Color(0xFF212121)
val TextMuted = Color(0xFF757575)

@Composable
fun DataTable(sheet: Sheet) {
    val hScroll = rememberScrollState()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.horizontalScroll(hScroll).background(HeadBg)) {
            sheet.headers.forEach { h ->
                Box(Modifier.width(130.dp).border(0.5.dp, CellBorder).padding(6.dp)) {
                    Text(h.ifBlank { " " }, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize().horizontalScroll(hScroll).padding(4.dp)) {
            itemsIndexed(sheet.rows) { r, row ->
                Row(Modifier.background(if (r % 2 == 0) SoftBg else Color.White)) {
                    sheet.headers.indices.forEach { i ->
                        Box(Modifier.width(130.dp).border(0.5.dp, CellBorder).padding(6.dp)) {
                            Text((row.getOrNull(i) ?: "").ifBlank { " " }, fontSize = 12.sp, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        if (sheet.rows.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("（该表没有数据行）", fontSize = 11.sp, color = TextMuted)
        }
    }
}

@Composable
fun FilterPanel(
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
            if (sheet != null) ColumnSelector(sheet, ci) { newCi -> ci = newCi; onConditions(conditions.mapIndexed { i, x -> if (i == idx) x.copy(colIndex = newCi) else x }) }
            LabeledDropdown(label = "匹配", options = MatchType.values().map { MATCH_LABELS[it]!! }, selected = mi) {
                newMi -> mi = newMi; onConditions(conditions.mapIndexed { i, x -> if (i == idx) x.copy(match = MatchType.values()[newMi]) else x })
            }
            OutlinedTextField(value = kw, onValueChange = { kw = it; onConditions(conditions.mapIndexed { i, x -> if (i == idx) x.copy(keyword = kw) else x }) }, singleLine = true, modifier = Modifier.weight(1f).padding(start = 4.dp), placeholder = { Text("关键字") }, textStyle = TextStyle(fontSize = 12.sp))
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
fun ColumnSelector(sheet: Sheet, value: Int, onPick: (Int) -> Unit, allowAny: Boolean = true) {
    val options = if (allowAny) listOf("(任意列)") + sheet.headers else sheet.headers
    val maxIdx = (sheet.headers.size - 1).coerceAtLeast(0)
    val selected = if (allowAny) (if (value < 0) 0 else (value + 1).coerceAtMost(options.size - 1)) else value.coerceIn(0, maxIdx)
    LabeledDropdown(label = "列", options = options, selected = selected) { onPick(if (allowAny) (if (it == 0) -1 else it - 1) else it) }
}

@Composable
fun LabeledDropdown(label: String, options: List<String>, selected: Int, onSelected: (Int) -> Unit) {
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
