package com.example.tabletool.io

import com.example.tabletool.model.ImportedFile
import com.example.tabletool.model.Sheet
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
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

    private fun cellString(c: org.apache.poi.ss.usermodel.Cell?): String {
        if (c == null) return ""
        return when (c.cellType) {
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(c)) c.dateCellValue.toString()
                else {
                    val d = c.numericCellValue
                    if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
                }
            }
            CellType.FORMULA -> c.cellFormula ?: ""
            CellType.BOOLEAN -> c.booleanCellValue.toString()
            else -> c.stringCellValue ?: ""
        }
    }
}
