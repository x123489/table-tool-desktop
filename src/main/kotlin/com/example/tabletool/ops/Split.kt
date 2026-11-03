package com.example.tabletool.ops

import com.example.tabletool.model.Sheet

/** 按某列的不同值，把一张表拆成多张结果表（按值排序）。 */
fun splitByColumn(sheet: Sheet, colIndex: Int): List<Sheet> {
    val grouped = sheet.rows.groupBy { it.getOrNull(colIndex) ?: "" }
    return grouped.entries.sortedBy { it.key }.mapIndexed { i, (key, rows) ->
        Sheet(sheet.fileName, "${sanitize(key)}_${i + 1}", sheet.headers, rows)
    }
}

/** 仅按指定的若干值拆分：每个值生成一张结果表（值不在列表中则忽略）。 */
fun splitByColumnValues(sheet: Sheet, colIndex: Int, values: List<String>): List<Sheet> {
    val grouped = sheet.rows.groupBy { it.getOrNull(colIndex) ?: "" }
    return values.mapNotNull { v ->
        grouped[v]?.let { rows -> Sheet(sheet.fileName, sanitize(v), sheet.headers, rows) }
    }
}

/** 把指定值对应的行抽取出来，合并为同一张表（用于"只挑某个部门的数据"）。 */
fun filterByColumnValues(sheet: Sheet, colIndex: Int, values: Set<String>): Sheet {
    val kept = sheet.rows.filter { (it.getOrNull(colIndex) ?: "") in values }
    return Sheet(sheet.fileName, "${sheet.sheetName}(抽取)", sheet.headers, kept)
}

private fun sanitize(s: String) = s.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(28)
