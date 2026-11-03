package com.example.tabletool.ops

import com.example.tabletool.model.Sheet

enum class CrossMode { INTERSECT, UNION }

/**
 * 跨 Sheet 重合筛选。
 * - INTERSECT：以第一张表为基准，保留"关键列的值在所选所有表中都出现"的行（取交集）。
 * - UNION：把所有所选表的行合并去重（取并集）。
 */
fun crossSheetFilter(sheets: List<Sheet>, keyColIndex: Int, mode: CrossMode): Sheet {
    require(sheets.isNotEmpty()) { "至少需要选择一张表" }
    val first = sheets.first()
    val keySets = sheets.map { s -> s.rows.map { it.getOrNull(keyColIndex) ?: "" }.toSet() }
    return when (mode) {
        CrossMode.INTERSECT -> {
            val common = keySets.reduce { a, b -> a.intersect(b) }
            val kept = first.rows.filter { (it.getOrNull(keyColIndex) ?: "") in common }
            Sheet(first.fileName, "重合(交集)_${first.sheetName}", first.headers, kept)
        }
        CrossMode.UNION -> {
            val all = sheets.flatMap { it.rows }.distinct()
            Sheet(first.fileName, "重合(并集)_${first.sheetName}", first.headers, all)
        }
    }
}
