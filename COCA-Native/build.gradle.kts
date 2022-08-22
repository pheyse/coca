/*
    IntelliJ Option to building code automatically on changes
    https://kotlinlang.org/docs/native-get-started.html#build-and-run-the-application
    1. Go to Settings/Preferences | Build, Execution, Deployment | Compiler.
    2. On the Compiler page, select Build project automatically.
    3. Apply the changes.

    Create exe files:
    run "nativeBinaries" in gradle
 */

plugins {
    kotlin("multiplatform") version "1.7.0"
    kotlin("plugin.serialization") version "1.7.10"
}

group = "de.bright-side.coca"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {

    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            executable("coca") {
                entryPoint = "main"
            }
        }
    }
    sourceSets {
        val okioVersion = "3.2.0"
        val commonMain by getting {
            dependencies {
                implementation("com.squareup.okio:okio:$okioVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
            }
        }
        val nativeMain by getting
        val nativeTest by getting
    }
}


tasks.register("coca_help") {
    doLast {
        println("running COCA help:")
        exec {
            commandLine = listOf("coca")
        }
    }
}


tasks.register("coca_preview_html") {
    doLast {
        println("running COCA HTML preview:")
        exec {
            commandLine = listOf("coca"
                , "-a", "p"
                , "-f", "h"
                , "-o", "C:\\Philip\\Development\\Kotlin\\CommentedOutCodeArchiver\\coca-preview.html"
                , "-c", "C:\\Philip\\Development\\Kotlin\\CommentedOutCodeArchiver\\coca-config.yaml")
        }
    }
}

tasks.register("coca_preview_multiline") {
    doLast {
        println("creating COCA multiline preview:")
        exec {
            commandLine = listOf("coca"
                , "-a", "p"
                , "-f", "m"
                , "-c", "C:\\Philip\\Development\\Kotlin\\CommentedOutCodeArchiver\\coca-config.yaml")
        }
    }
}

tasks.register("coca_archive_comments") {
    doLast {
        println("archiving comments:")
        exec {
            commandLine = listOf("coca"
                , "-a", "a"
                , "-c", "C:\\Philip\\Development\\Kotlin\\CommentedOutCodeArchiver\\coca-config.yaml")
        }
    }
}



