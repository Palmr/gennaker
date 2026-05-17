//
// Custom Gradle build scripts
//
plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.0.18")
    implementation("com.gradleup.nmcp:nmcp:0.1.3")
}