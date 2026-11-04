package com.example.tabletool

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tabletool.model.ImportedFile
import com.example.tabletool.model.Sheet
import com.example.tabletool.model.id

@Composable
fun PreviewPane(
    sheet: Sheet?,
    currentFile: ImportedFile?,
    selectedId: String?,
    showResult: Boolean,
    resultCount: Int,
    onShowOriginal: () -> Unit,
    onSelectSheet: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (showResult) "结果预览（共 $resultCount 个结果表，显示第一个）" else "原表预览",
                fontWeight = FontWeight.Bold, color = TextMain, fontSize = 13.sp
            )
            Spacer(Modifier.fillMaxWidth().weight(1f))
            if (showResult) TextButton(onClick = onShowOriginal) { Text("看原表", color = Brand, fontSize = 12.sp) }
        }
        Spacer(Modifier.height(6.dp))
        // 当前文件若有多个 Sheet，给出切换标签
        if (currentFile != null && currentFile.sheets.size > 1 && !showResult) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 4.dp)) {
                currentFile.sheets.forEach { s ->
                    val sel = s.id() == selectedId
                    Box(
                        Modifier.padding(end = 6.dp)
                            .background(if (sel) Brand else SoftBg, RoundedCornerShape(6.dp))
                            .clickable { onSelectSheet(s.id()) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(s.sheetName, fontSize = 12.sp, color = if (sel) Color.White else TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (sheet == null) Text("请选择左侧的表进行预览。", color = TextMuted)
        else DataTable(sheet)
    }
}
