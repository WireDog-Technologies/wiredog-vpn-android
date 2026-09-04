plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.wiredog.vpn"
    compileSdk = 36

    signingConfigs {
        create("release") {
            // Read local.properties file
            val localPropsFile = rootProject.file("local.properties")
            val props = mutableMapOf<String, String>()

            if (localPropsFile.exists()) {
                localPropsFile.readLines().forEach { line ->
                    if (!line.startsWith("#") && line.contains("=")) {
                        val (key, value) = line.split("=", limit = 2)
                        props[key.trim()] = value.trim()
                    }
                }
            }

            storeFile = rootProject.file("wiredog-vpn.keystore")
            storePassword = props["keystore.password"] ?: ""
            keyAlias = props["key.alias"] ?: ""
            keyPassword = props["key.password"] ?: ""
        }
    }

    defaultConfig {
        applicationId = "com.wiredog.vpn"
        minSdk = 28
        targetSdk = 36
        versionCode = 10
        versionName = "1.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "WIREDOG_API_BASE_URL", "\"https://api.wiredogvpn.com/api\"")
        buildConfigField("String", "WIREDOG_URL_DASHBOARD", "\"https://www.wiredogvpn.com/dashboard\"")
        buildConfigField("String", "WIREDOG_URL_PRIVACY", "\"https://www.wiredogvpn.com/legal/privacy\"")
        buildConfigField("String", "WIREDOG_URL_TERMS", "\"https://www.wiredogvpn.com/legal/terms-of-service\"")
        buildConfigField("String", "WIREDOG_URL_GET_STARTED", "\"https://www.wiredogvpn.com/get-started\"")
        buildConfigField("String", "WIREDOG_URL_CHECKOUT", "\"https://www.wiredogvpn.com/checkout\"")
        buildConfigField("String", "WIREDOG_APP_STORE_URL", "\"https://play.google.com/store/apps/details?id=com.wiredog.vpn\"")
    }

    buildTypes {
        debug {
            // Android Studio's Run button always builds the debug variant, so running
            // from the IDE hits the integration backend automatically. Release builds
            // (APK/bundle) use the production URL from defaultConfig instead.
            buildConfigField("String", "WIREDOG_API_BASE_URL", "\"https://intapi.wiredogvpn.com/api\"")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
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
        resources {
            excludes += listOf(
                "META-INF/proguard/androidx-*.pro",
                "META-INF/com.google.dagger_dagger.version",
                "META-INF/MANIFEST.MF",
                "META-INF/*.kotlin_module",
                "META-INF/kotlinx_coroutines_core.version"
            )
        }
    }
    lint {
        checkReleaseBuilds = true
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    ksp(libs.moshi.codegen)

    // DataStore
    implementation(libs.datastore.preferences)

    // Security
    implementation(libs.security.crypto)

    // AmneziaWG — local AAR (JitPack builds are broken; build from source:
    //   git clone --recursive https://github.com/amnezia-vpn/amneziawg-android.git
    //   ./gradlew :tunnel:assembleRelease
    //   copy tunnel/build/outputs/aar/tunnel-release.aar → app/libs/amneziawg-tunnel.aar)
    implementation(files("libs/amneziawg-tunnel.aar"))

    // Coil (images + SVG)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

    // Accompanist
    implementation(libs.accompanist.drawablepainter)

    // Coroutines
    implementation(libs.coroutines.android)

    // Play In-App Review
    implementation(libs.play.review.ktx)

    // Desugaring (required by WireGuard tunnel library)
    coreLibraryDesugaring(libs.desugar)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
