package de.wannerit.loadshift.core

/**
 * Deterministic random numbers: a pure function of seed, salt and index (SplitMix64), so the simulation can ask
 * for "the noise of tick 312" at any time and always gets the same answer – on every platform.
 */
object SeededRandom {
    /** A number in 0 until [range]. */
    fun value(seed: Long, salt: Long, index: Int, range: Int): Int {
        require(range > 0)
        val x = mix(seed.toULong() xor mix(salt.toULong() * 0x1_0000_0000uL + index.toLong().toULong()))
        return (x % range.toULong()).toInt()
    }

    /** A number in -spread..spread. */
    fun symmetric(seed: Long, salt: Long, index: Int, spread: Int): Int =
        value(seed, salt, index, 2 * spread + 1) - spread

    internal fun mix(value: ULong): ULong {
        var z = value + 0x9E3779B97F4A7C15uL
        z = (z xor (z shr 30)) * 0xBF58476D1CE4E5B9uL
        z = (z xor (z shr 27)) * 0x94D049BB133111EBuL
        return z xor (z shr 31)
    }
}

/** Division rounding up, for non-negative numbers. */
internal fun ceilDiv(a: Int, b: Int): Int = (a + b - 1) / b
