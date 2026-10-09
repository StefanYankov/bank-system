rootProject.name = "bank-system"

// --- Shared libraries ---
include("libs:error-handling-starter")

// --- Business services ---
include("services:employee-service")
// include("services:customer-service")
// include("services:account-service")
// include("services:loan-service")

// --- Platform services ---
// include("platform:api-gateway")
// include("platform:config-server")
// include("platform:discovery-server")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}