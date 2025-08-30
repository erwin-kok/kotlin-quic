plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.buildscript.kotlin)
    implementation(libs.buildscript.ktjni)
    implementation(libs.buildscript.ktlint)
    implementation(libs.buildscript.testlogger)
    implementation(libs.buildscript.versions)

    implementation(kotlin("gradle-plugin"))
    implementation(libs.oshai.logging)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    jvmToolchain(21)
}
