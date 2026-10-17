package de.wannerit.loadshift.app

import kotlinx.browser.localStorage

actual object Storage {
    actual fun getInt(key: String): Int = localStorage.getItem("loadshift.$key")?.toIntOrNull() ?: 0

    actual fun setInt(key: String, value: Int) {
        localStorage.setItem("loadshift.$key", value.toString())
    }
}
