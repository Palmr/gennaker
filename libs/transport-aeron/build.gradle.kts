plugins {
    id("java-conventions")
    id("publishing-conventions")
}

description = "Aeron-backed Transport for Gennaker — IPC / multi-process delivery via an embedded Aeron media driver"

dependencies {
    api(project(":libs:core"))

    implementation(libs.aeron)
    // SLF4J is pulled in transitively by aeron-all for Aeron's media driver
    // logging. We do not pick a binding here — consumers choose their own
    // (logback, log4j2, slf4j-simple, ...). Our own code logs via System.Logger.

    testImplementation(libs.bundles.testing)
    testAnnotationProcessor(project(":libs:annotation-processor"))
    testAnnotationProcessor(project(":libs:message-codec-sbe"))
    testRuntimeOnly(libs.logback)
}
