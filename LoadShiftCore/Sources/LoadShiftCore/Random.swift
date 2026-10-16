/// Deterministic random numbers: a pure function of seed, salt and index (SplitMix64), so the simulation can
/// ask for "the noise of tick 312" at any time and always gets the same answer. No state, no Foundation.
public enum SeededRandom {
    /// A number in 0 ..< range.
    public static func value(seed: UInt64, salt: UInt64, index: Int, range: Int) -> Int {
        precondition(range > 0)
        let x = mix(seed ^ mix(salt &* 0x1_0000_0000 &+ UInt64(bitPattern: Int64(index))))
        return Int(x % UInt64(range))
    }

    /// A number in -spread ... spread.
    public static func symmetric(seed: UInt64, salt: UInt64, index: Int, spread: Int) -> Int {
        value(seed: seed, salt: salt, index: index, range: 2 * spread + 1) - spread
    }

    static func mix(_ value: UInt64) -> UInt64 {
        var z = value &+ 0x9E37_79B9_7F4A_7C15
        z = (z ^ (z >> 30)) &* 0xBF58_476D_1CE4_E5B9
        z = (z ^ (z >> 27)) &* 0x94D0_49BB_1331_11EB
        return z ^ (z >> 31)
    }
}

/// Division rounding towards minus infinity.
@inlinable
func floorDiv(_ a: Int, _ b: Int) -> Int {
    let q = a / b
    return (a % b != 0 && (a < 0) != (b < 0)) ? q - 1 : q
}

/// Division rounding up, for non-negative numbers.
@inlinable
func ceilDiv(_ a: Int, _ b: Int) -> Int { (a + b - 1) / b }
