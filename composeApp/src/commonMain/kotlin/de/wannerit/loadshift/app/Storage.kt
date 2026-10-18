package de.wannerit.loadshift.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.wannerit.loadshift.core.Scenario

/** Small key-value store on the device (best stars per scenario). */
expect object Storage {
    fun getInt(key: String): Int
    fun setInt(key: String, value: Int)
    fun getString(key: String): String
    fun setString(key: String, value: String)
}

/** Puts a text on the system clipboard (where the platform allows it). */
expect fun copyText(text: String)

/** Best stars per scenario, kept on the device. */
object Progress {
    /** All scenarios open, whatever the stars (for courses that take the topics in another order). */
    var unlockAll by mutableStateOf(Storage.getInt("unlockAll") == 1)
        private set

    fun changeUnlockAll(on: Boolean) {
        unlockAll = on
        Storage.setInt("unlockAll", if (on) 1 else 0)
    }

    /** Changes with every new best result, so that the screens that show stars are drawn again. */
    var version by mutableStateOf(0)
        private set

    fun stars(scenario: Int): Int {
        @Suppress("UNUSED_VARIABLE") val v = version
        return Storage.getInt("stars.$scenario")
    }

    fun record(stars: Int, scenario: Int) {
        if (stars > stars(scenario)) {
            Storage.setInt("stars.$scenario", stars)
            version++
        }
    }

    /** No stars anywhere: every scenario that needs another closes again. */
    fun resetAll() {
        for (s in Scenario.ALL) Storage.setInt("stars.${s.number}", 0)
        Storage.setInt("lastScenario", 0)
        version++
    }

    /** A scenario is open when it needs no other, or the one it follows has at least one star. */
    fun isUnlocked(scenario: Scenario): Boolean =
        unlockAll || scenario.unlockAfter.let { it == null || stars(it) >= 1 }
}
