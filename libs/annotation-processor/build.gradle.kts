plugins {
    id("java-conventions")
    id("publishing-conventions")
}

description = "Annotation processor that generates publisher and subscriber proxies for @Topic interfaces"

tasks.javadoc {
    // Module contents are consumed via the annotationProcessor Gradle
    // configuration; only TopicAnnotationProcessor is a documented SPI
    // entry point. Silence doclint for the internal proxy/shape helpers.
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
    compileOnly("com.google.auto.service:auto-service:1.1.1")

    implementation(project(":libs:annotations"))
    implementation(project(":libs:codec-spi"))
    implementation(project(":libs:layout-api"))

    testImplementation(libs.bundles.testing)
    testImplementation(libs.compileTesting)
    testImplementation(libs.agrona)
    testImplementation(project(":libs:transport-spi"))
}
