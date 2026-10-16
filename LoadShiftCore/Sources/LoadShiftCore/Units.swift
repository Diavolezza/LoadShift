/// Units used in the simulation, all integers:
///
/// - power in W,
/// - energy in mWh (1 W for one tick of 15 minutes = 250 mWh),
/// - carbon intensity in tenths of g CO₂e/kWh,
/// - emissions in µg CO₂e (1 mWh at 1 g/kWh = 1 µg; with tenths: mWh × tenths / 10),
/// - shares in per mille.
public enum Units {
    /// 1 W for one tick (15 minutes) = 250 mWh.
    public static let milliwattHoursPerWattTick = 250
    /// Days a server is expected to be in service: 6 years (Microsoft and Google [S18]).
    public static let lifespanDays = 6 * 365

    /// Emissions in µg of an energy in mWh at an intensity in tenths of g/kWh.
    @inlinable
    public static func emissions(milliwattHours: Int, intensityTenths: Int) -> Int {
        milliwattHours * intensityTenths / 10
    }
}
