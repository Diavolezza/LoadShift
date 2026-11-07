package de.wannerit.loadshift.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window

/** Web app compiled to JavaScript (for Safari); the same as the Wasm one in wasmJsMain. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val pairs = window.location.search.removePrefix("?").split("&").filter { it.isNotEmpty() }.associate {
        val parts = it.split("=", limit = 2)
        parts[0] to parts.getOrNull(1)
    }
    println("app: main (JavaScript)")
    // Kotlin/JS: the graphics library (Skia, WebAssembly) must be loaded before the first viewport.
    org.jetbrains.skiko.wasm.onWasmReady {
        ComposeViewport(document.body!!) {
            App(LaunchOptions.parse(pairs))
        }
        // The page shows "Loading …" until the app is there, and an error instead of a blank page (index.html).
        js("if (window.loadShiftStarted) window.loadShiftStarted()")
    }
}
