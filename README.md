# Bank System

> [!IMPORTANT]
> **Project Status: In Development**
> This repository contains the **final project** for the "CSCB634 Practicum in Programming and Implementation of Databases" module at **New Bulgarian University**.

---

## Overview

Staff-facing banking system for managing customers, accounts, and loans with annuity repayment schedules. The system is built as Spring Boot microservices with an Angular frontend and Keycloak for identity and access management.

Current state:

- Employee Service is implemented through the persistence, application, and secured REST API layers.
- Customer Service is implemented through the persistence, application, and secured REST API layers.
- The Angular application includes Keycloak authentication and generated Employee and Customer Service clients.
- Employee management covers registration, details, profile updates, position changes, disablement, and reactivation.
- Customer management covers search and the complete Individual Customer workflow.
- Legal Entity Customer management covers registration, details, profile maintenance, EIK correction, closure, and reopening.
- Legal Entity Representative maintenance remains follow-up work.

## Architecture

| Component          | Owns                                                      | Status                                       |
| ------------------ | --------------------------------------------------------- | -------------------------------------------- |
| `employee-service` | Staff profiles and employment lifecycle                   | Implemented                                  |
| `customer-service` | Individual and Legal Entity Customers and Representatives | Implemented                                  |
| `account-service`  | Accounts and balances                                     | Planned                                      |
| `loan-service`     | Loans, schedules, and instalment payments                 | Planned                                      |
| Angular frontend   | Staff-facing web application                              | Employee and Customer management implemented |
| Keycloak           | Authentication, credentials, roles, and API clients       | Configured                                   |

Each service owns its PostgreSQL database. Services do not access another service's database directly.

## Technology Stack

| Area           | Technology                                                         |
| -------------- | ------------------------------------------------------------------ |
| Backend        | Java 25, Spring Boot 4.1.1                                         |
| Frontend       | Angular 22, TypeScript 6, Signals, Vitest, ESLint                  |
| Build          | Gradle 9.6 with Kotlin DSL, npm                                    |
| Database       | PostgreSQL 18.6, Flyway                                            |
| Identity       | Keycloak 26.7.4, OAuth 2.0, OpenID Connect                         |
| API            | Spring MVC, OpenAPI, Swagger UI, RFC 9457 Problem Details          |
| Mapping        | MapStruct                                                          |
| Testing        | JUnit 5, Mockito, AssertJ, MockMvc, Testcontainers, Vitest, JaCoCo |
| Infrastructure | Docker Compose, GitHub Actions                                     |

## Repository Structure

```text
buildSrc/               shared Gradle convention plugins
frontend/               Angular staff application
  openapi/              committed service contracts
  src/app/              application source
  tools/                API and form-constraint generation
gradle/                 Gradle wrapper and version catalog
infra/                  Docker Compose and Keycloak realm export
libs/                   shared technical libraries
services/               business microservices
  employee-service/     staff profiles and employment lifecycle
  customer-service/     Customers and Representatives
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

Run `CustomerServiceApplication` the same way.

For additional local diagnostics, activate the `dev` Spring profile.

### Start the Angular frontend

```bash
cd frontend
npm ci
npm start
```

Open [the local Angular application](http://localhost:4200).

The frontend loads environment-specific values from `frontend/public/config.json` before Angular starts. The local file configures the Keycloak realm, the public `bank-frontend` client, and the service base URLs.

Authentication uses Authorization Code with PKCE. Anonymous users can open the landing page, while protected routes redirect to Keycloak. Realm roles control access to Employee and Customer operations.

## Generated API Clients

The Angular application uses generated clients for Employee Service and Customer Service. Generated TypeScript is replaceable build output and must not be edited manually.

Service OpenAPI contracts, generator configuration, form-constraint generation, and pinned generator versions are committed so the clients can be reproduced locally and in CI.

### Refresh the Employee Service contract

Employee Service must be running locally:

```bash
curl --fail \
  http://localhost:8081/v3/api-docs \
  --output frontend/openapi/employee-service.json
```

Generate the Angular client from the committed contract:

```bash
cd frontend
npm run generate:employee-api
```

The generated TypeScript is written to:

```text
frontend/src/app/generated/employee-api/
```

### Refresh the Customer Service contract

Customer Service must be running locally:

```bash
curl --fail \
  http://localhost:8082/v3/api-docs \
  --output frontend/openapi/customer-service.json
