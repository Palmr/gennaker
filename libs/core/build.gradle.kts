plugins {
    id("java-conventions")
    id("publishing-conventions")
}

description = "Gennaker runtime — Gennaker entry point, Transport SPI, MessageHandler, and DirectTransport"

dependencies {
    api(project(":libs:annotations"))
    api(project(":libs:transport-spi"))
    api(libs.agrona)

    testImplementation(libs.bundles.testing)

    testAnnotationProcessor(project(":libs:annotation-processor"))
    testAnnotationProcessor(project(":libs:message-codec-sbe"))
    testImplementation(project(":libs:annotation-processor"))
}
