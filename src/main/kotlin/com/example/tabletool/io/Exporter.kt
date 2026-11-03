package com.example.tabletool.io

import com.example.tabletool.model.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File

object Exporter {

    /**
     * 导出结果表。
     * - .xlsx：多个结果表各作为一个 Sheet 写入同一个文件。
     * - .csv：只有一个结果时直接写；多个结果时按 "原名_序号_表名.csv" 各写一个文件。
     */
    fun export(results: List<Sheet>, outFile: File) {
        val lower = outFile.name.lowercase()
        when {
            lower.endsWith(".csv") -> {
                if (results.size == 1) exportCsv(results[0], outFile)
                else results.forEachIndexed { i, s ->
                    val f = File(outFile.parent, "${outFile.nameWithoutExtension}_${i + 1}_${sanitize(s.sheetName)}.csv")
                    exportCsv(s, f)
                }
            }
            else -> exportXlsx(results, outFile)
        }
    }

    private fun exportXlsx(results: List<Sheet>, out: File) {
        val wb = XSSFWorkbook()
        results.forEach { s ->
            val name = s.sheetName.take(31).ifBlank { "Sheet" }
            val sh = wb.createSheet(uniqueSheetName(wb, name))
            val head = sh.createRow(0)
            s.headers.forEachIndexed { i, h -> head.createCell(i).setCellValue(h) }
            s.rows.forEachIndexed { r, row ->
                val rr = sh.createRow(r + 1)
                row.forEachIndexed { i, v -> rr.createCell(i).setCellValue(v) }
            }
        }
        wb.write(out.outputStream())
        wb.close()
    }

    private fun exportCsv(s: Sheet, out: File) {
        CsvUtil.writeAll(out, listOf(s.headers) + s.rows)
    }

    private fun uniqueSheetName(wb: XSSFWorkbook, base: String): String {
        var n = base
        var i = 1
        while (wb.getSheet(n) != null) { n = "${base.take(27)}_${i++}" }
        return n
    }

    private fun sanitize(s: String) = s.replace(Regex("[\\\\/:*?\"<>|]"), "_")
}
