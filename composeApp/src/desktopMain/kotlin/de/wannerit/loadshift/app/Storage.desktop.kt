package de.wannerit.loadshift.app

import java.util.prefs.Preferences

actual object Storage {
    private val prefs = Preferences.userRoot().node("de/wanner-it/loadshift")

    actual fun getInt(key: String): Int = prefs.getInt(key, 0)

    actual fun setInt(key: String, value: Int) {
        prefs.putInt(key, value)
        prefs.flush()
    }
}
