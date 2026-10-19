package de.wannerit.loadshift.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.wannerit.loadshift.core.Scenario

/** Small key-value store on the device (best stars per scenario). */
expect object Storage {
    fun getInt(key: String): Int
    fun setInt(key: String, value: Int)
}

/** Best stars per scenario, kept on the device. */
object Progress {
    /** All scenarios open, whatever the stars – only for this start of the app (launch option "unlock"), not kept. */
    var unlockAll by mutableStateOf(false)

    /** The player has uncovered the solution of a scenario on the end card. */
    fun revealed(scenario: Int): Boolean {
        @Suppress("UNUSED_VARIABLE") val v = version
        return Storage.getInt("revealed.$scenario") == 1
    }

    fun reveal(scenario: Int) {
        Storage.setInt("revealed.$scenario", 1)
        version++
    }

    /** The solution of a scenario has been uncovered (the hidden settings of "What if" are shown). */
    fun solutionSeen(scenario: Int): Boolean = revealed(scenario)

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
        for (s in Scenario.ALL) {
            Storage.setInt("stars.${s.number}", 0)
            Storage.setInt("revealed.${s.number}", 0)
        }
        Storage.setInt("lastScenario", 0)
        Storage.setInt("unlockAll", 0) // kept by versions before 0.3.72
        version++
    }

    /** A scenario is open when it needs no other, or the one it follows has at least one star. */
    fun isUnlocked(scenario: Scenario): Boolean =
        unlockAll || scenario.unlockAfter.let { it == null || stars(it) >= 1 }
}
