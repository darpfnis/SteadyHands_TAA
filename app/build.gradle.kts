plugins {
    alias(libs.plugins.android.application)
    // Використовуємо класичний annotationProcessor для Java проєкту,
    // щоб не ускладнювати налаштування KSP, якщо ви пишете на Java.
}

android {
    // Namespace має відповідати тому, що ми пропишемо в маніфесті
    namespace = "com.taa.project"

    // Оновлюємо до 36, щоб зникли помилки "requires API 36 or later"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.taa.project"
        minSdk = 26 // Відповідно до ТЗ для фонових сервісів
        targetSdk = 34 // Лишаємо стабільну поведінку Android 14

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
    buildToolsVersion = "34.0.0"
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Використовуємо стабільні версії 1.13.0 або 1.12.0, які не просять SDK 36
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.google.android.material:material:1.9.0")

    // Room та інші лишаємо як були
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")
}