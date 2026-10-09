plugins {
    id("bank.library-conventions")
}

description = "RFC 9457 problem-details error handling for bank-system services"

dependencies {
    implementation(libs.spring.boot.autoconfigure)

    compileOnly(libs.spring.boot.starter.webmvc)
    compileOnly(libs.spring.boot.starter.security)
    compileOnly(libs.spring.data.commons)
    compileOnly(libs.spring.boot.starter.security.oauth2.resource.server)

    testImplementation(libs.spring.boot.starter.security.oauth2.resource.server)
    testImplementation(libs.spring.data.commons)
    testImplementation(libs.spring.boot.starter.validation)
    testImplementation(libs.spring.boot.starter.webmvc)
    testImplementation(libs.spring.boot.starter.security)
    testImplementation(libs.bundles.testing)
}