plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    compileOnly("javax.inject:javax.inject:1")

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

ktlint {
    outputToConsole.set(true)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    autoCorrect = false
    config.setFrom(files(rootProject.file("app/detekt.yml")))
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint()
    }
}
