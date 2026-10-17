import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// The lesson tests write their numbers for balancing to build/lesson-report.txt in the project root.
tasks.withType<Test>().configureEach {
    systemProperty("lessonReport", rootProject.layout.buildDirectory.file("lesson-report.txt").get().asFile.path)
    testLogging { events("failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}
