plugins {
    id("java-conventions")
}

group = "uk.co.palmr.gennaker.example"

spotbugs {
    onlyAnalyze = listOf("uk.co.palmr.example")
}

dependencies {
    annotationProcessor(project(":libs:annotation-processor"))
    annotationProcessor(project(":libs:message-codec-sbe"))
    implementation(project(":libs:annotation-processor"))

    implementation(project(":libs:annotations"))
    implementation(project(":libs:core"))
}