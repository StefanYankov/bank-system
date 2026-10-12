package bg.nbu.banksystem.employee.application;

import bg.nbu.banksystem.employee.application.dto.*;
import bg.nbu.banksystem.employee.domain.Employee;
import bg.nbu.banksystem.employee.domain.EmployeeErrorCode;
import bg.nbu.banksystem.employee.domain.EmployeePosition;
import bg.nbu.banksystem.employee.domain.EmployeeStatus;
import bg.nbu.banksystem.employee.persistence.EmployeeRepository;
import bg.nbu.banksystem.starter.error.DomainException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeServiceImpl (unit)")
class EmployeeServiceImplTest {

    private static final String EMPLOYEE_NUMBER = "EMP-000001";
    private static final String IDENTITY_SUBJECT_ID = "subject-001";
    private static final String WORK_EMAIL = "serafim.gerasimoff@example.com";
    private static final LocalDate START_DATE = LocalDate.of(2026, 10, 1);

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeNumberGenerator employeeNumberGenerator;

    private EmployeeServiceImpl employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeServiceImpl(
                employeeRepository,
                employeeNumberGenerator,
                Mappers.getMapper(EmployeeMapper.class)
        );
    }

    @Nested
    @DisplayName("Register")
    class Register {

        @Test
        @DisplayName("Happy Path: Should register an active employee with a generated number")
        void shouldRegisterEmployee() {
            // Arrange
            when(employeeNumberGenerator.generate()).thenReturn(EMPLOYEE_NUMBER);
            when(employeeRepository.save(any(Employee.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            EmployeeViewDto view = employeeService.register(registrationDto(WORK_EMAIL));

            // Assert
            assertThat(view.employeeNumber()).isEqualTo(EMPLOYEE_NUMBER);
            assertThat(view.workEmail()).isEqualTo(WORK_EMAIL);
            assertThat(view.status()).isEqualTo(EmployeeStatus.ACTIVE);
            verify(employeeRepository).save(any(Employee.class));
        }

        @Test
        @DisplayName("Edge Case: Should check uniqueness against the normalized email")
        void shouldCheckNormalizedEmail() {
            // Arrange
            when(employeeNumberGenerator.generate()).thenReturn(EMPLOYEE_NUMBER);
            when(employeeRepository.save(any(Employee.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            employeeService.register(registrationDto("  Serafim.Gerasimoff@Example.COM  "));

            // Assert
            verify(employeeRepository).existsByWorkEmail(WORK_EMAIL);
        }

        @Test
        @DisplayName("Error Case: Should reject an identity that is already linked")
        void shouldRejectLinkedIdentity() {
            // Arrange
            when(employeeRepository.existsByIdentitySubjectId(IDENTITY_SUBJECT_ID)).thenReturn(true);

            // Act & Assert
            assertDomainError(
                    () -> employeeService.register(registrationDto(WORK_EMAIL)),
                    EmployeeErrorCode.EMPLOYEE_IDENTITY_ALREADY_LINKED
            );

            verifyNoInteractions(employeeNumberGenerator);
            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Error Case: Should reject a work email that is already used")
        void shouldRejectUsedEmail() {
            // Arrange
            when(employeeRepository.existsByWorkEmail(WORK_EMAIL)).thenReturn(true);

            // Act & Assert
            assertDomainError(
                    () -> employeeService.register(registrationDto(WORK_EMAIL)),
                    EmployeeErrorCode.EMPLOYEE_EMAIL_ALREADY_USED
            );

            verifyNoInteractions(employeeNumberGenerator);
            verify(employeeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Queries")
    class Queries {

        @Test
        @DisplayName("Happy Path: Should return an employee by id")
        void shouldGetById() {
            // Arrange
            Employee employee = existingEmployee();
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

            // Act
            EmployeeViewDto view = employeeService.getById(employee.getId());

            // Assert
            assertThat(view.id()).isEqualTo(employee.getId());
        }

        @Test
        @DisplayName("Error Case: Should report a missing employee by id")
        void shouldReportMissingById() {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            when(employeeRepository.findById(unknownId)).thenReturn(Optional.empty());

            // Act & Assert
            assertDomainError(
                    () -> employeeService.getById(unknownId),
                    EmployeeErrorCode.EMPLOYEE_NOT_FOUND
            );
        }

        @Test
        @DisplayName("Error Case: Should report a missing employee by number")
        void shouldReportMissingByNumber() {
            // Arrange
            when(employeeRepository.findByEmployeeNumber("EMP-999999")).thenReturn(Optional.empty());

            // Act & Assert
            assertDomainError(
                    () -> employeeService.getByEmployeeNumber("EMP-999999"),
                    EmployeeErrorCode.EMPLOYEE_NOT_FOUND
            );
        }

        @Test
        @DisplayName("Error Case: Should report a missing current employee")
        void shouldReportMissingCurrent() {
            // Arrange
            when(employeeRepository.findByIdentitySubjectId("unknown-subject")).thenReturn(Optional.empty());

            // Act & Assert
            assertDomainError(
                    () -> employeeService.getCurrent("unknown-subject"),
                    EmployeeErrorCode.EMPLOYEE_NOT_FOUND
            );
        }

        @Test
        @DisplayName("Happy Path: Should map a page of employees")
        void shouldMapPage() {
            // Arrange
            PageRequest pageable = PageRequest.of(0, 20);
            Employee employee = existingEmployee();
            when(employeeRepository.findAll(pageable))
                    .thenReturn(new PageImpl<>(List.of(employee), pageable, 1));

            // Act
            Page<EmployeeViewDto> page = employeeService.getAll(pageable);

            // Assert
            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent())
                    .extracting(EmployeeViewDto::employeeNumber)
                    .containsExactly(EMPLOYEE_NUMBER);
        }
    }

    @Nested
    @DisplayName("Update profile")
    class UpdateProfile {

        @Test
        @DisplayName("Happy Path: Should update the profile when the new email is free")
        void shouldUpdateProfile() {
            // Arrange
            Employee employee = existingEmployee();
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

            // Act
            EmployeeViewDto view = employeeService.updateProfile(
                    employee.getId(),
                    new EmployeeUpdateDto("Serafim", "Gerasimoff", "  Serafim.New@Example.COM  ")
            );

            // Assert
            assertThat(view.lastName()).isEqualTo("Gerasimoff");
            assertThat(view.workEmail()).isEqualTo("serafim.new@example.com");
            verify(employeeRepository).existsByWorkEmail("serafim.new@example.com");

            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Edge Case: Should not check uniqueness when the email is unchanged")
        void shouldSkipCheckForUnchangedEmail() {
            // Arrange
            Employee employee = existingEmployee();
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

            // Act
            employeeService.updateProfile(
                    employee.getId(),
                    new EmployeeUpdateDto("Serafim", "Gerasimoff", WORK_EMAIL)
            );

            // Assert
            verify(employeeRepository, never()).existsByWorkEmail(any());
        }

        @Test
        @DisplayName("Error Case: Should reject a new email that is already used")
        void shouldRejectUsedEmail() {
            // Arrange
            Employee employee = existingEmployee();
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            when(employeeRepository.existsByWorkEmail("taken@example.com")).thenReturn(true);

            // Act & Assert
            assertDomainError(
                    () -> employeeService.updateProfile(
                            employee.getId(),
                            new EmployeeUpdateDto("Serafim", "Gerasimoff", "taken@example.com")
                    ),
                    EmployeeErrorCode.EMPLOYEE_EMAIL_ALREADY_USED
            );

            assertThat(employee.getLastName()).isEqualTo("Gerasimoff");
            assertThat(employee.getWorkEmail()).isEqualTo(WORK_EMAIL);
        }

        @Test
        @DisplayName("Error Case: Should report a missing employee")
        void shouldReportMissingEmployee() {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            when(employeeRepository.findById(unknownId)).thenReturn(Optional.empty());

            // Act & Assert
            assertDomainError(
                    () -> employeeService.updateProfile(
                            unknownId,
                            new EmployeeUpdateDto("Serafim", "Gerasimoff", WORK_EMAIL)
                    ),
                    EmployeeErrorCode.EMPLOYEE_NOT_FOUND
            );
        }
    }

    @Nested
    @DisplayName("Lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("Happy Path: Should change the employee position")
        void shouldChangePosition() {
            // Arrange
            Employee employee = existingEmployee();
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

            // Act
            EmployeeViewDto view = employeeService.changePosition(
                    employee.getId(),
                    new EmployeePositionChangeDto(EmployeePosition.LOAN_OFFICER)
            );

            // Assert
            assertThat(view.position()).isEqualTo(EmployeePosition.LOAN_OFFICER);
        }

        @Test
        @DisplayName("Happy Path: Should disable an active employee")
        void shouldDisableEmployee() {
            // Arrange
            Employee employee = existingEmployee();
            LocalDate endDate = LocalDate.of(2026, 12, 31);
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

            // Act
            EmployeeViewDto view = employeeService.disable(employee.getId(), new EmployeeDisableDto(endDate));

            // Assert
            assertThat(view.status()).isEqualTo(EmployeeStatus.INACTIVE);
            assertThat(view.employmentEndDate()).isEqualTo(endDate);
        }

        @Test
        @DisplayName("Happy Path: Should reactivate an inactive employee")
        void shouldReactivateEmployee() {
            // Arrange
            Employee employee = existingEmployee();
            employee.disable(LocalDate.of(2026, 12, 31));
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

            // Act
            EmployeeViewDto view = employeeService.reactivate(employee.getId());

            // Assert
            assertThat(view.status()).isEqualTo(EmployeeStatus.ACTIVE);
            assertThat(view.employmentEndDate()).isNull();
        }

        @Test
        @DisplayName("Error Case: Should pass entity lifecycle errors through unchanged")
        void shouldPassThroughEntityError() {
            // Arrange
            Employee employee = existingEmployee();
            employee.disable(LocalDate.of(2026, 12, 31));
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

            // Act & Assert
            assertDomainError(
                    () -> employeeService.disable(
                            employee.getId(),
                            new EmployeeDisableDto(LocalDate.of(2027, 1, 31))
                    ),
                    EmployeeErrorCode.EMPLOYEE_ALREADY_INACTIVE
            );
        }

        @Test
        @DisplayName("Error Case: Should report a missing employee")
        void shouldReportMissingEmployee() {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            when(employeeRepository.findById(unknownId)).thenReturn(Optional.empty());

            // Act & Assert
            assertDomainError(
                    () -> employeeService.reactivate(unknownId),
                    EmployeeErrorCode.EMPLOYEE_NOT_FOUND
            );
        }
    }

    private static EmployeeRegistrationDto registrationDto(String workEmail) {
        return new EmployeeRegistrationDto(
                IDENTITY_SUBJECT_ID,
                "Serafim",
                "Gerasimoff",
                workEmail,
                EmployeePosition.CLERK,
                START_DATE
        );
    }

    private static Employee existingEmployee() {
        return Employee.register(
                EMPLOYEE_NUMBER,
                IDENTITY_SUBJECT_ID,
                "Serafim",
                "Gerasimoff",
                WORK_EMAIL,
                EmployeePosition.CLERK,
                START_DATE
        );
    }

    private static void assertDomainError(ThrowingCallable action, EmployeeErrorCode expectedCode) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expectedCode)
                );
    }
}