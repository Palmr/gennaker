plugins {
    id("java-conventions")
    id("publishing-conventions")
}

description = "JSON codec for Gennaker — human-readable wire format, zero external runtime dependencies"

tasks.javadoc {
    // Only JsonMessageCodecGenerator is a documented SPI entry point.
    // JsonReader and JsonWriter must stay public because the generated
    // __layout helper classes (emitted into the user's package) call them
    // across the package boundary, but they are not part of the user-facing
    // API and are not worth documenting.
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

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
