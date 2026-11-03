package com.example.tabletool.ops

import com.example.tabletool.model.Sheet

enum class MatchType { CONTAINS, EQUALS, STARTS, ENDS, REGEX, NOT_CONTAINS }

enum class CombineOp { AND, OR }

/** 单条筛选条件。colIndex = -1 表示"任意列"。 */
data class Condition(
    val colIndex: Int,
    val match: MatchType,
    val keyword: String,
    val caseSensitive: Boolean = false
)

/** 一组条件的组合规则。 */
data class FilterRule(
    val conditions: List<Condition>,
    val combine: CombineOp
)

/** 按规则筛选一张表。 */
fun Sheet.filter(rule: FilterRule): Sheet {
    if (rule.conditions.isEmpty()) return this
    val matched = rows.filter { row ->
        val res = rule.conditions.map { matchRow(row, it) }
        if (rule.combine == CombineOp.AND) res.all { it } else res.any { it }
    }
    return Sheet(fileName, "$sheetName(筛选)", headers, matched)
}

private fun matchRow(row: List<String>, cond: Condition): Boolean {
    val test: (String) -> Boolean = { cell ->
        val a = if (cond.caseSensitive) cell else cell.lowercase()
        val b = if (cond.caseSensitive) cond.keyword else cond.keyword.lowercase()
        when (cond.match) {
            MatchType.CONTAINS -> a.contains(b)
            MatchType.EQUALS -> a == b
            MatchType.STARTS -> a.startsWith(b)
            MatchType.ENDS -> a.endsWith(b)
            MatchType.REGEX -> try {
                val opts = if (cond.caseSensitive) setOf<RegexOption>() else setOf(RegexOption.IGNORE_CASE)
                Regex(cond.keyword, opts).containsMatchIn(cell)
            } catch (e: Exception) { false }
            MatchType.NOT_CONTAINS -> !a.contains(b)
        }
    }
    return if (cond.colIndex < 0) row.any { test(it) }
    else test(row.getOrNull(cond.colIndex) ?: "")
}
