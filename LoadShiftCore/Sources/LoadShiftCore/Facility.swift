/// The building around the servers: cooling and power distribution (docs/CONCEPT.md, section 7.1).
/// Step 1 has one fixed cooling type; setpoint and outside temperature come in step 5.
public enum CoolingType: Int, CaseIterable, Sendable, Codable {
    case roomAir, aisleContainment, freeCooling, liquid

    public var name: String {
        switch self {
        case .roomAir: "Room air conditioning"
        case .aisleContainment: "Hot/cold aisle containment"
        case .freeCooling: "Free cooling"
        case .liquid: "Direct liquid cooling"
        }
    }

    /// Cooling overhead per kW of IT power, per mille (game values, CONCEPT.md 7.1).
    public var overheadPermille: Int {
        switch self {
        case .roomAir: 450
        case .aisleContainment: 300
        case .freeCooling: 150
        case .liquid: 50
        }
    }
}

public struct Facility: Equatable, Sendable, Codable {
    public var cooling: CoolingType
    /// Power distribution and lighting, per mille of the IT power.
    public static let distributionPermille = 70

    public init(cooling: CoolingType = .roomAir) {
        self.cooling = cooling
    }

    /// Facility power for an IT power, W.
    public func facilityPower(itPower: Int) -> Int {
        itPower * (1_000 + cooling.overheadPermille + Self.distributionPermille) / 1_000
    }

    /// PUE in per mille (1,520 = 1.52).
    public var pue: Int { 1_000 + cooling.overheadPermille + Self.distributionPermille }
}
