rootProject.name = "gennaker"
include(
    ":example",
    ":libs:annotation-processor",
    ":libs:annotations",
    ":libs:codec-spi",
    ":libs:core",
    ":libs:layout-api",
    ":libs:message-codec-json",
    ":libs:message-codec-sbe",
    ":libs:transport-aeron",
    ":libs:transport-spi"
)
