plugins {
    id("com.android.application") version "9.5.0" apply false
    id("com.android.library") version "9.5.0" apply false
    // AGP 9.5.0's documented Kotlin-compatible baseline is 2.4.10.
    // Kotlin 2.4.21 is newer, but its published AGP compatibility range
    // does not include AGP 9.5.0.
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
