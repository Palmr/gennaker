plugins {
    id("java-conventions")
}

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
    compileOnly("com.google.auto.service:auto-service:1.1.1")

    implementation(project(":libs:annotations"))

    testImplementation(libs.bundles.testing)

    implementation(libs.sbe)
    api(libs.agrona)
    api(libs.aeron)
}