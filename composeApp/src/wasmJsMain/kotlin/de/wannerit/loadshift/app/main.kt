package de.wannerit.loadshift.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window

/** Web app. Options in the address: ?scenario=2&play&ticks=40&careful&lang=de */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val pairs = window.location.search.removePrefix("?").split("&").filter { it.isNotEmpty() }.associate {
        val parts = it.split("=", limit = 2)
        parts[0] to parts.getOrNull(1)
    }
    println("app: main")
    ComposeViewport(document.body!!) {
        androidx.compose.runtime.LaunchedEffect(Unit) { println("app: first composition done") }
        // ?minimal: only a red area with a line of text – does Compose draw at all on this device?
        if ("minimal" in pairs) {
            androidx.compose.foundation.layout.Box(
                androidx.compose.ui.Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFFCC4D40)),
            ) {
                androidx.compose.foundation.text.BasicText("Compose draws here. · Compose zeichnet hier.")
            }
        } else App(LaunchOptions.parse(pairs))
    }
    println("app: viewport created")
    // The page shows "Loading …" until the app is there, and an error instead of a blank page (index.html).
    started()
}

private fun started(): Unit = js("window.loadShiftStarted && window.loadShiftStarted()")
