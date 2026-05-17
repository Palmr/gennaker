plugins {
    id("java-conventions")
    id("publishing-conventions")
}

description = "JSON codec for Gennaker — human-readable wire format, zero external runtime dependencies"

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
    compileOnly("com.google.auto.service:auto-service:1.1.1")

    implementation(project(":libs:annotations"))
    implementation(project(":libs:codec-spi"))

    testImplementation(libs.bundles.testing)
    testImplementation(libs.agrona)
    testImplementation(project(":libs:annotation-processor"))
    testImplementation(project(":libs:layout-api"))
}
