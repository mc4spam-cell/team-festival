plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.mc.mateamhf"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mc.teamfestival"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        val syncUrl = (project.findProperty("mateamhf.syncUrl") as String?) ?: ""
        buildConfigField("String", "SYNC_URL", "\"$syncUrl\"")

        // Spotify Web API client_id — needed for OAuth + playlist generation feature.
        // Register your own app at https://developer.spotify.com/dashboard, set the
        // redirect URI to "com.mc.teamfestival://oauth/spotify", and drop the client_id
        // into ~/.gradle/gradle.properties as teamfestival.spotifyClientId=<value>.
        // The feature is silently disabled when the field is empty.
        val spotifyClientId = (project.findProperty("teamfestival.spotifyClientId") as String?) ?: ""
        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"$spotifyClientId\"")
    }

    signingConfigs {
        create("release") {
            val ksPath = project.findProperty("mateamhf.releaseKeystorePath") as String?
            if (!ksPath.isNullOrBlank() && file(ksPath).exists()) {
                storeFile = file(ksPath)
                storePassword = project.findProperty("mateamhf.releaseKeystorePassword") as String
                keyAlias = project.findProperty("mateamhf.releaseKeyAlias") as String
                keyPassword = project.findProperty("mateamhf.releaseKeyPassword") as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Sign with our release key when the keystore is configured; falls back to
            // unsigned release otherwise (build will warn, install impossible).
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
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
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)

    implementation(libs.okhttp)

    // Chrome Custom Tabs — used to launch OAuth flows (Spotify) and let the browser
    // share the user's logged-in session instead of trapping them in a WebView.
    implementation("androidx.browser:browser:1.8.0")

    // Firebase BOM aligns all Firebase artifact versions
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Modern Google Sign-In via Credential Manager
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.id)
    implementation(libs.kotlinx.coroutines.play.services)
}
