package de.wannerit.loadshift.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.intl.Locale

/**
 * Language of the app: the player's choice on the start page, kept on the device; without one German if the system
 * language is German, else English. A launch option sets it for one run. A state, so that switching redraws the app.
 */
object Language {
    var german: Boolean by mutableStateOf(
        when (Storage.getInt("language")) {
            1 -> true
            2 -> false
            else -> Locale.current.language == "de"
        },
    )

    /** The player's choice, kept for the next start. */
    fun choose(german: Boolean) {
        this.german = german
        Storage.setInt("language", if (german) 1 else 2)
    }
}

/**
 * The text in the app's language, with the arguments filled in. Keys are the English texts with the placeholders
 * %@ (text) and %lld (whole number); translations may number them (%1$@, %2$lld). With arguments, %% is a literal %.
 */
fun t(key: String, vararg args: Any): String {
    val pattern = if (Language.german) GERMAN[key] ?: key else key
    if (args.isEmpty()) return pattern
    val out = StringBuilder()
    var next = 0
    var i = 0
    while (i < pattern.length) {
        val c = pattern[i]
        if (c != '%') {
            out.append(c); i++; continue
        }
        if (pattern.startsWith("%%", i)) {
            out.append('%'); i += 2; continue
        }
        // Optional position "1$", then "@", "lld", "d" or "s".
        var j = i + 1
        var position: Int? = null
        val digits = pattern.drop(j).takeWhile { it.isDigit() }
        if (digits.isNotEmpty() && pattern.getOrNull(j + digits.length) == '$') {
            position = digits.toInt() - 1
            j += digits.length + 1
        }
        val length = when {
            pattern.startsWith("lld", j) -> 3
            pattern.startsWith("@", j) || pattern.startsWith("d", j) || pattern.startsWith("s", j) -> 1
            else -> 0
        }
        if (length == 0) {
            out.append(c); i++; continue
        }
        val index = position ?: next++
        out.append(args.getOrNull(index)?.toString() ?: "")
        i = j + length
    }
    return out.toString()
}
