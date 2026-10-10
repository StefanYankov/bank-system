package bg.nbu.banksystem.employee.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@DisplayName("Employee (unit)")
class EmployeeTest {

    private static final String EMPLOYEE_NUMBER = "EMP-000001";
    private static final String IDENTITY_SUBJECT_ID = "keycloak-subject-id";
    private static final String FIRST_NAME = "Serafim";
    private static final String LAST_NAME = "Gerasimoff";
    private static final String WORK_EMAIL = "serafim.gerasimoff@example.com";

    private static final LocalDate EMPLOYMENT_START_DATE =
            LocalDate.of(2026, 10, 1);

    @Nested
    @DisplayName("Registration")
    class Registration {

        @Test
        @DisplayName("Happy Path: Should register an active employee with an assigned UUID")
        void shouldRegisterActiveEmployee() {
            // Act
            Employee employee = registerEmployee();

            // Assert
            assertThat(employee.getId()).isNotNull();
            assertThat(employee.getEmployeeNumber()).isEqualTo(EMPLOYEE_NUMBER);
            assertThat(employee.getIdentitySubjectId()).isEqualTo(IDENTITY_SUBJECT_ID);
            assertThat(employee.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(employee.getLastName()).isEqualTo(LAST_NAME);
            assertThat(employee.getWorkEmail()).isEqualTo(WORK_EMAIL);
            assertThat(employee.getPosition()).isEqualTo(EmployeePosition.CLERK);
            assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
            assertThat(employee.getEmploymentStartDate()).isEqualTo(EMPLOYMENT_START_DATE);
            assertThat(employee.getEmploymentEndDate()).isNull();
        }

        @Test
        @DisplayName("Happy Path: Should assign a different UUID to every employee")
        void shouldAssignDifferentUuidToEveryEmployee() {
            // Act
            Employee firstEmployee = registerEmployee();
            Employee secondEmployee = registerEmployee();

            // Assert
            assertThat(firstEmployee.getId())
                    .isNotEqualTo(secondEmployee.getId());
        }

        @Test
        @DisplayName("Edge Case: Should trim text and normalize the work email")
        void shouldNormalizeRegistrationData() {
            // Act
            Employee employee = Employee.register(
                    "  EMP-000001  ",
                    "  keycloak-subject-id  ",
                    "  Serafim  ",
                    "  Gerasimoff  ",
                    "  Serafim.Gerasimoff@Example.COM  ",
                    EmployeePosition.CLERK,
                    EMPLOYMENT_START_DATE
            );

            // Assert
            assertThat(employee.getEmployeeNumber()).isEqualTo(EMPLOYEE_NUMBER);
            assertThat(employee.getIdentitySubjectId()).isEqualTo(IDENTITY_SUBJECT_ID);
            assertThat(employee.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(employee.getLastName()).isEqualTo(LAST_NAME);
            assertThat(employee.getWorkEmail()).isEqualTo(WORK_EMAIL);
        }

        @ParameterizedTest(name = "Error Case: Should reject invalid {0}")
        @MethodSource("bg.nbu.banksystem.employee.domain.EmployeeTest#invalidTextRegistrationData")
        @DisplayName("Error Case: Should reject missing mandatory text fields")
        void shouldRejectInvalidMandatoryText(
                String field,
                String employeeNumber,
                String identitySubjectId,
                String firstName,
                String lastName,
                String workEmail
        ) {
            // Act & Assert
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Employee.register(
                            employeeNumber,
                            identitySubjectId,
                            firstName,
                            lastName,
                            workEmail,
                            EmployeePosition.CLERK,
                            EMPLOYMENT_START_DATE
                    ))
                    .withMessageContaining(field);
        }

        @Test
        @DisplayName("Error Case: Should reject a missing position")
        void shouldRejectMissingPosition() {
            // Act & Assert
            assertThatNullPointerException()
                    .isThrownBy(() -> Employee.register(
                            EMPLOYEE_NUMBER,
                            IDENTITY_SUBJECT_ID,
                            FIRST_NAME,
                            LAST_NAME,
                            WORK_EMAIL,
                            null,
                            EMPLOYMENT_START_DATE
                    ))
                    .withMessage("position must not be null");
        }

        @Test
        @DisplayName("Error Case: Should reject a missing employment start date")
        void shouldRejectMissingEmploymentStartDate() {
            // Act & Assert
            assertThatNullPointerException()
                    .isThrownBy(() -> Employee.register(
                            EMPLOYEE_NUMBER,
                            IDENTITY_SUBJECT_ID,
                            FIRST_NAME,
                            LAST_NAME,
                            WORK_EMAIL,
                            EmployeePosition.CLERK,
                            null
                    ))
                    .withMessage("employmentStartDate must not be null");
        }
    }

    @Nested
    @DisplayName("Profile")
    class Profile {

        @Test
        @DisplayName("Happy Path: Should update and normalize editable profile information")
        void shouldUpdateProfile() {
            // Arrange
            Employee employee = registerEmployee();

            // Act
            employee.updateProfile(
                    "  Updated  ",
                    "  Employee  ",
                    "  Updated.Employee@Example.COM  "
            );

            // Assert
            assertThat(employee.getFirstName()).isEqualTo("Updated");
            assertThat(employee.getLastName()).isEqualTo("Employee");
            assertThat(employee.getWorkEmail())
                    .isEqualTo("updated.employee@example.com");
        }

