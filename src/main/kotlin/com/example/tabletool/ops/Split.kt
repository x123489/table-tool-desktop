package com.example.tabletool.ops

import com.example.tabletool.model.Sheet

/** 按某列的不同值，把一张表拆成多张结果表（按值排序）。 */
fun splitByColumn(sheet: Sheet, colIndex: Int): List<Sheet> {
    val grouped = sheet.rows.groupBy { it.getOrNull(colIndex) ?: "" }
    return grouped.entries.sortedBy { it.key }.mapIndexed { i, (key, rows) ->
        Sheet(sheet.fileName, "${sanitize(key)}_${i + 1}", sheet.headers, rows)
    }
}

private fun sanitize(s: String) = s.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(28)
