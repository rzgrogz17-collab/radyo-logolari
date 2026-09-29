import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.mahjongmaster"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.classicgame.mahjong"
        minSdk = 24
        targetSdk = 35
        versionCode = 31
        versionName = "2.31"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // Temel Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose BOM — tüm Compose kütüphaneleri aynı sürümde
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // ============================================================
    // REKLAM SDK'LARI — hangisinin kullanılacağına AdConfig.kt karar verir
    // ============================================================
    // --- YANDEX ADS (RU, TR, KZ, BY, UZ, AM, KG, AZ, TJ, GE, MD, RS) ---
    implementation("com.yandex.android:mobileads:7.4.0")
    // --- HUAWEI PETAL ADS (listedeki ülkeler DIŞINDAKİ tüm ülkeler) ---
    implementation("com.huawei.hms:ads-lite:13.4.91.300")
    // --- GOOGLE ADMOB (AdConfig.USE_YANDEX_AND_HUAWEI = false olduğunda) ---
    implementation("com.google.android.gms:play-services-ads:25.5.0")
    // AdMob için GDPR/KVKK onay formu (Google UMP) — AB/İngiltere kullanıcıları için
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")

    // Resim ve İkon Kütüphaneleri
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-svg:2.5.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    // viewModelScope için (eşleşme animasyonunun zamanlamasında kullanılıyor)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")

    // Testler
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
