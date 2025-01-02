plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor(project(":gennaker-annotation-processor"))
    implementation(project(":gennaker-annotation-processor"))

    implementation(project(":gennaker-annotations"))
    implementation(project(":gennaker-core"))
}

tasks.withType<JavaExec> {
    jvmArgs("--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED")
}
tasks.withType<Test> {
    jvmArgs("--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED")
}