        @Test
        @DisplayName("Happy Path: Should change the employee position")
        void shouldChangePosition() {
            // Arrange
            Employee employee = registerEmployee();

            // Act
            employee.changePosition(EmployeePosition.LOAN_OFFICER);

            // Assert
            assertThat(employee.getPosition())
                    .isEqualTo(EmployeePosition.LOAN_OFFICER);
        }

        @Test
        @DisplayName("Error Case: Should reject a missing new position")
        void shouldRejectMissingNewPosition() {
            // Arrange
            Employee employee = registerEmployee();

            // Act & Assert
            assertThatNullPointerException()
                    .isThrownBy(() -> employee.changePosition(null))
                    .withMessage("newPosition must not be null");
        }
    }

    @Nested
    @DisplayName("Employment lifecycle")
    class EmploymentLifecycle {

        @Test
        @DisplayName("Happy Path: Should disable an active employee")
        void shouldDisableActiveEmployee() {
            // Arrange
            Employee employee = registerEmployee();
            LocalDate employmentEndDate = LocalDate.of(2026, 10, 31);

            // Act
            employee.disable(employmentEndDate);

            // Assert
            assertThat(employee.getStatus())
                    .isEqualTo(EmployeeStatus.INACTIVE);
            assertThat(employee.getEmploymentEndDate())
                    .isEqualTo(employmentEndDate);
        }

        @Test
        @DisplayName("Happy Path: Should reactivate an inactive employee")
        void shouldReactivateInactiveEmployee() {
            // Arrange
            Employee employee = registerEmployee();
            employee.disable(LocalDate.of(2026, 10, 31));

            // Act
            employee.reactivate();

            // Assert
            assertThat(employee.getStatus())
                    .isEqualTo(EmployeeStatus.ACTIVE);
            assertThat(employee.getEmploymentEndDate()).isNull();
        }

        @Test
        @DisplayName("Edge Case: Should allow the employment end date to equal the start date")
        void shouldAllowEmploymentEndDateEqualToStartDate() {
            // Arrange
            Employee employee = registerEmployee();

            // Act
            employee.disable(EMPLOYMENT_START_DATE);

            // Assert
            assertThat(employee.getStatus())
                    .isEqualTo(EmployeeStatus.INACTIVE);
            assertThat(employee.getEmploymentEndDate())
                    .isEqualTo(EMPLOYMENT_START_DATE);
        }

        @Test
        @DisplayName("Error Case: Should reject a missing employment end date")
        void shouldRejectMissingEmploymentEndDate() {
            // Arrange
            Employee employee = registerEmployee();

            // Act & Assert
            assertThatNullPointerException()
                    .isThrownBy(() -> employee.disable(null))
                    .withMessage("endDate must not be null");
        }

        @Test
        @DisplayName("Error Case: Should reject an end date before the employment start date")
        void shouldRejectEndDateBeforeStartDate() {
            // Arrange
            Employee employee = registerEmployee();
            LocalDate invalidEndDate =
                    EMPLOYMENT_START_DATE.minusDays(1);

            // Act & Assert
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> employee.disable(invalidEndDate))
                    .withMessage(
                            "Employment end date must not precede the start date"
                    );
        }

        @Test
        @DisplayName("Error Case: Should reject disabling an already inactive employee")
        void shouldRejectDisablingInactiveEmployee() {
            // Arrange
            Employee employee = registerEmployee();
            employee.disable(LocalDate.of(2026, 10, 31));

            // Act & Assert
            assertThatIllegalStateException()
                    .isThrownBy(() ->
                            employee.disable(LocalDate.of(2026, 11, 1))
                    )
                    .withMessage("Employee is already inactive");
        }

        @Test
        @DisplayName("Error Case: Should reject reactivating an already active employee")
        void shouldRejectReactivatingActiveEmployee() {
            // Arrange
            Employee employee = registerEmployee();

            // Act & Assert
            assertThatIllegalStateException()
                    .isThrownBy(employee::reactivate)
                    .withMessage("Employee is already active");
        }
    }

    private static Employee registerEmployee() {
        return Employee.register(
                EMPLOYEE_NUMBER,
                IDENTITY_SUBJECT_ID,
                FIRST_NAME,
                LAST_NAME,
                WORK_EMAIL,
                EmployeePosition.CLERK,
                EMPLOYMENT_START_DATE
        );
    }

    static Stream<Arguments> invalidTextRegistrationData() {
        return Stream.of(
                Arguments.of(
                        "employeeNumber",
                        null,
                        IDENTITY_SUBJECT_ID,
                        FIRST_NAME,
                        LAST_NAME,
                        WORK_EMAIL
                ),
                Arguments.of(
                        "identitySubjectId",
                        EMPLOYEE_NUMBER,
                        "   ",
                        FIRST_NAME,
                        LAST_NAME,
                        WORK_EMAIL
                ),
                Arguments.of(
                        "firstName",
                        EMPLOYEE_NUMBER,
                        IDENTITY_SUBJECT_ID,
                        "",
                        LAST_NAME,
                        WORK_EMAIL
                ),
                Arguments.of(
                        "lastName",
                        EMPLOYEE_NUMBER,
                        IDENTITY_SUBJECT_ID,
                        FIRST_NAME,
                        "   ",
                        WORK_EMAIL
                ),
                Arguments.of(
                        "workEmail",
                        EMPLOYEE_NUMBER,
                        IDENTITY_SUBJECT_ID,
                        FIRST_NAME,
                        LAST_NAME,
                        null
                )
        );
    }
}