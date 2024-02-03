import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

plugins {
    kotlin("jvm") version "1.9.0"
    id("org.jetbrains.dokka") version "1.9.0"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test-junit"))
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets {
        named("main") {
            // used as project name in the header
            moduleName = "Dokka Gradle Example"

            // contains descriptions for the module and the packages
            includes.from("Module.md")

            // adds source links that lead to this repository, allowing readers
            // to easily find source code for inspected declarations
            sourceLink {
                localDirectory = file("src/main/kotlin")
                remoteUrl = URL("https://github.com/Kotlin/dokka/tree/master/" +
                        "examples/gradle/dokka-gradle-example/src/main/kotlin"
                )
                remoteLineSuffix = "#L"
            }
        }
    }
}
