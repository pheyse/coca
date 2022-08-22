import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
    application
}

group = "de.bright-side.coca"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf(
                "src/main/kotlin",
                "/../COCA-Native/src/commonMain/kotlin",
            ))
        }
        resources {
            setSrcDirs(listOf("src/main/resources"))
        }
    }
    test {
        java {
            setSrcDirs(listOf(
                "src/test/kotlin",
                "/../COCA-Native/src/commonTest/kotlin"
            ))
        }
        resources {
            setSrcDirs(listOf("src/main/resources"))
        }
    }
}


dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}