plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.rocket.radar"
    compileSdk = 34 // Use SDK 34, which is the stable standard.

    defaultConfig {
        applicationId = "com.rocket.radar"
        minSdk = 24
        targetSdk = 34 // Target the stable SDK.
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8 // Use Java 8, the standard for broad compatibility.
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    // This is often needed for test resources to be packaged correctly.
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // --- Core Android UI & App Compat (UPGRADED) ---
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // This version is stable and fine.
    implementation("androidx.recyclerview:recyclerview:1.3.2")       // This version is stable and fine.

    // --- Image Loading (Stable Glide version) ---
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // --- Firebase (Stable BoM) ---
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")


    // =================================================================================
    //  TESTING DEPENDENCIES (Using Latest Stable Versions)
    // =================================================================================

    // --- Local Unit Tests (JVM) ---
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    // --- Instrumentation Tests (Android Device/Emulator) ---
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1")

    androidTestImplementation(platform("com.google.firebase:firebase-bom:33.1.2"))


    // Both are required for fragment testing.
    androidTestImplementation("androidx.fragment:fragment-testing:1.8.0")
    debugImplementation("androidx.fragment:fragment-testing:1.8.0")
}
