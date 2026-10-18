package de.wannerit.loadshift.app

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.prefs.Preferences

actual object Storage {
    private val prefs = Preferences.userRoot().node("de/wanner-it/loadshift")

    actual fun getInt(key: String): Int = prefs.getInt(key, 0)

    actual fun setInt(key: String, value: Int) {
        prefs.putInt(key, value)
        prefs.flush()
    }

    actual fun getString(key: String): String = prefs.get(key, "")

    actual fun setString(key: String, value: String) {
        prefs.put(key, value)
        prefs.flush()
    }
}

actual fun copyText(text: String) {
    runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null) }
}
