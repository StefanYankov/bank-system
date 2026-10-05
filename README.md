# Bank System

> [!IMPORTANT]
> **Project Status: In Development**
> This repository contains the **final project** for the "CSCB634 Practicum in Programming and Implementation of Databases" module at **New Bulgarian University**.

---

## Overview
Staff-facing banking system for managing customers, accounts and loans with annuity repayment schedules. Built as Spring Boot microservices with an Angular frontend and Keycloak for identity.

Current state: `employee-service` foundation (database, security, tests).

## Architecture
| Component | Owns | Status |
|---|---|---|
| employee-service | Staff profiles and employment status | In progress |
| customer-service | Individual and legal-entity customers | Planned |
| account-service | Accounts and balances | Planned |
| loan-service | Loans, schedules, instalment payments | Planned |
| Keycloak | Authentication, credentials, roles | Configured |

Each service has its own PostgreSQL database. No cross-service joins.

## Technology Stack
| Area | Technology |
|---|---|
| Backend | Java 25, Spring Boot 4.1.1, Gradle 9.6 (Kotlin DSL) |
| Database | PostgreSQL 18.6, Flyway |
| Identity | Keycloak 26.7.4 (OAuth2 / OIDC) |
| Testing | JUnit 5, Mockito, AssertJ, Testcontainers, JaCoCo (70% gate) |
| Infrastructure | Docker Compose, GitHub Actions |

##
```
buildSrc/        shared Gradle convention plugins
gradle/          wrapper and version catalog
infra/           Docker Compose, Keycloak realm export
services/        business microservices
```

## Local Setup
Prerequisites: Java 25, Docker Desktop.

```bash
cp infra/.env.example infra/.env
docker compose -f infra/compose.yaml up -d
```

Run `EmployeeServiceApplication` with `infra/.env` loaded as environment
variables, then check `http://localhost:8081/actuator/health`.

## Local Credentials
Keycloak console: http://localhost:8180, admin credentials from `infra/.env`.

| User | Role |
|---|---|
| clerk | CLERK |
| loan.officer | LOAN_OFFICER |
| bank.admin | ADMIN |

Dev-only accounts imported from `infra/keycloak/bank-system-realm.json`.

## Design Decisions
- Monorepo with one Gradle build; shared build logic in convention plugins.
- Database per service, each in its own container.
- Keycloak issues tokens; services only validate them and never store passwords.
- Keycloak realm roles mapped to Spring authorities with a custom converter.
- Employee Service holds staff profiles; Keycloak holds credentials and roles.
- Records are disabled, never deleted.

## Testing
```bash
./gradlew check
```
Runs tests and enforces 70% line coverage per service.

## License

This project is licensed under the MIT License.

## Acknowledgments

- Developed for the [CSCB634 Practicum in Programming and Implementation of Databases](https://ecatalog.nbu.bg/default.asp?V_Year=2026&YSem=6&Spec_ID=&Mod_ID=&PageShow=coursepresent&P_Menu=courses_part2&Fac_ID=3&M_PHD=0&P_ID=832&TabIndex=1&K_ID=50304&K_TypeID=77&l=1) course at [New Bulgarian University](https://www.nbu.bg/en).
- Special thanks to the course instructor for their guidance, project requirements, and practical foundation provided throughout the module.

## Repository

- [Bank System repository](https://github.com/StefanYankov/bank-system)