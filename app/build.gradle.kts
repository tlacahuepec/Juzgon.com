plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.kotlin.serialization)
}

val roomSchemaDir = "$projectDir/schemas"

fun getGitSha(): String =
    try {
        ProcessBuilder("git", "rev-parse", "--short", "HEAD")
            .directory(projectDir)
            .start()
            .inputStream
            .bufferedReader()
            .readText()
            .trim()
    } catch (_: Exception) {
        "unknown"
    }

android {
    namespace = "com.juzgon"
    compileSdk = 36
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "com.juzgon"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.3.0"

        testInstrumentationRunner = "com.juzgon.HiltTestRunner"

        buildConfigField("String", "BUILD_CHANNEL", "\"dev\"")
        buildConfigField("String", "GIT_SHA", "\"${getGitSha()}\"")
        buildConfigField("String", "BUILD_TIMESTAMP", "\"${System.currentTimeMillis()}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    sourceSets {
        getByName("debug") {
            assets.directories.add(roomSchemaDir)
        }
        getByName("test") {
            assets.directories.add(roomSchemaDir)
        }
        getByName("androidTest") {
            assets.directories.add(roomSchemaDir)
        }
    }
}

room {
    schemaDirectory(roomSchemaDir)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ktlint {
    android.set(true)
    outputToConsole.set(true)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    autoCorrect = false
    config.setFrom(files("detekt.yml"))
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}

tasks.register("checkDependencyBoundaries") {
    description = "Validates that feature/* does not import data/*, and domain/* does not import feature/* or data/*"
    group = "verification"
    doLast {
        val srcDir = file("src/main/java/com/juzgon")
        val violations = mutableListOf<String>()
        fileTree(srcDir.resolve("feature")).filter { it.extension == "kt" }.forEach { file ->
            file.readLines().forEachIndexed { lineNum, line ->
                if (line.trimStart().startsWith("import com.juzgon.data.")) {
                    violations.add("${file.relativeTo(srcDir)} (line ${lineNum + 1}): feature imports data layer: $line")
                }
            }
        }
        fileTree(srcDir.resolve("domain")).filter { it.extension == "kt" }.forEach { file ->
            file.readLines().forEachIndexed { lineNum, line ->
                val trimmed = line.trimStart()
                if (trimmed.startsWith("import com.juzgon.feature.") || trimmed.startsWith("import com.juzgon.data.")) {
                    violations.add("${file.relativeTo(srcDir)} (line ${lineNum + 1}): domain imports feature or data layer: $line")
                }
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException("Dependency boundary violations:\n" + violations.joinToString("\n"))
        }
        println("Dependency boundary check passed.")
    }
}

tasks.named("check") { dependsOn("checkDependencyBoundaries") }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material3)
    implementation(libs.material)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.timber)
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.navigation.testing)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.sqlite)
    testImplementation(libs.androidx.sqlite.framework)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.androidx.navigation.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
