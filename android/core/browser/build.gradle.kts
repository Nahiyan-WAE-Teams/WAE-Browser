plugins {
    id("com.android.library")
}

android {
    namespace = "com.wae.browser.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.datastore:datastore-preferences:1.3.0")
}
