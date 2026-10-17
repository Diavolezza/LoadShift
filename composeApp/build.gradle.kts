import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// The version from gradle.properties (written by "./build.sh commit"), for the About window.
val generateBuildInfo by tasks.registering {
    val out = layout.buildDirectory.dir("generated/buildinfo/kotlin")
    val appVersion = project.version.toString()
    inputs.property("version", appVersion)
    outputs.dir(out)
    doLast {
        val file = out.get().file("de/wannerit/loadshift/app/BuildInfo.kt").asFile
        file.parentFile.mkdirs()
        file.writeText("package de.wannerit.loadshift.app\n\ninternal object BuildInfo {\n    const val VERSION = \"$appVersion\"\n}\n")
    }
}

kotlin {
    jvm("desktop")
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("loadshift")
        browser {
            commonWebpackConfig { outputFileName = "loadshift.js" }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.configure { kotlin.srcDir(generateBuildInfo) }
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.resources)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.coroutines.swing)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "de.wannerit.loadshift.app.MainKt"
        // The app name in the macOS menu bar and Dock, also for "./gradlew :composeApp:run".
        if (System.getProperty("os.name").startsWith("Mac")) jvmArgs += listOf("-Xdock:name=Load Shift")
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "LoadShift"
            packageVersion = "1.0.0"
        }
    }
}

compose.resources {
    packageOfResClass = "de.wannerit.loadshift.app.resources"
    publicResClass = false
}
