plugins {
    id("java-conventions")
}

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
