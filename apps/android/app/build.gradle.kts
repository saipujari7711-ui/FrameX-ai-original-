plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.plugin.compose")
}

val googleWebClientId = providers.gradleProperty("GOOGLE_WEB_CLIENT_ID")
  .orElse(providers.environmentVariable("GOOGLE_WEB_CLIENT_ID"))
  .orElse("")

android {
  namespace = "ai.framex.app"
  compileSdk = 37

  defaultConfig {
    applicationId = "ai.framex.app"
    minSdk = 26
    targetSdk = 37
    versionCode = 2
    versionName = "0.2.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    resValue("string", "google_web_client_id", googleWebClientId.get())
  }

  buildFeatures {
    compose = true
    buildConfig = true
    resValues = true
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

dependencies {
  val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
  implementation(composeBom)
  androidTestImplementation(composeBom)

  implementation("androidx.activity:activity-compose:1.13.0")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-tooling-preview")
  debugImplementation("androidx.compose.ui:ui-tooling")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
  implementation("androidx.credentials:credentials:1.6.0")
  implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
  implementation("com.google.android.libraries.identity.googleid:googleid:1.2.1")
  implementation("com.google.android.gms:play-services-auth:22.0.0")
  implementation("androidx.work:work-runtime-ktx:2.12.0")

  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.compose.ui:ui-test-junit4")
  androidTestImplementation("androidx.test:rules:1.6.1")
  debugImplementation("androidx.compose.ui:ui-test-manifest")
}
