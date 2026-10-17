package de.wannerit.loadshift.app

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/** Desktop app. Options as "-scenario 2 -play -ticks 40 -careful -lang de". */
fun main(args: Array<String>) {
    val pairs = mutableMapOf<String, String?>()
    var i = 0
    while (i < args.size) {
        val key = args[i].removePrefix("-")
        val value = args.getOrNull(i + 1)?.takeUnless { it.startsWith("-") }
        pairs[key] = value
        i += if (value == null) 1 else 2
    }
    application {
        Window(onCloseRequest = ::exitApplication, title = "Load Shift", state = rememberWindowState(width = 1440.dp, height = 960.dp)) {
            App(LaunchOptions.parse(pairs))
        }
    }
}
