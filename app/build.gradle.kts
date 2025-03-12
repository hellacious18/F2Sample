plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id ("com.google.gms.google-services") // Ensure this is added

}

android {
    namespace = "com.example.f2sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.f2sample"
        minSdk = 24
        targetSdk = 35
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
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
//    classpath ("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")

    implementation("com.google.firebase:firebase-auth:23.2.0")
    runtimeOnly("com.google.gms:google-services:4.4.2")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.google.firebase:firebase-bom:33.9.0")
    implementation("com.google.firebase:firebase-firestore:25.1.2")
    implementation ("com.google.firebase:firebase-storage:20.3.0")
    implementation ("com.google.firebase:firebase-appcheck-debug:17.1.0")
    implementation ("com.google.firebase:firebase-appcheck-playintegrity:17.0.1")  // For production
    implementation("com.google.ai.client.generativeai:generativeai:0.1.1")
    implementation ("com.google.firebase:firebase-ml-vision:24.0.3")




    implementation("com.google.firebase:firebase-vertexai:16.1.0")

    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")

    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation ("io.noties.markwon:core:4.6.2")

    implementation ("com.squareup.okhttp3:okhttp:4.9.0")
    implementation ("com.github.bumptech.glide:glide:4.16.0")

}
