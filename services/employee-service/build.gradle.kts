plugins {
	id("bank.spring-service-conventions")
}

description = "Employee profiles and employment status for bank staff"

dependencies {
	implementation(project(":libs:error-handling-starter"))
	implementation(project(":libs:common-identifiers"))

	implementation(libs.bundles.web)
	implementation(libs.bundles.persistence)
	implementation(libs.bundles.security)
	implementation(libs.springdoc.openapi.starter.webmvc.ui)

	implementation(libs.mapstruct)
	annotationProcessor(libs.mapstruct.processor)

	compileOnly(libs.lombok)
	annotationProcessor(libs.lombok)
	annotationProcessor(libs.lombok.mapstruct.binding)

	runtimeOnly(libs.postgresql)

	testImplementation(libs.bundles.testing)
	testImplementation(libs.bundles.integration.testing)
	testCompileOnly(libs.lombok)
	testAnnotationProcessor(libs.lombok)
}