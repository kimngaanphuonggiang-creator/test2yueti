import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.yueti.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.yueti.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "0.8.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Produce an installable offline evaluation APK; replace this with the
            // product keystore before store distribution.
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        jniLibs.useLegacyPackaging = true
    }

    lint {
        checkTestSources = false
        ignoreTestSources = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    kapt("androidx.room:room-compiler:2.8.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    // Material 3 Expressive APIs: MotionScheme, expressive loading, button groups,
    // split buttons, floating toolbars, FAB menus and adaptive navigation surfaces.
    implementation("androidx.compose.material3:material3:1.5.0-alpha26")
    implementation("androidx.compose.material:material-icons-extended")
    // Official Material Components Android library from material-components/material-components-android.
    // MotionUtils below bridges its canonical motion tokens into Compose animations.
    implementation("com.google.android.material:material:1.13.0")
    implementation("com.airbnb.android:lottie-compose:6.7.1")
    implementation("net.objecthunter:exp4j:0.4.8")
    implementation("org.opencv:opencv:4.12.0")

    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.room:room-testing:2.8.4")

    testImplementation("junit:junit:4.13.2") {
        exclude(group = "org.hamcrest", module = "hamcrest-core")
    }
    testImplementation(files("libs/hamcrest-core-1.3.jar"))
    debugImplementation("androidx.compose.ui:ui-tooling")
}

kapt {
    correctErrorTypes = true
}
