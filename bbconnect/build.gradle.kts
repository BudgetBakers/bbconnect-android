plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    `maven-publish`
}

android {
    namespace = "com.budgetbakers.bbconnect"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    publishing { singleVariant("release") }
}

dependencies {
    implementation("androidx.browser:browser:1.8.0")
    // ReturnUrl parsing is pure java.net — unit-testable on the JVM without
    // Robolectric or an emulator.
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.code.gson:gson:2.11.0")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.budgetbakers"
            artifactId = "bbconnect"
            version = "0.1.0"
            afterEvaluate { from(components["release"]) }
        }
    }
}
