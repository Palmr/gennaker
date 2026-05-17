plugins {
    id("java-conventions")
}

group = "uk.co.palmr.gennaker.example"

tasks.javadoc {
    enabled = false
}

tasks.spotbugsMain {
    // Scope analysis to hand-written sources. Generated proxies and SBE codecs
    // sit alongside them in build/classes/java/main, but pull in agrona SBE
    // flyweight references that aren't on SpotBugs' auxClasspath and aren't
    // ours to lint anyway.
    val srcDir = file("src/main/java")
    classes = sourceSets["main"].output.classesDirs.asFileTree.matching {
        srcDir.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .map { it.relativeTo(srcDir).invariantSeparatorsPath.removeSuffix(".java") }
            .forEach { className ->
                include("$className.class")
                include("$className\$*.class")
            }
    }
}

dependencies {
    annotationProcessor(project(":libs:annotation-processor"))
    annotationProcessor(project(":libs:message-codec-sbe"))
    annotationProcessor(project(":libs:message-codec-json"))
    implementation(project(":libs:annotation-processor"))

    implementation(project(":libs:annotations"))
    implementation(project(":libs:core"))
    implementation(project(":libs:message-codec-json"))
    implementation(project(":libs:transport-aeron"))

    // Library code logs via System.Logger. Pick a runtime binding here so the
    // example actually shows logs; consumers of gennaker pick whichever binding
    // they like.
    runtimeOnly(libs.logback)
}