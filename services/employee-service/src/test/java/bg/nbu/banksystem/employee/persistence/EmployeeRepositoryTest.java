package bg.nbu.banksystem.employee.persistence;

import bg.nbu.banksystem.employee.domain.Employee;
import bg.nbu.banksystem.employee.domain.EmployeePosition;
import bg.nbu.banksystem.employee.domain.EmployeeStatus;
import jakarta.persistence.EntityManager;
import org.hibernate.proxy.HibernateProxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@Import(JpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("EmployeeRepository (integration)")
class EmployeeRepositoryTest {

    private static final String FIRST_NAME = "Serafim";
    private static final String LAST_NAME = "Gerasimoff";

    private static final LocalDate EMPLOYMENT_START_DATE =
            LocalDate.of(2026, 10, 1);

    private static final String AUTHENTICATED_SUBJECT =
            "authenticated-keycloak-subject";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(
                    DockerImageName.parse("postgres:18.6-alpine3.24")
            );

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Persistence")
    class Persistence {

        @Test
        @DisplayName("Happy Path: Should persist and reload an employee")
        void shouldPersistAndReloadEmployee() {
            // Arrange
            Employee employee = registerEmployee(
                    "EMP-000001",
                    "subject-001",
                    "serafim.gerasimoff@example.com"
            );

            UUID assignedId = employee.getId();

            // Act
            employeeRepository.saveAndFlush(employee);

            entityManager.clear();

            Employee reloaded = employeeRepository.findById(assignedId)
                    .orElseThrow();

            // Assert
            assertThat(reloaded.getId()).isEqualTo(assignedId);
            assertThat(reloaded.getEmployeeNumber()).isEqualTo("EMP-000001");
            assertThat(reloaded.getIdentitySubjectId()).isEqualTo("subject-001");
            assertThat(reloaded.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(reloaded.getLastName()).isEqualTo(LAST_NAME);
            assertThat(reloaded.getWorkEmail())
                    .isEqualTo("serafim.gerasimoff@example.com");
            assertThat(reloaded.getPosition())
                    .isEqualTo(EmployeePosition.CLERK);
            assertThat(reloaded.getStatus())
                    .isEqualTo(EmployeeStatus.ACTIVE);
            assertThat(reloaded.getEmploymentStartDate())
                    .isEqualTo(EMPLOYMENT_START_DATE);
            assertThat(reloaded.getEmploymentEndDate()).isNull();
        }

        @Test
        @DisplayName("Happy Path: Should preserve the application-assigned UUID")
        void shouldPreserveApplicationAssignedUuid() {
            // Arrange
            Employee employee = registerEmployee(
                    "EMP-000002",
                    "subject-002",
                    "serafim.002@example.com"
            );

            UUID idBeforePersistence = employee.getId();

            // Act
            employeeRepository.saveAndFlush(employee);

            // Assert
            assertThat(employee.getId()).isEqualTo(idBeforePersistence);
        }

        @Test
        @DisplayName("Happy Path: Should initialize the optimistic-locking version")
        void shouldInitializeOptimisticLockingVersion() {
            // Arrange
            Employee employee = registerEmployee(
                    "EMP-000003",
                    "subject-003",
                    "serafim.003@example.com"
            );

            // Act
            employeeRepository.saveAndFlush(employee);

            // Assert
            assertThat(employee.getVersion()).isNotNull();
            assertThat(employee.getVersion()).isZero();
        }
    }

    @Nested
    @DisplayName("Auditing")
    class Auditing {

        @Test
        @DisplayName("Happy Path: Should populate timestamps and SYSTEM actor")
        void shouldPopulateSystemAuditFields() {
            // Arrange
            Employee employee = registerEmployee(
                    "EMP-000004",
                    "subject-004",
                    "serafim.004@example.com"
            );

            // Act
            employeeRepository.saveAndFlush(employee);

            // Assert
            assertThat(employee.getCreatedAt()).isNotNull();
            assertThat(employee.getUpdatedAt()).isNotNull();

            assertThat(employee.getCreatedBy())
                    .isEqualTo(JpaAuditingConfig.SYSTEM_ACTOR);

            assertThat(employee.getUpdatedBy())
                    .isEqualTo(JpaAuditingConfig.SYSTEM_ACTOR);
        }

        @Test
        @DisplayName("Happy Path: Should use the authenticated JWT subject as audit actor")
        void shouldUseAuthenticatedSubjectAsAuditActor() {
            // Arrange
            SecurityContextHolder.getContext().setAuthentication(
                    authenticatedJwt(AUTHENTICATED_SUBJECT)
            );

            Employee employee = registerEmployee(
                    "EMP-000005",
                    "subject-005",
                    "serafim.005@example.com"
            );

            // Act
            employeeRepository.saveAndFlush(employee);

            // Assert
            assertThat(employee.getCreatedBy())
                    .isEqualTo(AUTHENTICATED_SUBJECT);

            assertThat(employee.getUpdatedBy())
                    .isEqualTo(AUTHENTICATED_SUBJECT);
        }

        @Test
        @DisplayName("Happy Path: Should update modification metadata without changing creation metadata")
        void shouldUpdateModificationMetadata() {
            // Arrange
            Employee employee = registerEmployee(
                    "EMP-000006",
                    "subject-006",
                    "serafim.006@example.com"
            );

            employeeRepository.saveAndFlush(employee);

            var initialCreatedAt = employee.getCreatedAt();
            var initialCreatedBy = employee.getCreatedBy();
            var initialUpdatedAt = employee.getUpdatedAt();
            var initialVersion = employee.getVersion();

            SecurityContextHolder.getContext().setAuthentication(
                    authenticatedJwt("updating-keycloak-subject")
            );

            // Act
            employee.updateProfile(
                    "Serafim",
                    "Gerasimoff",
                    "serafim.updated@example.com"
            );

            employeeRepository.flush();

            // Assert
            assertThat(employee.getCreatedAt()).isEqualTo(initialCreatedAt);
            assertThat(employee.getCreatedBy()).isEqualTo(initialCreatedBy);

            assertThat(employee.getUpdatedAt())
                    .isAfterOrEqualTo(initialUpdatedAt);

            assertThat(employee.getUpdatedBy())
                    .isEqualTo("updating-keycloak-subject");

            assertThat(employee.getVersion())
                    .isGreaterThan(initialVersion);
        }
    }

    @Nested
    @DisplayName("Business identifier queries")
    class BusinessIdentifierQueries {

        @Test
        @DisplayName("Happy Path: Should find an employee by employee number")
        void shouldFindByEmployeeNumber() {
            // Arrange
            Employee saved = employeeRepository.saveAndFlush(
                    registerEmployee(
                            "EMP-000007",
                            "subject-007",
                            "serafim.007@example.com"
                    )
            );

            entityManager.clear();

            // Act
            var result = employeeRepository.findByEmployeeNumber(
                    "EMP-000007"
            );

            // Assert
            assertThat(result)
                    .isPresent()
                    .contains(saved);
        }

        @Test
        @DisplayName("Happy Path: Should find an employee by identity subject")
        void shouldFindByIdentitySubject() {
            // Arrange
            Employee saved = employeeRepository.saveAndFlush(
                    registerEmployee(
                            "EMP-000008",
                            "subject-008",
                            "serafim.008@example.com"
                    )
            );

            entityManager.clear();

            // Act
            var result = employeeRepository.findByIdentitySubjectId(
                    "subject-008"
            );

            // Assert
            assertThat(result)
                    .isPresent()
                    .contains(saved);
        }

        @Test
        @DisplayName("Happy Path: Should detect an existing work email")
        void shouldDetectExistingWorkEmail() {
            // Arrange
            employeeRepository.saveAndFlush(
                    registerEmployee(
                            "EMP-000009",
                            "subject-009",
                            "serafim.009@example.com"
                    )
            );

            // Act
            boolean exists = employeeRepository.existsByWorkEmail(
                    "serafim.009@example.com"
            );

            // Assert
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Edge Case: Should return empty results for unknown identifiers")
        void shouldReturnEmptyResultsForUnknownIdentifiers() {
            // Act and Assert
            assertThat(
                    employeeRepository.findByEmployeeNumber("EMP-999999")
            ).isEmpty();

            assertThat(
                    employeeRepository.findByIdentitySubjectId(
                            "unknown-subject"
                    )
            ).isEmpty();

            assertThat(
                    employeeRepository.existsByWorkEmail(
                            "unknown@example.com"
                    )
            ).isFalse();
            assertThat(
                    employeeRepository.existsByIdentitySubjectId(
                            "unknown-subject"
                    )
            ).isFalse();
        }

        @Test
        @DisplayName("Happy Path: Should detect an existing identity subject")
        void shouldDetectExistingIdentitySubject() {
            // Arrange
            employeeRepository.saveAndFlush(
                    registerEmployee(
                            "EMP-000016",
                            "subject-016",
                            "serafim.016@example.com"
                    )
            );

            // Act
            boolean exists = employeeRepository.existsByIdentitySubjectId("subject-016");

            // Assert
            assertThat(exists).isTrue();
        }
    }

    @Nested
    @DisplayName("Database constraints")
    class DatabaseConstraints {

        @Test
        @DisplayName("Error Case: Should reject duplicate employee numbers")
        void shouldRejectDuplicateEmployeeNumber() {
            // Arrange
            employeeRepository.saveAndFlush(
                    registerEmployee(
                            "EMP-000010",
                            "subject-010-a",
                            "serafim.010.a@example.com"
                    )
            );

            Employee duplicate = registerEmployee(
                    "EMP-000010",
                    "subject-010-b",
                    "serafim.010.b@example.com"
            );

            // Act and Assert
            assertThatThrownBy(() ->
                    employeeRepository.saveAndFlush(duplicate)
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Error Case: Should reject duplicate identity subjects")
        void shouldRejectDuplicateIdentitySubject() {
            // Arrange
            employeeRepository.saveAndFlush(
                    registerEmployee(
                            "EMP-000011",
                            "subject-011",
                            "serafim.011.a@example.com"
                    )
            );

            Employee duplicate = registerEmployee(
                    "EMP-000012",
                    "subject-011",
                    "serafim.011.b@example.com"
            );

            // Act and Assert
            assertThatThrownBy(() ->
                    employeeRepository.saveAndFlush(duplicate)
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Error Case: Should reject duplicate work emails")
        void shouldRejectDuplicateWorkEmail() {
            // Arrange
            employeeRepository.saveAndFlush(
                    registerEmployee(
                            "EMP-000013",
                            "subject-013-a",
                            "duplicate@example.com"
                    )
            );

            Employee duplicate = registerEmployee(
                    "EMP-000014",
                    "subject-014",
                    "duplicate@example.com"
            );

            // Act and Assert
            assertThatThrownBy(() ->
                    employeeRepository.saveAndFlush(duplicate)
            ).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("Entity identity")
    class EntityIdentity {

        @Test
        @DisplayName("Happy Path: Should treat an entity and its Hibernate proxy as equal")
        void shouldTreatEntityAndProxyAsEqual() {
            // Arrange
            Employee employee = employeeRepository.saveAndFlush(
                    registerEmployee(
                            "EMP-000015",
                            "subject-015",
                            "serafim.015@example.com"
                    )
            );

            UUID employeeId = employee.getId();

            entityManager.clear();

            // Act
            Employee proxy = entityManager.getReference(
                    Employee.class,
                    employeeId
            );

            // Assert
            assertThat(proxy).isInstanceOf(HibernateProxy.class);

            assertThat(employee).isEqualTo(proxy);
            assertThat(proxy).isEqualTo(employee);

            assertThat(employee.hashCode())
                    .isEqualTo(proxy.hashCode());
        }
    }

    /**
     * Registers a valid Employee through the real domain factory.
     *
     * <p>This is a test-data factory only. It does not use a builder, reflection,
     * setters, or a test-only constructor, so every test creates an Employee
     * through the same invariant-protecting path as production code.
     */
    private static Employee registerEmployee(
            String employeeNumber,
            String identitySubjectId,
            String workEmail
    ) {
        return Employee.register(
                employeeNumber,
                identitySubjectId,
                FIRST_NAME,
                LAST_NAME,
                workEmail,
                EmployeePosition.CLERK,
                EMPLOYMENT_START_DATE
        );
    }

    private static JwtAuthenticationToken authenticatedJwt(String subject) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(subject)
                .build();

        return new JwtAuthenticationToken(jwt, List.of());
    }
}