import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/**
 * Release signing is read from an untracked `keystore.properties` at the repo root.
 * Nothing about the keystore is committed; if the file is absent the release build
 * simply falls back to the debug signing config so CI can still produce an artifact.
 */
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.greenwood.school"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.greenwood.school"
        minSdk = 26
        targetSdk = 35
        // Bumped for password recovery by one-time passcode, which replaces a flow
        // that emailed a reset link — the link pointed at the web frontend, so
        // resetting a password from the phone never really worked. Carries the
        // 1.2.0 contents too: teacher add/edit, the reachable student edit form,
        // and the client half of seven defects. A new versionCode is what lets the
        // device recognise this as an upgrade rather than refusing to install over
        // the previous build.
        versionCode = 4
        versionName = "1.3.0"

        testInstrumentationRunner = "com.greenwood.school.HiltTestRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    /**
     * One flavour per environment. The backend base URL is *only* declared here —
     * nothing else in the codebase hard-codes a host. Override any of them at build
     * time with e.g. `-PdevApiUrl=http://10.0.2.2:8080/api/v1/`.
     *
     * NOTE the trailing slash: Retrofit requires it on the base URL, and every
     * @GET/@POST path in the API interfaces is therefore relative (no leading slash).
     */
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "Greenwood Dev")
            // 10.0.2.2 is the host loopback as seen from the Android emulator.
            buildConfigField(
                "String",
                "BASE_URL",
                "\"${providers.gradleProperty("devApiUrl").getOrElse("http://10.0.2.2:8080/api/v1/")}\"",
            )
            buildConfigField("boolean", "ALLOW_CLEARTEXT", "true")
        }
        create("qa") {
            dimension = "environment"
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa"
            resValue("string", "app_name", "Greenwood QA")
            buildConfigField(
                "String",
                "BASE_URL",
                "\"${providers.gradleProperty("qaApiUrl").getOrElse("http://132.226.191.38:8080/api/v1/")}\"",
            )
            buildConfigField("boolean", "ALLOW_CLEARTEXT", "true")
        }
        create("prod") {
            dimension = "environment"
            resValue("string", "app_name", "Greenwood School")
            // TLS is now terminated by Caddy in front of the backend (sms-caddy
            // reverse-proxies this hostname to backend:8080 and manages the
            // certificate), so prod talks HTTPS and needs no cleartext exemption.
            //
            // The hostname is nip.io, which resolves 132-226-191-38.nip.io to
            // 132.226.191.38 — a real DNS name is required because a certificate
            // cannot be issued for a bare IP.
            buildConfigField(
                "String",
                "BASE_URL",
                "\"${providers.gradleProperty("prodApiUrl").getOrElse("https://132-226-191-38.nip.io/api/v1/")}\"",
            )
            buildConfigField("boolean", "ALLOW_CLEARTEXT", "false")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "false")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/LICENSE.md",
            "/META-INF/LICENSE-notice.md",
        )
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.androidx.arch.core.testing)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}
