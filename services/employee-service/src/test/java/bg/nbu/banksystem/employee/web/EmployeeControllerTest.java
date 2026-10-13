package bg.nbu.banksystem.employee.web;

import bg.nbu.banksystem.employee.application.dto.*;
import bg.nbu.banksystem.employee.config.SecurityConfig;
import bg.nbu.banksystem.employee.domain.EmployeePosition;
import bg.nbu.banksystem.employee.domain.EmployeeStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@WebMvcTest(EmployeeController.class)
@Import(SecurityConfig.class)
@DisplayName("EmployeeController (MVC)")
class EmployeeControllerTest extends EmployeeControllerTestSupport {

    @Nested
    @DisplayName("POST /api/employees")
    class Registration {

        @Test
        @DisplayName("Happy Path: Should return 201, Location and the registered employee")
        void shouldRegisterEmployee() throws Exception {
            // Arrange
            EmployeeRegistrationDto request = registrationDto();
            EmployeeViewDto response = employeeView();

            given(employeeService.register(request))
                    .willReturn(response);

            // Act & Assert
            mockMvc.perform(
                            post(BASE_URL)
                                    .with(adminJwt())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isCreated())
                    .andExpect(
                            header().string(
                                    "Location",
                                    "http://localhost" + BASE_URL + "/" + EMPLOYEE_ID
                            )
                    )
                    .andExpect(jsonPath("$.id").value(EMPLOYEE_ID.toString()))
                    .andExpect(jsonPath("$.employeeNumber").value(EMPLOYEE_NUMBER))
                    .andExpect(jsonPath("$.firstName").value(FIRST_NAME))
                    .andExpect(jsonPath("$.lastName").value(LAST_NAME))
                    .andExpect(jsonPath("$.workEmail").value(WORK_EMAIL))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            verify(employeeService).register(request);
        }

        @Test
        @DisplayName("Error Case: Should return 400 when registration data is invalid")
        void shouldRejectInvalidRegistrationData() throws Exception {
            // Arrange
            EmployeeRegistrationDto invalidRequest =
                    new EmployeeRegistrationDto(
                            IDENTITY_SUBJECT_ID,
                            "",
                            LAST_NAME,
                            WORK_EMAIL,
                            null,
                            EMPLOYMENT_START_DATE
                    );

            // Act & Assert
            mockMvc.perform(
                            post(BASE_URL)
                                    .with(adminJwt())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            jsonMapper.writeValueAsString(
                                                    invalidRequest
                                            )
                                    )
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(
                            jsonPath("$.code")
                                    .value("VALIDATION_FAILED")
                    )
                    .andExpect(jsonPath("$.errors").isArray());

            verify(employeeService, never())
                    .register(any());
        }
    }

    @Nested
    @DisplayName("GET /api/employees")
    class GetEmployees {

        @Test
        @DisplayName("Happy Path: Should return a stable paged response")
        void shouldReturnEmployeePage() throws Exception {
            // Arrange
            PageRequest pageable = PageRequest.of(0, 20);

            given(employeeService.getAll(pageable))
                    .willReturn(
                            new PageImpl<>(
                                    List.of(employeeView()),
                                    pageable,
                                    1
                            )
                    );

            // Act & Assert
            mockMvc.perform(
                            get(BASE_URL)
                                    .with(adminJwt())
                                    .param("page", "0")
                                    .param("size", "20")
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(
                            jsonPath("$.content[0].employeeNumber")
                                    .value(EMPLOYEE_NUMBER)
                    )
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.last").value(true));

            verify(employeeService).getAll(pageable);
        }
    }

    @Nested
    @DisplayName("GET /api/employees/{id}")
    class GetEmployeeById {

