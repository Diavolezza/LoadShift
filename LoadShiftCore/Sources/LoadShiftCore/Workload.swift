/// Workloads (docs/CONCEPT.md, section 5). Step 1 has interactive work only.
public enum WorkloadKind: Int, CaseIterable, Sendable, Codable {
    case interactive, batch, aiTraining, streaming

    public var name: String {
        switch self {
        case .interactive: "Interactive"
        case .batch: "Batch"
        case .aiTraining: "AI training"
        case .streaming: "Streaming"
        }
    }
}

/// Demand of interactive work (web shop, app backend): a daily curve with an evening peak, scaled to the
/// scenario's peak, with a small random noise per tick. One work unit = 1,000 requests.
public struct InteractiveLoad: Equatable, Sendable, Codable {
    /// Work units per tick at the evening peak (curve value 1,000, before noise).
    public var peak: Int
    /// Half width of the random noise per tick, per mille.
    public var noise: Int

    /// Share of the peak at the full hours, per mille (game values: night about 20 % of the evening peak,
    /// morning rise, a small bump at lunch, peak at 20:00).
    public static let dailyCurve = [380, 300, 240, 210, 200, 220, 300, 420, 540, 620, 660, 690,
                                    720, 700, 670, 660, 680, 730, 800, 900, 1_000, 960, 780, 560]

    public init(peak: Int, noise: Int = 50) {
        self.peak = peak
        self.noise = noise
    }

    /// Expected demand at a tick, without noise (what the operator can plan with).
    public func expected(tick: Int) -> Int {
        let perHour = GameTime.ticksPerHour
        let inDay = tick % GameTime.ticksPerDay
        let hour = inDay / perHour
        let r = inDay % perHour
        let share = (Self.dailyCurve[hour] * (perHour - r) + Self.dailyCurve[(hour + 1) % 24] * r) / perHour
        return peak * share / 1_000
    }

    /// Actual demand at a tick: the expected demand with the noise of that tick.
    public func actual(tick: Int, seed: UInt64) -> Int {
        let e = SeededRandom.symmetric(seed: seed, salt: 10, index: tick, spread: noise)
        return max(0, expected(tick: tick) * (1_000 + e) / 1_000)
    }

    /// Share of demand served in time, per mille of the capacity: above it requests are slow and count only
    /// half towards the SLA (simple latency model, game value).
    public static let slowAbove = 850
}
