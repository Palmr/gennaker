plugins {
    id("java-library")
    id("checkstyle")
    id("idea")
}

checkstyle {
    maxWarnings = 0
    toolVersion = libs.versions.checkstyleVersion.get()
}

repositories {
    mavenCentral()
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

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaExec> {
    jvmArgs("--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED")
}
tasks.withType<Test> {
    jvmArgs("--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED")
}
