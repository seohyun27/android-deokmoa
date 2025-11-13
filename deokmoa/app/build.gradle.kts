plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // 추가
    id("kotlin-kapt")
}

android {
    namespace = "com.example.deokmoa"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.deokmoa"
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
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Room (Database)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    // Coroutines-Kotlin Extensions for Room
    implementation("androidx.room:room-ktx:$roomVersion")

    // Lifecycle (LiveData & ViewModel - Room과 함께 사용)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Activity KTX (ActivityResultLauncher 사용을 위해)
    implementation("androidx.activity:activity-ktx:1.9.0")

    // 이미지를 쉽게 로드하기 위한 라이브러리 (선택 사항이지만 권장)
    implementation("io.coil-kt:coil:2.6.0")
}