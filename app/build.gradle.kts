plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.gasmonsoft.fbccalidad"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gasmonsoft.fbccalidad"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "URL_API",
                "\"https://fuelsoftwarecontrol.com/\""
            )
            buildConfigField("String", "DB_NAME", "\"fuelboxcontrol.db\"")
        }
        release {
            isMinifyEnabled = false
            buildConfigField(
                "String",
                "URL_API",
                "\"https://fuelsoftwarecontrol.com/\""
            )

            buildConfigField("String", "DB_NAME", "\"fuelboxcontrol.db\"")
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime.livedata)

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation(libs.kotlinx.serialization.json)

    // Dependency Injection - Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")
    implementation("androidx.hilt:hilt-work:1.3.0")
    ksp("androidx.hilt:hilt-compiler:1.3.0")
    implementation(libs.androidx.work.runtime.ktx)

    // Location & Permissions
    implementation(libs.play.services.location)
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Room
    val roomVersion = "2.7.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Networking
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation(libs.okhttp.logging)

    implementation("androidx.compose.material:material-icons-extended")

    // Herramientas de Matemáticas
    implementation("org.apache.commons:commons-math3:3.6.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    // Use testImplementation for local unit tests
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    // Use androidTestImplementation for instrumented tests
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    // Excel
    implementation("org.apache.poi:poi-ooxml:5.5.1")

    // DataStore
    val dataStore = "1.2.1"
    implementation("androidx.datastore:datastore-preferences:$dataStore")
    implementation("androidx.datastore:datastore:$dataStore")
}
