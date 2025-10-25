plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.rocket.radar"
    compileSdk = 36

    buildFeatures {
        // This enables the generation of classes that basically contain all elements of a view by
        // id so that findViewById only needs to be called rarely.
        // Add `tools:viewBindingIgnore="true" to the root of layout files we don't want to bind.
        // For more information see: https://developer.android.com/topic/libraries/view-binding
        viewBinding = true
        
        // This enables automatic updating of views from our view model classes.
        // For more information see: https://developer.android.com/topic/libraries/data-binding
        dataBinding = true
    }

    defaultConfig {
        applicationId = "com.rocket.radar"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)

    // Instrumentation testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}