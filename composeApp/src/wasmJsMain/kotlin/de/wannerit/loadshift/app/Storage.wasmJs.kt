package de.wannerit.loadshift.app

import kotlinx.browser.localStorage
import kotlinx.browser.window

actual object Storage {
    actual fun getInt(key: String): Int = localStorage.getItem("loadshift.$key")?.toIntOrNull() ?: 0

    actual fun setInt(key: String, value: Int) {
        localStorage.setItem("loadshift.$key", value.toString())
    }

    actual fun getString(key: String): String = localStorage.getItem("loadshift.$key") ?: ""

    actual fun setString(key: String, value: String) {
        localStorage.setItem("loadshift.$key", value)
    }
}

actual fun copyText(text: String) {
    window.navigator.clipboard.writeText(text)
}
