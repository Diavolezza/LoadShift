import SwiftUI
import LoadShiftCore

/// Texts that differ between scenarios: briefing, the reference operator, the rule for the first star and a
/// closing remark in the debrief.
struct ScenarioTexts {
    let situation: LocalizedStringKey
    let idea: LocalizedStringKey
    let screen: LocalizedStringKey
    /// "compared with the reference operator, who …" (end card).
    let comparedWith: LocalizedStringKey
    /// The rule for the first star.
    let rules: LocalizedStringKey
    /// One remark in the debrief.
    let remark: LocalizedStringKey

    static func of(_ scenario: Scenario) -> ScenarioTexts {
        switch scenario.number {
        case 2:
            return ScenarioTexts(
                situation: "The web shop's batch cluster in Spain: ten servers for reports, backups, imports and conversions. Over two days seven jobs arrive, each with a deadline. Nobody minds when a job runs – as long as it is finished in time.",
                idea: "The same job emits less CO₂ when the grid is clean. In Spain's solar grid the cleanest hours are around midday, not at night. Plan every job into clean hours before its deadline – and keep in mind that only ten servers are there.",
                screen: "Right: the jobs with their size and deadline. Plan a start time for each job with plus and minus, or start it now; a planned job starts by itself. Bottom: the carbon intensity with the forecast, and below it one row per job – light: the time between arrival and deadline, outlined: planned, filled: running or done. The game pauses when a job must start within the hour.",
                comparedWith: "compared with the reference operator, who starts every job as soon as it arrives.",
                rules: "every job by its deadline and less CO₂ than the reference",
                remark: "The night is not clean everywhere: in a solar grid the midday hours are the cleanest. Part of the CO₂ is the hardware's manufacturing – it stays the same whenever the jobs run.")
        default:
            return ScenarioTexts(
                situation: "You run the data center of a web shop in Germany. Its 20 servers are sized for the Christmas season. Today is an ordinary autumn day: in the evening many people shop, at night hardly anyone. It is midnight, and the previous shift has left every server on.",
                idea: "An idle server still draws about a quarter of its maximum power. Switch off the servers that are not needed – and switch them on again in time: a server takes 30 minutes before it can work.",
                screen: "The game starts paused. Top: clock and speed – pause, 1×, 4×, 16× – and below it your goals. Middle: your racks; grey servers are off, light blue ones are booting, the blue bar shows how busy a server is. Right: plus and minus for the servers, and the situation right now. Bottom: carbon intensity of the grid with the forecast, the load so far and the expected load (dotted); the lines on the right show how many servers a load needs.",
                comparedWith: "compared with the reference operator, who leaves every server on.",
                rules: "SLA at least 99 % and less CO₂ than the reference",
                remark: "Switching servers off saves energy, not hardware: the embodied emissions stay the same.")
        }
    }
}
