package com.example.tabletool

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

@Composable
fun FileList(
    files: List<ImportedFile>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onRemove: (ImportedFile) -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("导入的表（${files.sumOf { it.sheets.size }}）", fontWeight = FontWeight.Bold, color = TextMain, fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        if (files.isEmpty()) Text("暂无，点右上角“导入”。", color = TextMuted, fontSize = 12.sp)
        files.forEach { f ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(f.fileName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Brand, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                TextButton(onClick = { onRemove(f) }, modifier = Modifier.width(36.dp).padding(0.dp)) { Text("×", color = Color(0xFFD32F2F), fontSize = 16.sp) }
            }
            f.sheets.forEach { s ->
                val sid = s.id()
                val sel = sid == selectedId
                Row(
                    Modifier.fillMaxWidth().padding(start = 8.dp)
                        .background(if (sel) HeadBg else Color.Transparent)
                        .clickable { onSelect(sid) },
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
