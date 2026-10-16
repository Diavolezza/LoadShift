// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "LoadShiftCore",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "LoadShiftCore", targets: ["LoadShiftCore"])
    ],
    targets: [
        // Optimized also in Debug builds: the reference operator and the effect preview simulate whole
        // scenarios in the background, and the tests run faster.
        .target(name: "LoadShiftCore", swiftSettings: [.unsafeFlags(["-O"], .when(configuration: .debug))]),
        .testTarget(name: "LoadShiftCoreTests", dependencies: ["LoadShiftCore"])
    ]
)
