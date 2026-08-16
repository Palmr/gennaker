plugins {
    `java-library`
    `maven-publish`
    `signing`
    id("com.gradleup.nmcp")
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = "gennaker-${project.name}"
            version = project.version.toString()

            pom {
                name = "gennaker-${project.name}"
                description = provider { project.description ?: "Gennaker — ${project.name}" }
                url = "https://github.com/Palmr/gennaker"

                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }

                developers {
                    developer {
                        id = "Palmr"
                        name = "Nick Palmer"
                        email = "nick@palmr.co.uk"
                    }
                }

                scm {
                    connection = "scm:git:https://github.com/Palmr/gennaker.git"
                    developerConnection = "scm:git:ssh://github.com:Palmr/gennaker.git"
                    url = "https://github.com/Palmr/gennaker"
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
}
