plugins {
    id("java-conventions")
    id("publishing-conventions")
}

description = "Aeron-backed Transport for Gennaker — IPC / multi-process delivery via an embedded Aeron media driver"

dependencies {
    api(project(":libs:core"))

    implementation(libs.aeron)
    implementation(libs.slf4j)
    implementation(libs.logback)

    testImplementation(libs.bundles.testing)
    testAnnotationProcessor(project(":libs:annotation-processor"))
    testAnnotationProcessor(project(":libs:message-codec-sbe"))
}
