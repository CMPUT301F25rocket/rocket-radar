import java.io.IOException

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.rocket.radar"
    compileSdk = 36

    sourceSets {
        getByName("test") {
            java.srcDir("src/testShared/java")
        }
        getByName("androidTest") {
            java.srcDir("src/testShared/java")
        }
    }

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

    // Complains about final when not abstract. It's magic. It's a feature. It's a magic feature.
    abstract class StartFirebaseEmulation : DefaultTask() {
        @Internal
        var emulatorProcess: Process? = null

        @TaskAction
        fun startEmulators() {
            try {
                val process = ProcessBuilder("firebase", "emulators:start")
                    .redirectErrorStream(true)
                    .start()
                val lines = process.inputStream.bufferedReader().lines()
                for (line in lines) {
                    logger.lifecycle(line)
                    if (line.contains("All emulators ready!", ignoreCase = true)) break;
                }
                // Hmm yes, java rust.
                emulatorProcess = if (process.isAlive) process else null
            } catch (e: IOException) {
                logger.error("Dear Bozo, you don't have `firebase` installed. You can install it with `npm install -g firebase-tools`. Yours sincerly, Gradle.")
                logger.error(e.toString())
            }
        }
    }

    // The alternative to the gradle task was running all the test through terminal wrapping with
    // firebase emulators:exec ..., but I that's annoying and I didn't want to overwrite the builtin
    // test tasks.
    //
    tasks.register<StartFirebaseEmulation>("startFirebaseEmulation")
    tasks.register("stopFirebaseEmulation") {
        val startFirebaseEmulation = tasks.named<StartFirebaseEmulation>("startFirebaseEmulation")
        mustRunAfter(startFirebaseEmulation)
        doLast {
            startFirebaseEmulation.get().emulatorProcess?.destroy()
        }
    }
    // Only start Firebase emulator for instrumented tests (connectedAndroidTest, deviceAndroidTest)
    tasks.matching { it.name.contains("AndroidTest") }.configureEach {
        dependsOn("startFirebaseEmulation")
        finalizedBy("stopFirebaseEmulation")
    }

    buildTypes {
        debug {
            // Include parameter names in debug builds for better UML diagrams
            tasks.withType<JavaCompile> {
                if (name.contains("Debug")) {
                    options.compilerArgs.add("-parameters")
                }
            }
        }
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
    //implementation(files("C:/Users/bwood/AppData/Local/Android/Sdk/platforms/android-36/android.jar"))
    //Phone number authentication
    implementation("com.googlecode.libphonenumber:libphonenumber:8.13.27")

    //Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.robolectric:robolectric:4.11.1")

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

    implementation("androidx.annotation:annotation:1.7.0")

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
