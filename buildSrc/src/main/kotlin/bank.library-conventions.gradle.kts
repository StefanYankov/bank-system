import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    id("bank.java-conventions")
    `java-library`
}

dependencies {
    add("implementation", platform(SpringBootPlugin.BOM_COORDINATES))
    add("annotationProcessor", platform(SpringBootPlugin.BOM_COORDINATES))

    add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
}