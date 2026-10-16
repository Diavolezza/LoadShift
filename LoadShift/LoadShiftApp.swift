import SwiftUI
import LoadShiftCore

@main
struct LoadShiftApp: App {
    var body: some Scene {
        WindowGroup {
            StartView()
        }
    }
}

/// Placeholder until step 2: shows that app and simulation core are wired up.
struct StartView: View {
    var body: some View {
        VStack(spacing: 18) {
            Image(systemName: "server.rack")
                .font(.system(size: 64, weight: .light))
                .foregroundStyle(.tint)
            Text("Load Shift")
                .font(.system(size: 44, weight: .bold, design: .rounded))
            Text("Run a greener data center")
                .font(.title3)
                .foregroundStyle(.secondary)
            Text("A game day has \(GameTime.ticksPerDay) steps of \(GameTime.minutesPerTick) minutes.")
                .font(.footnote.monospacedDigit())
                .foregroundStyle(.secondary)
                .padding(.top, 24)
        }
        .padding(40)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(red: 0.96, green: 0.97, blue: 0.96))
    }
}
