plugins {
    id("java-conventions")
    id("publishing-conventions")
}

description = "Gennaker transport SPI — Transport and MessageHandler, implemented by transport providers and generated proxies"

dependencies {
    api(libs.agrona)
}
