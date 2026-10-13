import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    id("bank.java-conventions")
    id("org.springframework.boot")
}

dependencies {
    add("implementation", platform(SpringBootPlugin.BOM_COORDINATES))
    add("annotationProcessor", platform(SpringBootPlugin.BOM_COORDINATES))
    add("testAnnotationProcessor", platform(SpringBootPlugin.BOM_COORDINATES))

    add("implementation", "org.springframework.boot:spring-boot-starter-actuator")
    add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
}

springBoot {
    buildInfo()
}

tasks.named<Jar>("jar") {
    enabled = false
}