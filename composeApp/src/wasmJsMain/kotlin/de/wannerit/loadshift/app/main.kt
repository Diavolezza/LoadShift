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
}
