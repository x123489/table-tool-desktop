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

/** 按关键列 join：相同关键值的行横向合并（外连接），其余列追加（关键列不重复）。
 *  keyColName 按“列名”对齐，因此不同表的同名列即使列序不同也能正确关联（如工资表+履历表按“姓名”合并）。 */
fun mergeByKey(sheets: List<Sheet>, keyColName: String): Sheet {
    require(sheets.isNotEmpty()) { "至少需要选择一张表" }
    val base = sheets.first()
    // 每张表各自解析关键列索引（列序不同也能对齐）
    val keyIdx = sheets.map { s -> val i = s.headers.indexOf(keyColName); if (i < 0) 0 else i }
    val outHeaders = base.headers.toMutableList()
    sheets.forEachIndexed { si, s ->
        if (si == 0) return@forEachIndexed
        s.headers.forEachIndexed { i, h -> if (i != keyIdx[si] && h !in outHeaders) outHeaders.add(h) }
    }
    val baseKeyName = base.headers.getOrNull(keyIdx[0]) ?: keyColName
    val map = LinkedHashMap<String, MutableList<String>>()
    sheets.forEachIndexed { si, s ->
        val ki = keyIdx[si]
        val otherHeaders = s.headers.mapIndexed { i, h -> i to h }.filter { it.first != ki }
        s.rows.forEach { row ->
            val k = row.getOrNull(ki) ?: ""
            var rec = map[k]
            if (rec == null) {
                rec = MutableList(outHeaders.size) { "" }
                val baseKeyIdx = outHeaders.indexOf(baseKeyName)
                if (baseKeyIdx >= 0) rec[baseKeyIdx] = k
                map[k] = rec
            }
            otherHeaders.forEach { (ci, h) ->
                val oi = outHeaders.indexOf(h)
                if (oi >= 0) rec[oi] = row.getOrNull(ci) ?: ""
            }
        }
    }
    return Sheet(base.fileName, "合并(按关键列)", outHeaders, map.values.toList())
}
