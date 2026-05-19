package io.github.nadeemiqbal.flagbar.sample.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.nadeemiqbal.flagbar.sample.SampleApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "FlagBar Sample",
        state = rememberWindowState(width = 600.dp, height = 800.dp),
    ) {
        SampleApp()
    }
}
