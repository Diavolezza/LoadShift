package de.wannerit.loadshift.app

/** Small key-value store on the device (best stars per scenario). */
expect object Storage {
    fun getInt(key: String): Int
    fun setInt(key: String, value: Int)
}

/** Best stars per scenario, kept on the device. */
object Progress {
    fun stars(scenario: Int): Int = Storage.getInt("stars.$scenario")

    fun record(stars: Int, scenario: Int) {
        if (stars > stars(scenario)) Storage.setInt("stars.$scenario", stars)
    }

    /** A scenario is open when it is the first or the one before has at least one star. */
    fun isUnlocked(scenario: Int): Boolean = scenario <= 1 || stars(scenario - 1) >= 1
}
