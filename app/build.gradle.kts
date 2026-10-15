plugins { id("com.android.application") }
android {
 namespace = "org.aletheia.reader"
 compileSdk = 35
 defaultConfig {
  applicationId = "org.aletheia.reader"
  minSdk = 26
  targetSdk = 35
  versionCode = 3
  versionName = "0.3.0-reader-audio-repair"
 }
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }
}
