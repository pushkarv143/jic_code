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
        // 1.5.0 — one section per class, and a timetable of your own.
        //
        // The school now runs a single section per class, so the section picker is
        // gone from attendance (the id is resolved from the class instead), the class
        // module's "Sections" tab is "Class Setup" — that section's room, capacity
        // and class teacher — and "Section A" has stopped being printed on rows where
        // it said nothing.
        //
        // New: My Timetable, for teachers, students and parents. It asks
        // `timetable/me`, so the server decides whose week it is and there is no id in
        // the path; a teacher sees the periods they teach and which class each is for,
        // a student their class's week and who teaches each subject.
        //
        // Also follows the server's tightened authorization, so the phone stops
        // offering what the API would refuse: browsing any class's week is
        // management-only now, so the Timetable tab is hidden for teachers rather than
        // arriving as an empty grid, and a class teacher only ever sees the leave
        // applications and students of their own class - both already scoped
        // server-side, so they need no client change beyond not asking.
        //
        // 1.6.0 brings the web app's live authorization model to the phone: the app
        // reads GET /me/access on sign-in and after every silent token refresh, so a
        // permission an administrator grants or revokes — or a module they switch off —
        // reaches a signed-in device without waiting for the next login. Menu and
        // in-screen filtering became strict with it (no grants, no action); the My Class
        // module arrived, keyed on the homeroom assignment rather than the CLASS_TEACHER
        // role; taking the register narrowed to the class teacher; and the roll-number
        // field is gone, being server-assigned and unique per class.
        //
        // 1.7.0 gives the class module its write side on the phone. It was a viewer:
        // six tabs that could show a class teacher, a subject list, a subject-teacher
        // mapping and the posts, but change none of them. It now assigns the class
        // teacher, adds and removes subjects, assigns subject teachers, ends a post,
        // and sets a period - filling Monday to Saturday in one tap, as the web app
        // does. Every control is gated on the same grant the endpoint behind it
        // enforces, so nothing is offered that the API would refuse.
        //
        // 1.8.0 takes the menu off the phone and puts it in the database. Until now
        // the app carried its own copy of the navigation menu with each entry's
        // roles hard-coded, duplicating the web client's - so deciding who saw a
        // menu meant editing two files in two languages and releasing two apps.
        // The menu is now the `menus` and `role_menus` tables, arrives inside
        // GET /me/access already filtered, and the app supplies only the route and
        // icon behind each menu key. An administrator changes who sees what from
        // Settings > Roles & Permissions > Menus, and a signed-in phone picks it up
        // on its next token refresh without an update.
        //
        // 1.9.0 drops the CLASS_TEACHER role. There is one Teacher role now, and
        // being a class teacher is teachers.is_class_teacher — a flag that comes
        // and goes with the section assignment and grants six extra permissions
        // while it is set. The two roles were never two kinds of person: the second
        // held exactly the first's permissions plus those six, and 44 users carried
        // it while only 17 headed a section. The app reads the capability from
        // /me/access as it already did, so the change is mostly subtraction — but
        // Role.from still maps the retired name to TEACHER, so a phone holding an
        // access token issued before the migration is not left staring at an empty
        // shell until it expires. Who is a class teacher is now shown on the
        // teacher list, where the role used to answer it.
        //
        // 1.10.0 removes self-registration. "Create an account" is gone from the
        // sign-in screen: the school admits a student through its own form, which
        // generates a username (first name plus the initial of the last name) and a
        // random first-time password, and emails both. An account that could exist
        // before the admission did was an account nobody had asked for.
        //
        // A provisioned account opens the new Choose-your-password screen instead of
        // the dashboard, and cannot reach anything else until it has one of its own —
        // the server refuses every other endpoint, so an older build would simply
        // show a dashboard where nothing loaded. Changing the password revokes every
        // token, so the flow ends back at sign-in. The state survives the app being
        // killed mid-reset.
        //
        // 1.11.0 adds Resend Credentials to the student form, for SUPER_ADMIN
        // alone. It regenerates the temporary password, keeps the username (that is
        // the student's identity, and a lost password says nothing about it), sends
        // both by email and SMS, re-arms the forced first-login reset and ends every
        // session on the account. Behind a confirmation, because the student's
        // current password stops working the moment it is tapped.
        //
        // 1.12.0 fixes a first-login dead end. A student whose account was created
        // by the school could sign in and then reach nothing: the flag saying "this
        // password must be changed" was threaded into the navigation graph, which is
        // built once, so it was captured before anyone had signed in and was always
        // false. They landed on the dashboard, where the API refuses every request
        // until the password is changed, and the app looked broken rather than asking
        // for a new one. It now comes from the sign-in itself, on both the password
        // and the passcode path.
        //
        // The passcode path mattered more than it looked: a one-time code is how
        // somebody gets in when the credentials email never arrived, so it is the
        // likeliest way to reach an account that still owes a password change - and
        // that person has never seen the emailed password. The old password is
        // therefore optional on the change-password screen now.
        //
        // 1.13.0 is a design pass rather than a feature. The app was already Material
        // 3, but three things were missing underneath it and every screen paid for
        // them: there was no spacing scale, so padding was 5, 6, 10, 12 or 14dp
        // depending on who wrote the screen and nothing lined up between cards; the
        // type scale stopped at headlineLarge, so anything that wanted to be bigger
        // invented its own fontSize; and the M3 surface-container roles were never
        // assigned, so Compose fell back to its purple-tinted defaults and every
        // elevated surface came out subtly off-brand.
        //
        // ui/theme now carries the tokens — an 8dp grid, an elevation ladder, the full
        // display-to-label type scale, and the surface tones tinted toward the indigo
        // brand hue. Cards read 16dp corners and a hairline outline from it, which is
        // what stops a 1dp card looking like a flat box on the light background.
        // Because the shared cards are where every list and detail screen gets its
        // chrome, all of them inherit the change without being touched.
        //
        // First load shows a shimmer skeleton shaped like the content that is coming
        // instead of a centred spinner, so the screen no longer jumps when data lands.
        // The dashboard was rebuilt on the tokens: its greeting sits in a large app bar
        // that collapses as you scroll, the role is a chip rather than grey caption
        // text, events are led by a calendar-tile date, and a tapped card lifts and
        // scales slightly — a ripple alone is mostly hidden behind a tile's own
        // content. Bottom-bar icons fill in on selection, since the pill indicator is
        // the first thing to disappear on a dimmed screen or for a user with low
        // vision.
        //
        // Dynamic colour (Material You) is wired up but off. This ships alongside a web
        // client users move between in the same session, and recolouring the phone to
        // match its wallpaper would leave the indigo/amber identity surviving only on
        // devices whose wallpaper happens to be blue. SchoolTheme takes a flag for it,
        // and the hero and shimmer colours follow the resolved scheme either way, so
        // turning it on later is one argument rather than an audit.
        //
        // Carries 1.5.0 before it (one section per class and My Timetable), 1.4.0 before
        // that (the class module on the phone), 1.3.0 before that (passcode sign-in and
        // recovery). A new versionCode is what lets the device recognise this as an
        // upgrade rather than refusing to install over the previous build.
        versionCode = 14
        versionName = "1.13.0"

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
