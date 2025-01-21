plugins {
    id("java-conventions")
}

dependencies {
    annotationProcessor(project(":gennaker-annotation-processor"))
    implementation(project(":gennaker-annotation-processor"))

    implementation(project(":gennaker-annotations"))
    implementation(project(":gennaker-core"))
}