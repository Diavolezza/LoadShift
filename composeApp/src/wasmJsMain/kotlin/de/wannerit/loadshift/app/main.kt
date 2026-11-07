package de.wannerit.loadshift.app

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
    ComposeViewport(document.body!!) {
        App(LaunchOptions.parse(pairs))
    }
    // The page shows "Loading …" until the app is there, and an error instead of a blank page (index.html).
    started()
}

private fun started(): Unit = js("window.loadShiftStarted && window.loadShiftStarted()")
