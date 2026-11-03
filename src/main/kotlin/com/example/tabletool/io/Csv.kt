package com.example.tabletool.io

import java.io.File
import java.nio.charset.StandardCharsets

/**
 * 轻量 CSV 读写（RFC 4180 子集）：
 * - 支持带引号的字段、字段内逗号/换行、双引号转义（""）。
 * - 读：自动去掉 UTF-8 BOM；按 \n 或 \r\n 分行。
 * - 写：以 UTF-8 + BOM 写出，Excel（含中文 Windows）可直接正确打开。
 */
object CsvUtil {

    fun readAll(file: File): List<List<String>> {
        val raw = file.readText(StandardCharsets.UTF_8).removePrefix("\uFEFF")
        if (raw.isBlank()) return emptyList()
        return parse(raw)
    }

    fun writeAll(file: File, rows: List<List<String>>) {
        val sb = StringBuilder("\uFEFF")
        for (row in rows) {
            sb.append(row.joinToString(",") { escape(it) })
            sb.append("\r\n")
        }
        file.writeText(sb.toString(), StandardCharsets.UTF_8)
    }

    private fun escape(field: String): String {
        return if (field.contains(',') || field.contains('"') || field.contains('\n') || field.contains('\r')) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
    }

    private fun parse(text: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        val n = text.length
        while (i < n) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < n && text[i + 1] == '"') {
                        field.append('"'); i += 2; continue
                    }
                    inQuotes = false; i++; continue
                }
                field.append(c); i++; continue
            }
            when (c) {
                '"' -> { inQuotes = true; i++ }
                ',' -> { row.add(field.toString()); field.clear(); i++ }
                '\r' -> {
                    if (i + 1 < n && text[i + 1] == '\n') i++
                    row.add(field.toString()); field.clear(); records.add(row.toList()); row.clear(); i++
                }
                '\n' -> {
                    row.add(field.toString()); field.clear(); records.add(row.toList()); row.clear(); i++
                }
                else -> { field.append(c); i++ }
            }
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString()); records.add(row.toList())
        }
        return records
    }
}
