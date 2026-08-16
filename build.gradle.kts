plugins {
    `idea`
    id("com.gradleup.nmcp.aggregation")
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

// Publishes every library as a single Central Portal deployment:
//   ./gradlew publishAggregationToCentralPortal
nmcpAggregation {
    centralPortal {
        username = providers.gradleProperty("centralUsername")
        password = providers.gradleProperty("centralPassword")
        publishingType = "USER_MANAGED"
    }
}

dependencies {
    nmcpAggregation(project(":libs:annotation-processor"))
    nmcpAggregation(project(":libs:annotations"))
    nmcpAggregation(project(":libs:codec-spi"))
    nmcpAggregation(project(":libs:core"))
    nmcpAggregation(project(":libs:layout-api"))
    nmcpAggregation(project(":libs:message-codec-json"))
    nmcpAggregation(project(":libs:message-codec-sbe"))
    nmcpAggregation(project(":libs:transport-aeron"))
    nmcpAggregation(project(":libs:transport-spi"))
}
