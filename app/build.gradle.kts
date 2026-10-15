plugins { id("com.android.application") }
android {
 namespace = "org.aletheia.reader"
 compileSdk = 35
 defaultConfig {
  applicationId = "org.aletheia.reader"
  minSdk = 26
  targetSdk = 35
  versionCode = 4
  versionName = "0.4.0-native-reader-rebuild"
 }
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }
}

// PDF documents are processed on-device only. No content is uploaded.
dependencies {
 implementation("com.tom-roush:pdfbox-android:2.0.27.0")
}
