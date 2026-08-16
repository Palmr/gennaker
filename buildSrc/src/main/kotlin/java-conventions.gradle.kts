//
// Java conventions...
//

plugins {
    `java-library`
    `checkstyle`
    id("com.github.spotbugs")
}

checkstyle {
    maxWarnings = 0
    toolVersion = "13.10.0"
}

repositories {
    mavenCentral()
}

dependencies {
    // No default dependencies?
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

tasks.spotbugsTest {
    enabled = false
}

tasks.spotbugsMain {
    excludeFilter = rootProject.layout.projectDirectory.file("config/spotbugs/exclude.xml").asFile
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}