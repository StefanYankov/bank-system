# Bank System

> [!IMPORTANT]
> **Project Status: In Development**
> This repository contains the **final project** for the "CSCB634 Practicum in Programming and Implementation of Databases" module at **New Bulgarian University**.

---

## Overview

Staff-facing banking system for managing customers, accounts, and loans with annuity repayment schedules. Built as Spring Boot microservices with an Angular frontend and Keycloak for identity.

Current state: `employee-service` is implemented through the persistence, application, and REST API layers. The Angular application foundation and Keycloak authentication are also implemented.

## Architecture

| Component | Owns | Status |
|---|---|---|
| `employee-service` | Staff profiles and employment lifecycle | Implemented |
| `customer-service` | Individual and legal-entity customers | Planned |
| `account-service` | Accounts and balances | Planned |
| `loan-service` | Loans, schedules, and instalment payments | Planned |
| Angular frontend | Staff-facing web application | Authentication implemented |
| Keycloak | Authentication, credentials, roles, and API clients | Configured |

Each service owns its PostgreSQL database. Services do not access another service's database directly.

## Technology Stack

| Area | Technology |
|---|---|
| Backend | Java 25, Spring Boot 4.1.1 |
| Frontend | Angular 22, TypeScript 6, Signals, Vitest, ESLint |
| Build | Gradle 9.6 with Kotlin DSL, npm |
| Database | PostgreSQL 18.6, Flyway |
| Identity | Keycloak 26.7.4, OAuth 2.0, OpenID Connect |
| API | Spring MVC, OpenAPI, Swagger UI, RFC 9457 Problem Details |
| Mapping | MapStruct |
| Testing | JUnit 5, Mockito, AssertJ, MockMvc, Testcontainers, Vitest, JaCoCo |
| Infrastructure | Docker Compose, GitHub Actions |

## Repository Structure

```text
buildSrc/   shared Gradle convention plugins
frontend/   Angular staff application
gradle/     Gradle wrapper and version catalog
infra/      Docker Compose and Keycloak realm export
libs/       shared technical libraries
services/   business microservices
```

Business services may depend on shared technical libraries under `libs/`. A business service must not depend directly on another business service's code or database.

## Local Setup

### Prerequisites

- Java 25
- Node.js 24 and npm
- Docker Desktop

### Start the local infrastructure

```bash
cp infra/.env.example infra/.env
docker compose -f infra/compose.yaml up -d
```

Run `EmployeeServiceApplication` with the values from `infra/.env` loaded as environment variables.

For additional local diagnostics, activate the `dev` Spring profile.

### Start the Angular frontend

```bash
cd frontend
npm ci
npm start
```

Open [the local Angular application](http://localhost:4200).

The frontend loads environment-specific values from `frontend/public/config.json` before Angular starts. The local file configures the Keycloak realm, the public `bank-frontend` client, and the Employee Service base URL.

Authentication uses Authorization Code with PKCE. Anonymous users can open the landing page, while protected routes redirect to Keycloak. Realm roles control role-restricted routes such as the temporary ADMIN example.

### Local endpoints

| Endpoint | Purpose |
|---|---|
| [Angular frontend](http://localhost:4200) | Staff-facing web application |
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

- Monorepo with one Gradle build and shared build logic in convention plugins.
- One Angular application for the staff-facing frontend.
- Database per service, each in its own container.
- Keycloak issues tokens; services validate them and never store passwords.
- Keycloak realm roles map to Spring authorities through a custom converter.
- The Angular application uses Keycloak's public-client Authorization Code flow with PKCE.
- Frontend deployment values are loaded at runtime from `config.json`, allowing the same build to be used in different environments.
- Employee Service holds staff profiles; Keycloak holds credentials and roles.
- Records are disabled, never deleted.

## Shared Libraries

### `common-identifiers`

Provides shared UUIDv7 generation for application-assigned entity identifiers.

### `error-handling-starter`

Provides shared runtime error handling for Spring MVC services.

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

Run all backend checks:

```bash
./gradlew check
```

Run Employee Service checks only:

```bash
./gradlew :services:employee-service:check
```

Run frontend checks:

```bash
cd frontend
npm ci
npm run lint
npm test
npm run build
```

Backend checks include unit, MVC, security, and PostgreSQL Testcontainers tests. JaCoCo enforces a 70% line-coverage gate per service.

Frontend checks include ESLint, Vitest, and the production Angular build. Authentication tests cover runtime configuration loading, authentication state, route guards, role checks, and API URL matching for bearer-token propagation.

## Continuous Integration

GitHub Actions runs checks for changed modules. Changes to a shared library also trigger checks for services that consume it. Frontend changes run dependency installation, linting, tests, and a production build.

## License

This project is licensed under the MIT License.

## Acknowledgments

- Developed for the [CSCB634 Practicum in Programming and Implementation of Databases](https://ecatalog.nbu.bg/default.asp?V_Year=2026&YSem=6&Spec_ID=&Mod_ID=&PageShow=coursepresent&P_Menu=courses_part2&Fac_ID=3&M_PHD=0&P_ID=832&TabIndex=1&K_ID=50304&K_TypeID=77&l=1) course at [New Bulgarian University](https://www.nbu.bg/en).
- Special thanks to the course instructor for their guidance, project requirements, and practical foundation provided throughout the module.

## Repository

- [Bank System repository](https://github.com/StefanYankov/bank-system)
