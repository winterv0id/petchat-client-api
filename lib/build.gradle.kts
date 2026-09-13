plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly(libs.junit.platform.launcher)

    implementation(libs.commons.math3)
    implementation(libs.guava)
    implementation(libs.httpclient5)
    implementation(libs.gson)
    implementation(libs.signalr)
    implementation(libs.jackson.databind)
    implementation(libs.slf4j.api)

    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