```

Generate the client and form constraints:

```bash
cd frontend
npm run generate:customer-api
```

Generated output:

```text
frontend/src/app/generated/customer-api/
frontend/src/app/generated/customer-schema-constraints.generated.ts
```

The generated directories are not committed or linted.

## Frontend Capabilities

### Employee management

ADMIN users can:

- list and inspect Employees;
- register an Employee linked to an existing Keycloak subject;
- update profile information;
- change positions;
- disable and reactivate Employees.

Authenticated Employees can open their current Employee profile.

### Customer search

The shared Customer search page supports Individual and Legal Entity Customers by:

- Customer number;
- Individual Customer name or EGN;
- Legal Entity company name or EIK.

Search input is debounced and uses latest-request-wins behavior so stale responses cannot replace newer results.

### Individual Customer management

CLERK and ADMIN users can:

- register Individual Customers;
- view details;
- update name, contact information, and residential address;
- correct the EGN;
- close and reopen the Customer relationship.

LOAN_OFFICER users have read-only Customer access.

The frontend validates EGN structure, encoded birth date, and check digit before submission. Customer Service remains authoritative for validation, uniqueness, authorization, and persistence.

### Legal Entity Customer management

CLERK and ADMIN users can:

- register Legal Entity Customers;
- view details, addresses, and Representatives;
- update company name and contact information;
- update registered and correspondence addresses independently;
- correct the EIK;
- close and reopen the Customer relationship.

The frontend validates the EIK structure and check digit before submission. Legal Entity Customer pages are grouped under `customers/legal-entity/pages`, while Individual Customer pages are grouped under `customers/individual/pages`.

Representative information is currently read-only in the Angular application. Adding, correcting, linking, and ending Representatives remains follow-up work.

## Local Endpoints

| Endpoint                                                             | Purpose                                |
| -------------------------------------------------------------------- | -------------------------------------- |
| [Angular frontend](http://localhost:4200)                            | Staff-facing web application           |
| [Employee Service health](http://localhost:8081/actuator/health)     | Employee Service health check          |
| [Employee Service Swagger UI](http://localhost:8081/swagger-ui.html) | Interactive Employee API documentation |
| [Employee Service OpenAPI JSON](http://localhost:8081/v3/api-docs)   | Generated Employee API specification   |
| [Customer Service health](http://localhost:8082/actuator/health)     | Customer Service health check          |
| [Customer Service Swagger UI](http://localhost:8082/swagger-ui.html) | Interactive Customer API documentation |
| [Customer Service OpenAPI JSON](http://localhost:8082/v3/api-docs)   | Generated Customer API specification   |
| [Keycloak administration](http://localhost:8180)                     | Realm and client administration        |

## Local Identity and API Testing

Development users are imported from `infra/keycloak/bank-system-realm.json`.

| User           | Realm role     | Frontend access                         |
| -------------- | -------------- | --------------------------------------- |
| `clerk`        | `CLERK`        | Customer read and maintenance workflows |
| `loan.officer` | `LOAN_OFFICER` | Read-only Customer workflows            |
| `bank.admin`   | `ADMIN`        | Employee and Customer administration    |

### Keycloak clients

| Client          | Purpose                                            | Authentication flow                   |
| --------------- | -------------------------------------------------- | ------------------------------------- |
| `bank-frontend` | Angular staff application                          | Authorization Code with PKCE          |
| `bank-dev-cli`  | Postman, command-line tools, and local API testing | Direct access grant, development only |

Both clients use the shared `bank-api-audience` client scope. Backend services accept access tokens containing the `bank-api` audience.

## Design Decisions

- Monorepo with one Gradle build and shared build logic in convention plugins.
- One Angular application for the staff-facing frontend.
- Database per service, each in its own container.
- Keycloak issues tokens; services validate them and never store passwords.
- Keycloak realm roles map to Spring authorities through a custom converter.
- The Angular application uses Keycloak's public-client Authorization Code flow with PKCE.
- Frontend deployment values are loaded at runtime from `config.json`, allowing the same build to be used in different environments.
- Angular API clients and form constraints are generated from committed service OpenAPI contracts.
- Employee Service holds staff profiles; Keycloak holds credentials and roles.
- Records are disabled or closed, never deleted.
- Closed Customers are retained and can be reopened; other changes are rejected while a Customer is closed.
- Customer email is contact data, not a login, so it is not unique.
- EGN and EIK correction are explicit operations rather than general profile edits.
- Registered and correspondence addresses are maintained independently for Legal Entity Customers.
- Representative names are stored as a snapshot and are not updated when a linked Customer later changes name.
- Frontend maintenance actions are role-aware for usability, route guards protect SPA navigation, and backend authorization remains authoritative.

## Shared Libraries

### `common-identifiers`

Provides shared UUIDv7 generation for application-assigned entity identifiers.

### `error-handling-starter`

Provides shared runtime error handling for Spring MVC services.

The starter handles domain, validation, framework, authentication, authorization, and unexpected errors. Responses follow RFC 9457 and include a project error code.

| Category          | HTTP status |
| ----------------- | ----------- |
| `VALIDATION`      | 400         |
| `NOT_FOUND`       | 404         |
| `CONFLICT`        | 409         |
| `RULE_VIOLATION`  | 422         |
| `UNAUTHENTICATED` | 401         |
| `ACCESS_DENIED`   | 403         |
| `INTERNAL`        | 500         |

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

Run Customer Service checks only:

```bash
./gradlew :services:customer-service:check
```

Run frontend checks:

```bash
cd frontend
npm ci
npm run generate:employee-api
npm run generate:customer-api
npm run lint
npm test
npm run build
```

Backend checks include unit, MVC, security, and PostgreSQL Testcontainers tests. JaCoCo enforces a 70% line-coverage gate per service.

Frontend checks include reproducible Employee and Customer API client generation, generated form constraints, ESLint, Vitest, and the production Angular build. Tests cover runtime configuration, authentication, route guards, role-aware actions, bearer-token propagation, Employee management, Customer search, Individual Customer workflows, and Legal Entity Customer workflows.

## Continuous Integration

GitHub Actions runs checks for changed modules. Changes to a shared library also trigger checks for services that consume it.

Frontend CI installs dependencies, regenerates the Employee and Customer API clients and form constraints from the committed OpenAPI contracts, runs linting and tests, and produces a production build.

## Security

Report suspected vulnerabilities privately according to the [security policy](.github/SECURITY.md).

## License

This project is licensed under the MIT License.

## Acknowledgments

- Developed for the [CSCB634 Practicum in Programming and Implementation of Databases](https://ecatalog.nbu.bg/default.asp?V_Year=2026&YSem=6&Spec_ID=&Mod_ID=&PageShow=coursepresent&P_Menu=courses_part2&Fac_ID=3&M_PHD=0&P_ID=832&TabIndex=1&K_ID=50304&K_TypeID=77&l=1) course at [New Bulgarian University](https://www.nbu.bg/en).
- Special thanks to the course instructor for their guidance, project requirements, and practical foundation provided throughout the module.

## Repository

- [Bank System repository](https://github.com/StefanYankov/bank-system)
