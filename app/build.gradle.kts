import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
}
val versionPropsFile = rootProject.file("version.properties")
val props = Properties()
if (versionPropsFile.exists()) {
    versionPropsFile.inputStream().use { props.load(it) }
}

val vMajor = props.getProperty("versionMajor", "0").toInt()
val vMinor = props.getProperty("versionMinor", "1").toInt()
val vPatch = props.getProperty("versionPatch", "0").toInt()
val vBuild = props.getProperty("versionBuild", "1").toInt()

val currentVersionCode = vMajor * 10000 + vMinor * 1000 + vPatch * 100 + vBuild
val currentVersionName = "$vMajor.$vMinor.$vPatch"

android {
    namespace = "dev.photodine.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.photodine"
        minSdk = 29
        targetSdk = 36
        versionCode = currentVersionCode
        versionName = currentVersionName

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: "photodine-release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "photodine123"
            keyAlias = System.getenv("KEY_ALIAS") ?: "photodine"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "photodine123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:engine"))
    implementation(project(":core:ui"))
    implementation(project(":feature:canvas"))
    implementation(project(":feature:layers"))
    implementation(project(":feature:tools"))
    implementation(project(":feature:colorpicker"))
    implementation(project(":feature:export"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.navigation)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.coroutines.android)
    ksp(libs.hilt.compiler)
}

detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/baseline.xml")
}

tasks.register("bumpPatch") {
    doLast {
        val p = Properties()
        if (versionPropsFile.exists()) {
            versionPropsFile.inputStream().use { p.load(it) }
        }
        val nextPatch = p.getProperty("versionPatch", "0").toInt() + 1
        val nextBuild = p.getProperty("versionBuild", "1").toInt() + 1
        p.setProperty("versionPatch", nextPatch.toString())
        p.setProperty("versionBuild", nextBuild.toString())
        versionPropsFile.outputStream().use { p.store(it, "Version updated by bumpPatch") }
        println("Bumped version to ${p["versionMajor"]}.${p["versionMinor"]}.$nextPatch (Build $nextBuild)")
    }
}

tasks.register("bumpMinor") {
    doLast {
        val p = Properties()
        if (versionPropsFile.exists()) {
            versionPropsFile.inputStream().use { p.load(it) }
        }
        val nextMinor = p.getProperty("versionMinor", "1").toInt() + 1
        val nextBuild = p.getProperty("versionBuild", "1").toInt() + 1
        p.setProperty("versionMinor", nextMinor.toString())
        p.setProperty("versionPatch", "0")
        p.setProperty("versionBuild", nextBuild.toString())
        versionPropsFile.outputStream().use { p.store(it, "Version updated by bumpMinor") }
        println("Bumped version to ${p["versionMajor"]}.$nextMinor.0 (Build $nextBuild)")
    }
}

tasks.register("bumpMajor") {
    doLast {
        val p = Properties()
        if (versionPropsFile.exists()) {
            versionPropsFile.inputStream().use { p.load(it) }
        }
        val nextMajor = p.getProperty("versionMajor", "0").toInt() + 1
        val nextBuild = p.getProperty("versionBuild", "1").toInt() + 1
        p.setProperty("versionMajor", nextMajor.toString())
        p.setProperty("versionMinor", "0")
        p.setProperty("versionPatch", "0")
        p.setProperty("versionBuild", nextBuild.toString())
        versionPropsFile.outputStream().use { p.store(it, "Version updated by bumpMajor") }
        println("Bumped version to $nextMajor.0.0 (Build $nextBuild)")
    }
}
