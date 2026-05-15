plugins {
    id("java-conventions")
}

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
    compileOnly("com.google.auto.service:auto-service:1.1.1")

    implementation(project(":libs:annotations"))
    implementation(project(":libs:codec-spi"))

    testImplementation(libs.bundles.testing)
    testImplementation(libs.compileTesting)
    testImplementation(libs.agrona)
}
