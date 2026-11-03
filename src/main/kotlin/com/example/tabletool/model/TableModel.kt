package com.example.tabletool.model

/**
 * 一张表（来自某个文件的某个 Sheet，CSV 则只有一个 Sheet）。
 * headers 与每行元素一一对应；行元素可能比表头少，访问时统一用 getOrNull。
 */
data class Sheet(
    val fileName: String,
    val sheetName: String,
    val headers: List<String>,
    val rows: List<List<String>>
) {
    val colCount: Int get() = headers.size
    val rowCount: Int get() = rows.size
}

/** 一个导入的文件：Excel 可能含多个 Sheet，CSV 只有一个 Sheet。 */
data class ImportedFile(
    val fileName: String,
    val sheets: List<Sheet>
)

/** 全局唯一标识一个 Sheet："文件名#Sheet名" */
fun Sheet.id(): String = "${fileName}#${sheetName}"
