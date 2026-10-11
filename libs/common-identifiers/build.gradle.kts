plugins {
    id("bank.library-conventions")
}

description = "Shared identifier generation utilities"

dependencies {
    implementation(libs.uuid.creator)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
}