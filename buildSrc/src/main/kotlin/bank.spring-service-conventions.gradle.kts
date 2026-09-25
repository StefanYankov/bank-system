plugins {
    id("bank.java-conventions")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libs.findVersion("springCloud").get()}")
    }
}

dependencies {
    add("implementation", "org.springframework.boot:spring-boot-starter-actuator")
    add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
    add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
}

tasks.named<Jar>("jar") {
    enabled = false
}