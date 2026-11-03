package com.example.tabletool.ops

import com.example.tabletool.model.Sheet

enum class MergeMode { APPEND, BY_KEY }

/** 纵向拼接：所有表按表头名对齐，缺失列填空。 */
fun mergeAppend(sheets: List<Sheet>): Sheet {
    require(sheets.isNotEmpty()) { "至少需要选择一张表" }
    val base = sheets.first()
    val headers = base.headers.toMutableList()
    sheets.forEach { s -> s.headers.forEach { h -> if (h !in headers) headers.add(h) } }
    val out = mutableListOf<List<String>>()
    sheets.forEach { s ->
        val idxMap = s.headers.mapIndexed { i, h -> h to i }.toMap()
        s.rows.forEach { row ->
            val newRow = MutableList(headers.size) { "" }
            headers.forEachIndexed { hi, h ->
                val si = idxMap[h]
                if (si != null) newRow[hi] = row.getOrNull(si) ?: ""
            }
            out.add(newRow)
        }
    }
    return Sheet(base.fileName, "合并(纵向)", headers, out)
}

/** 按关键列 join：相同关键值的行横向合并（外连接），其余列追加（关键列不重复）。 */
fun mergeByKey(sheets: List<Sheet>, keyColIndex: Int): Sheet {
    require(sheets.isNotEmpty()) { "至少需要选择一张表" }
    val base = sheets.first()
    val outHeaders = base.headers.toMutableList()
    sheets.drop(1).forEach { s ->
        s.headers.forEachIndexed { i, h -> if (i != keyColIndex && h !in outHeaders) outHeaders.add(h) }
    }
    val baseKeyName = base.headers.getOrNull(keyColIndex) ?: ""
    val map = LinkedHashMap<String, MutableList<String>>()
    sheets.forEach { s ->
        val otherHeaders = s.headers.mapIndexed { i, h -> i to h }.filter { it.first != keyColIndex }
        s.rows.forEach { row ->
            val key = row.getOrNull(keyColIndex) ?: ""
            var rec = map[key]
            if (rec == null) {
                rec = MutableList(outHeaders.size) { "" }
                val baseKeyIdx = outHeaders.indexOf(baseKeyName)
                if (baseKeyIdx >= 0) rec[baseKeyIdx] = key
                map[key] = rec
            }
            otherHeaders.forEach { (ci, h) ->
                val oi = outHeaders.indexOf(h)
                if (oi >= 0) rec[oi] = row.getOrNull(ci) ?: ""
            }
        }
    }
    return Sheet(base.fileName, "合并(按关键列)", outHeaders, map.values.toList())
}
