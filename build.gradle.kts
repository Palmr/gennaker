plugins {
    `idea`
}

allprojects {
    group = "uk.co.palmr.gennaker"
    version = "1.0.0-SNAPSHOT"
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}