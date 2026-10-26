plugins { id("com.android.application") version "9.3.0" }
android {
    namespace = "com.pulse.fitness"
    compileSdk { version = release(37) }
    defaultConfig {
        applicationId = "com.pulse.fitness"
        minSdk = 26
        targetSdk = 37
        versionCode = 5
        versionName = "1.2.2"
    }
    buildTypes {
        debug { }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint { disable += setOf("HardcodedText", "SetTextI18n", "OldTargetApi") }
}
dependencies { implementation(project(":core")) }
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }
