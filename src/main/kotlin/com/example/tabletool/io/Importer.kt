package com.example.tabletool.io

import com.example.tabletool.model.ImportedFile
import com.example.tabletool.model.Sheet
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.FormulaError
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File

object Importer {

    fun import(file: File): ImportedFile {
        val lower = file.name.lowercase()
        return when {
            lower.endsWith(".xlsx") || lower.endsWith(".xls") -> importExcel(file)
            lower.endsWith(".csv") -> importCsv(file)
            else -> throw IllegalArgumentException(
                "不支持的文件格式：${file.name}（仅支持 .xlsx / .xls / .csv）"
            )
        }
    }

    private fun importExcel(file: File): ImportedFile {
        val wb = WorkbookFactory.create(file)
        val sheets = mutableListOf<Sheet>()
        for (i in 0 until wb.numberOfSheets) {
            val s = wb.getSheetAt(i)
            val headers = mutableListOf<String>()
            val rows = mutableListOf<List<String>>()
            val it = s.iterator()
            if (it.hasNext()) {
                val head = it.next()
                for (c in head) headers.add(cellString(c))
            }
            while (it.hasNext()) {
                val r = it.next()
                val rowVals = headers.indices.map { idx -> cellString(r.getCell(idx)) }
                rows.add(rowVals)
            }
            sheets.add(Sheet(file.name, s.sheetName ?: "Sheet${i + 1}", headers, rows))
        }
        wb.close()
        return ImportedFile(file.name, sheets)
    }

    private fun importCsv(file: File): ImportedFile {
        val all = CsvUtil.readAll(file)
        if (all.isEmpty()) {
            return ImportedFile(file.name, listOf(Sheet(file.name, file.nameWithoutExtension, emptyList(), emptyList())))
        }
        val headers = all.first()
        val rows = all.drop(1)
        return ImportedFile(file.name, listOf(Sheet(file.name, file.nameWithoutExtension, headers, rows)))
    }

    private val formatter = DataFormatter()

    private fun cellString(c: org.apache.poi.ss.usermodel.Cell?): String {
        if (c == null) return ""
        return try {
            when (c.cellType) {
                CellType.NUMERIC -> formatNumeric(c)
                CellType.STRING -> c.stringCellValue ?: ""
                CellType.FORMULA -> formatFormula(c)
                CellType.BOOLEAN -> c.booleanCellValue.toString()
                CellType.ERROR -> errorText(c)
                CellType.BLANK -> ""
                else -> ""
            }
        } catch (e: Exception) {
            // 兜底：极端情况下也不让整张表导入失败
            try { formatter.formatCellValue(c) ?: "" } catch (_: Exception) { "" }
        }
    }

    private fun formatNumeric(c: org.apache.poi.ss.usermodel.Cell): String {
        if (DateUtil.isCellDateFormatted(c)) {
            return c.dateCellValue?.toString() ?: ""
        }
        val d = c.numericCellValue
        return if (d.isFinite() && d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
    }

    private fun formatFormula(c: org.apache.poi.ss.usermodel.Cell): String {
        return when (c.cachedFormulaResultType) {
            CellType.NUMERIC -> formatNumeric(c)
            CellType.STRING -> c.stringCellValue ?: ""
            CellType.BOOLEAN -> c.booleanCellValue.toString()
            CellType.ERROR -> errorText(c)
            else -> ""
        }
    }

    private fun errorText(c: org.apache.poi.ss.usermodel.Cell): String {
        return try {
            // 字面量错误单元格可直接取错误码并转成 "#DIV/0!" 等文本
            FormulaError.forInt(c.errorCellValue.toInt()).string
        } catch (_: Exception) {
            // 公式单元格的缓存错误结果取不到错误码，用 DataFormatter 兜底
            try { formatter.formatCellValue(c) ?: "ERROR" } catch (_: Exception) { "ERROR" }
        }
    }
}
