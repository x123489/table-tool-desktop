package com.example.tabletool

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension

fun main() = application {
    val state = rememberWindowState(placement = WindowPlacement.Maximized)
    Window(
        onCloseRequest = ::exitApplication,
        title = "表格处理工具 · 单机版",
        state = state
    ) {
        window.minimumSize = Dimension(1000, 640)
        App()
    }
}
