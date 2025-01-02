plugins {
    id("java-library")
}

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
    compileOnly("com.google.auto.service:auto-service:1.1.1")

    implementation(project(":gennaker-annotations"))

    testImplementation(libs.bundles.testing)

    implementation(libs.sbe)
    api(libs.agrona)
    api(libs.aeron)
}

tasks.test {
    useJUnitPlatform()
}
