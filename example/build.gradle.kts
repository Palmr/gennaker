plugins {
    id("java-conventions")
}

spotbugs {
    onlyAnalyze = listOf("uk.co.palmr.example")
}

dependencies {
    annotationProcessor(project(":gennaker-annotation-processor"))
    implementation(project(":gennaker-annotation-processor"))

    implementation(project(":gennaker-annotations"))
    implementation(project(":gennaker-core"))
}