        @Test
        @DisplayName("Happy Path: Should return an employee by technical ID")
        void shouldReturnEmployeeById() throws Exception {
            // Arrange
            given(employeeService.getById(EMPLOYEE_ID))
                    .willReturn(employeeView());

            // Act & Assert
            mockMvc.perform(
                            get(BASE_URL + "/{id}", EMPLOYEE_ID)
                                    .with(jwtWithRole("CLERK"))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(EMPLOYEE_ID.toString()))
                    .andExpect(
                            jsonPath("$.employeeNumber")
                                    .value(EMPLOYEE_NUMBER)
                    )
                    .andExpect(jsonPath("$.workEmail").value(WORK_EMAIL));

            verify(employeeService).getById(EMPLOYEE_ID);
        }
    }

    @Nested
    @DisplayName("GET /api/employees/by-number/{employeeNumber}")
    class GetEmployeeByNumber {

        @Test
        @DisplayName("Happy Path: Should return an employee by business number")
        void shouldReturnEmployeeByNumber() throws Exception {
            // Arrange
            given(employeeService.getByEmployeeNumber(EMPLOYEE_NUMBER))
                    .willReturn(employeeView());

            // Act & Assert
            mockMvc.perform(
                            get(
                                    BASE_URL + "/by-number/{employeeNumber}",
                                    EMPLOYEE_NUMBER
                            )
                                    .with(jwtWithRole("LOAN_OFFICER"))
                    )
                    .andExpect(status().isOk())
                    .andExpect(
                            jsonPath("$.employeeNumber")
                                    .value(EMPLOYEE_NUMBER)
                    )
                    .andExpect(jsonPath("$.firstName").value(FIRST_NAME))
                    .andExpect(jsonPath("$.lastName").value(LAST_NAME));

            verify(employeeService)
                    .getByEmployeeNumber(EMPLOYEE_NUMBER);
        }
    }

    @Nested
    @DisplayName("GET /api/employees/me")
    class GetCurrentEmployee {

        @Test
        @DisplayName("Happy Path: Should resolve the employee from the JWT subject")
        void shouldReturnCurrentEmployee() throws Exception {
            // Arrange
            given(employeeService.getCurrent(IDENTITY_SUBJECT_ID))
                    .willReturn(employeeView());

            // Act & Assert
            mockMvc.perform(
                            get(BASE_URL + "/me")
                                    .with(jwtWithRole("CLERK"))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(EMPLOYEE_ID.toString()))
                    .andExpect(
                            jsonPath("$.employeeNumber")
                                    .value(EMPLOYEE_NUMBER)
                    );

            verify(employeeService).getCurrent(IDENTITY_SUBJECT_ID);
        }
    }

    @Nested
    @DisplayName("PUT /api/employees/{id}/profile")
    class UpdateProfile {

        @Test
        @DisplayName("Happy Path: Should return the updated employee profile")
        void shouldUpdateEmployeeProfile() throws Exception {
            // Arrange
            EmployeeUpdateDto request = new EmployeeUpdateDto(
                    FIRST_NAME,
                    LAST_NAME,
                    "serafim.updated@example.com"
            );

            EmployeeViewDto response = new EmployeeViewDto(
                    EMPLOYEE_ID,
                    EMPLOYEE_NUMBER,
                    FIRST_NAME,
                    LAST_NAME,
                    "serafim.updated@example.com",
                    EmployeePosition.CLERK,
                    EmployeeStatus.ACTIVE,
                    EMPLOYMENT_START_DATE,
                    null
            );

            given(employeeService.updateProfile(EMPLOYEE_ID, request))
                    .willReturn(response);

            // Act & Assert
            mockMvc.perform(
                            put(BASE_URL + "/{id}/profile", EMPLOYEE_ID)
                                    .with(adminJwt())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isOk())
                    .andExpect(
                            jsonPath("$.workEmail")
                                    .value("serafim.updated@example.com")
                    );

            verify(employeeService).updateProfile(EMPLOYEE_ID, request);
        }

