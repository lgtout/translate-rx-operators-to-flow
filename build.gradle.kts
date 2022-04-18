@file:Suppress("SuspiciousCollectionReassignment")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.6.20"
}

object DependencyVersions {
    const val kotest = "5.2.3"
    const val kotlin = "1.6.20"
    const val coroutines = "1.6.1"
    const val turbine = "0.7.0"
    const val arrow = "1.0.1"
}

repositories {
    mavenCentral()
    jcenter()
    maven("https://oss.jfrog.org/artifactory/oss-snapshot-local/")
    maven("https://jitpack.io")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf("-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi", "-Xopt-in=kotlin.RequiresOptIn")
    }
}

kotlin {
    sourceSets.all {
        languageSettings.apply {
            languageVersion = "1.7"
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    implementation("io.arrow-kt:arrow-core:${DependencyVersions.arrow}")
    implementation(kotlin("stdlib"))
    implementation(kotlin("script-runtime"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${DependencyVersions.coroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${DependencyVersions.coroutines}")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm")
    // Coroutine exception stacktrace helper
    // https://github.com/Anamorphosee/stacktrace-decoroutinator
    implementation("dev.reformator.stacktracedecoroutinator:stacktrace-decoroutinator-jvm:2.2.1")
    testImplementation("io.kotest:kotest-runner-junit5:${ DependencyVersions.kotest }")
    testImplementation("io.kotest:kotest-assertions-core:${ DependencyVersions.kotest }")
    // testImplementation("io.kotest:kotest-common-jvm:${ DependencyVersions.kotest }")
    testImplementation("app.cash.turbine:turbine:${DependencyVersions.turbine}")
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    languageVersion = "1.7"
}