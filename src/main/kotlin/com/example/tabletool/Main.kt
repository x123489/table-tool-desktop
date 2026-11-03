package com.example.tabletool

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension

fun main() = application {
    val state = rememberWindowState(width = 1200.dp, height = 760.dp)
    Window(
        onCloseRequest = ::exitApplication,
        title = "表格处理工具 · 单机版"
    ) {
        window.minimumSize = Dimension(920, 600)
        App()
    }
}
