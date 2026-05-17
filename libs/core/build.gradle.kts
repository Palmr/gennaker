plugins {
    id("java-conventions")
    id("publishing-conventions")
}

description = "Gennaker runtime — publisher and subscriber entry point, DirectTransport and AeronTransport"

dependencies {
    api(project(":libs:annotations"))

    testImplementation(libs.bundles.testing)

    testAnnotationProcessor(project(":libs:annotation-processor"))
    testAnnotationProcessor(project(":libs:message-codec-sbe"))
    testImplementation(project(":libs:annotation-processor"))
    api(libs.agrona)

    implementation(libs.aeron)
    implementation(libs.slf4j)
    implementation(libs.logback)
}
