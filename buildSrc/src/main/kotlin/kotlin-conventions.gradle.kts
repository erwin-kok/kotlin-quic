import com.adarshr.gradle.testlogger.theme.ThemeType

plugins {
    kotlin("jvm")
    id("java-conventions")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.adarshr.test-logger")
}

testlogger {
    theme = ThemeType.MOCHA
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}
