plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
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
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true
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
    implementation(files("C:/Users/bwood/AppData/Local/Android/Sdk/platforms/android-36/android.jar"))
    //Phone number authentication
    implementation("com.googlecode.libphonenumber:libphonenumber:8.13.27")

    //Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.11.0")

    //Android Testing
    androidTestImplementation("org.mockito:mockito-android:4.11.0")
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.navigation:navigation-testing:2.7.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")

    androidTestUtil("androidx.test:orchestrator:1.4.2")
    androidTestImplementation("androidx.fragment:fragment-testing:1.6.2")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.firebase.auth)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.google.firebase.messaging)

    // Bottom sheet pickers. Implementation was tried.
    implementation(libs.calendar)
    implementation(libs.clock)
    implementation(libs.color)

    // QR code generation
    implementation(libs.qrcodegen)

    // Instrumentation testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation (libs.google.firebase.firestore)

    // google maps dependency
    implementation(libs.play.services.maps)
    implementation(libs.firebase.messaging)
    implementation("com.google.android.gms:play-services-location:21.3.0")
}
