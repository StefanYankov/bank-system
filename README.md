# Bank System

> [!IMPORTANT]
> **Project Status: In Development**
> This repository contains the **final project** for the "CSCB634 Practicum in Programming and Implementation of Databases" module at **New Bulgarian University**.

---

## Overview
Staff-facing banking system for managing customers, accounts and loans with annuity repayment schedules. Built as Spring Boot microservices with an Angular frontend and Keycloak for identity.

Current state: `employee-service` is implemented through the persistence, application, and REST API layers.

## Architecture
| Component | Owns | Status |
|---|---|---|
| `employee-service` | Staff profiles and employment lifecycle | Implemented |
| `customer-service` | Individual and legal-entity customers | Planned |
| `account-service` | Accounts and balances | Planned |
| `loan-service` | Loans, schedules and instalment payments | Planned |
| Keycloak | Authentication, credentials, roles, and API clients | Configured |

Each service owns its PostgreSQL database. Services do not access another service's database directly.

## Technology Stack
| Area | Technology |
|---|---|
| Backend | Java 25, Spring Boot 4.1.1 |
| Build | Gradle 9.6 with Kotlin DSL |
| Database | PostgreSQL 18.6, Flyway |
| Identity | Keycloak 26.7.4 (OAuth2 / OIDC) |
| API | Spring MVC, OpenAPI, Swagger UI, RFC 9457 Problem Details |
| Mapping | MapStruct |
| Testing | JUnit 5, Mockito, AssertJ, MockMvc, Testcontainers, JaCoCo |
| Infrastructure | Docker Compose, GitHub Actions |

## Repository Structure

```text
buildSrc/   shared Gradle convention plugins
gradle/     Gradle wrapper and version catalog
infra/      Docker Compose and Keycloak realm export
libs/       shared technical libraries
services/   business microservices
```

Business services may depend on shared technical libraries under `libs/`. A business service must not depend directly on another business service's code or database.

## Local Setup

### Prerequisites

- Java 25
- Docker Desktop

### Start the local infrastructure

```bash
cp infra/.env.example infra/.env
docker compose -f infra/compose.yaml up -d
```

Run `EmployeeServiceApplication` with the values from `infra/.env` loaded as environment variables.

For additional local diagnostics, activate the `dev` Spring profile.

### Local endpoints

| Endpoint | Purpose |
|---|---|
| [Employee Service health](http://localhost:8081/actuator/health) | Health check |
| [Swagger UI](http://localhost:8081/swagger-ui.html) | Interactive Employee API documentation |
| [OpenAPI JSON](http://localhost:8081/v3/api-docs) | Generated API specification |
| [Keycloak administration](http://localhost:8180) | Realm and client administration |

## Local Identity and API Testing

Development users are imported from `infra/keycloak/bank-system-realm.json`.

| User | Role |
|---|---|
| `clerk` | `CLERK` |
| `loan.officer` | `LOAN_OFFICER` |
| `bank.admin` | `ADMIN` |

### Keycloak clients

| Client | Purpose | Authentication flow |
|---|---|---|
| `bank-frontend` | Angular staff application | Authorization Code with PKCE |
| `bank-dev-cli` | Postman, command-line tools, and local API testing | Direct access grant, development only |

Both clients use the shared `bank-api-audience` client scope. Backend services accept access tokens containing the `bank-api` audience.

## Design Decisions
- Monorepo with one Gradle build; shared build logic in convention plugins.
- Database per service, each in its own container.
- Keycloak issues tokens; services only validate them and never store passwords.
- Keycloak realm roles mapped to Spring authorities with a custom converter.
- Employee Service holds staff profiles; Keycloak holds credentials and roles.
- Records are disabled, never deleted.

## Shared Libraries

### `common-identifiers`

Provides shared UUIDv7 generation for application-assigned entity identifiers.

### `error-handling-starter`

Provides shared runtime error handling for Spring MVC services.

The starter:

The starter handles domain, validation, framework, authentication, authorization, and unexpected errors. Responses follow RFC 9457 and include a project error code.

| Category | HTTP status |
|---|---:|
| `VALIDATION` | 400 |
| `NOT_FOUND` | 404 |
| `CONFLICT` | 409 |
| `RULE_VIOLATION` | 422 |
| `UNAUTHENTICATED` | 401 |
| `ACCESS_DENIED` | 403 |
| `INTERNAL` | 500 |

Internal starter classes are not part of the public API. Services depend only on the public error contract.

## Testing

Run all project checks:

```bash
./gradlew check
```

Run Employee Service checks only:

```bash
./gradlew :services:employee-service:check
```

The build runs unit, MVC, security, and PostgreSQL Testcontainers tests. JaCoCo enforces a 70% line-coverage gate per service.

## Continuous Integration

GitHub Actions runs checks for changed modules. Changes to a shared library also trigger checks for services that consume it.

## License

This project is licensed under the MIT License.

## Acknowledgments

- Developed for the [CSCB634 Practicum in Programming and Implementation of Databases](https://ecatalog.nbu.bg/default.asp?V_Year=2026&YSem=6&Spec_ID=&Mod_ID=&PageShow=coursepresent&P_Menu=courses_part2&Fac_ID=3&M_PHD=0&P_ID=832&TabIndex=1&K_ID=50304&K_TypeID=77&l=1) course at [New Bulgarian University](https://www.nbu.bg/en).
- Special thanks to the course instructor for their guidance, project requirements, and practical foundation provided throughout the module.

## Repository

- [Bank System repository](https://github.com/StefanYankov/bank-system)
