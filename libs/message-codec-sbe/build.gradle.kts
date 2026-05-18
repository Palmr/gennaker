plugins {
    id("java-conventions")
    id("publishing-conventions")
}

description = "SBE codec for Gennaker — allocation-free binary wire format using Simple Binary Encoding"

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
    compileOnly("com.google.auto.service:auto-service:1.1.1")

    implementation(project(":libs:annotations"))
    implementation(project(":libs:codec-spi"))

    implementation(libs.sbe)
    implementation(libs.agrona)

    testImplementation(libs.bundles.testing)
    testImplementation(libs.compileTesting)
    testImplementation(project(":libs:annotation-processor"))
    testImplementation(project(":libs:transport-spi"))
}
