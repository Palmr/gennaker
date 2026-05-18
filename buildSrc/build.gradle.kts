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
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.5.4")
    implementation("com.gradleup.nmcp:nmcp:0.1.3")
}