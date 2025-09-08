plugins {
    id("kotlin-conventions")
}

group = "org.erwinkok.quic"
version = "0.1.0"

dependencies {
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlin.logging)
    implementation(libs.ktor.network)
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.8.0")

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    runtimeOnly(libs.logback.classic)
}