        @Test
        @DisplayName("Error Case: Should return 400 for an invalid profile")
        void shouldRejectInvalidProfile() throws Exception {
            // Arrange
            EmployeeUpdateDto invalidRequest = new EmployeeUpdateDto(
                    "",
                    LAST_NAME,
                    "not-an-email"
            );

            // Act & Assert
            mockMvc.perform(
                            put(BASE_URL + "/{id}/profile", EMPLOYEE_ID)
                                    .with(adminJwt())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            jsonMapper.writeValueAsString(
                                                    invalidRequest
                                            )
                                    )
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(
                            jsonPath("$.code")
                                    .value("VALIDATION_FAILED")
                    )
                    .andExpect(jsonPath("$.errors").isArray());

            verify(employeeService, never())
                    .updateProfile(any(), any());
        }
    }

    @Nested
    @DisplayName("PUT /api/employees/{id}/position")
    class ChangePosition {

        @Test
        @DisplayName("Happy Path: Should return the employee with the new position")
        void shouldChangeEmployeePosition() throws Exception {
            // Arrange
            EmployeePositionChangeDto request =
                    new EmployeePositionChangeDto(
                            EmployeePosition.LOAN_OFFICER
                    );

            EmployeeViewDto response = new EmployeeViewDto(
                    EMPLOYEE_ID,
                    EMPLOYEE_NUMBER,
                    FIRST_NAME,
                    LAST_NAME,
                    WORK_EMAIL,
                    EmployeePosition.LOAN_OFFICER,
                    EmployeeStatus.ACTIVE,
                    EMPLOYMENT_START_DATE,
                    null
            );

            given(employeeService.changePosition(EMPLOYEE_ID, request))
                    .willReturn(response);

            // Act & Assert
            mockMvc.perform(
                            put(BASE_URL + "/{id}/position", EMPLOYEE_ID)
                                    .with(adminJwt())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isOk())
                    .andExpect(
                            jsonPath("$.position")
                                    .value("LOAN_OFFICER")
                    );

            verify(employeeService)
                    .changePosition(EMPLOYEE_ID, request);
        }
    }

    @Nested
    @DisplayName("Employee lifecycle")
    class EmployeeLifecycle {

        @Test
        @DisplayName("Happy Path: Should disable an active employee")
        void shouldDisableEmployee() throws Exception {
            // Arrange
            LocalDate endDate = LocalDate.of(2026, 12, 31);
            EmployeeDisableDto request =
                    new EmployeeDisableDto(endDate);

            EmployeeViewDto response = new EmployeeViewDto(
                    EMPLOYEE_ID,
                    EMPLOYEE_NUMBER,
                    FIRST_NAME,
                    LAST_NAME,
                    WORK_EMAIL,
                    EmployeePosition.CLERK,
                    EmployeeStatus.INACTIVE,
                    EMPLOYMENT_START_DATE,
                    endDate
            );

            given(employeeService.disable(EMPLOYEE_ID, request))
                    .willReturn(response);

            // Act & Assert
            mockMvc.perform(
                            post(BASE_URL + "/{id}/disable", EMPLOYEE_ID)
                                    .with(adminJwt())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isOk())
                    .andExpect(
                            jsonPath("$.status")
                                    .value("INACTIVE")
                    )
                    .andExpect(
                            jsonPath("$.employmentEndDate")
                                    .value("2026-12-31")
                    );

            verify(employeeService).disable(EMPLOYEE_ID, request);
        }

        @Test
        @DisplayName("Happy Path: Should reactivate an inactive employee")
        void shouldReactivateEmployee() throws Exception {
            // Arrange
            EmployeeViewDto response = employeeView();

            given(employeeService.reactivate(EMPLOYEE_ID))
                    .willReturn(response);

            // Act & Assert
            mockMvc.perform(
                            post(
                                    BASE_URL + "/{id}/reactivate",
                                    EMPLOYEE_ID
                            )
                                    .with(adminJwt())
                    )
                    .andExpect(status().isOk())
                    .andExpect(
                            jsonPath("$.status")
                                    .value("ACTIVE")
                    )
                    .andExpect(
                            jsonPath("$.employmentEndDate")
                                    .doesNotExist()
                    );

            verify(employeeService).reactivate(EMPLOYEE_ID);
        }
    }
}