plugins {
    id("bank.spring-service-conventions")
}

description = "Customer-owned bank accounts, balances, and transaction history"

dependencies {
    implementation(project(":libs:error-handling-starter"))
    implementation(project(":libs:common-identifiers"))

    implementation(libs.bundles.web)
    implementation(libs.bundles.persistence)
    implementation(libs.bundles.security)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.spring.boot.starter.restclient)
    implementation(libs.resilience4j.spring.boot4)
    
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.lombok.mapstruct.binding)

    runtimeOnly(libs.postgresql)

    testImplementation(libs.bundles.testing)
    testImplementation(libs.bundles.integration.testing)
    testImplementation(libs.wiremock.spring.boot)
    testImplementation(libs.spring.boot.starter.restclient.test)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}
