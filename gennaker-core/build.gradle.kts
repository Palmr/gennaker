plugins {
    id("java-conventions")
}

dependencies {
    api(project(":gennaker-annotations"))

    testImplementation(libs.bundles.testing)

    testAnnotationProcessor(project(":gennaker-annotation-processor"))
    testImplementation(project(":gennaker-annotation-processor"))
    api(libs.agrona)

    implementation(libs.aeron)
    implementation(libs.slf4j)
    implementation(libs.logback)
}
