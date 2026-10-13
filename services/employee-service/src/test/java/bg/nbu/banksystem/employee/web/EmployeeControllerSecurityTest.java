package bg.nbu.banksystem.employee.web;

import bg.nbu.banksystem.employee.application.dto.EmployeeRegistrationDto;
import bg.nbu.banksystem.employee.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the Employee API role policy declared with {@code @PreAuthorize}.
 *
 * <p>
 * Token decoding, Keycloak role mapping and the shape of 401/403 problem responses
 * are covered by {@code SecurityConfigTest}.
 * </p>
 */
@WebMvcTest(EmployeeController.class)
@Import(SecurityConfig.class)
@DisplayName("EmployeeController security (MVC)")
class EmployeeControllerSecurityTest extends EmployeeControllerTestSupport {

    @Test
    @DisplayName("Happy Path: Should allow a clerk to retrieve another employee")
    void shouldAllowStaffEmployeeLookup() throws Exception {
        // Arrange
        given(employeeService.getById(EMPLOYEE_ID)).willReturn(employeeView());

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/{id}", EMPLOYEE_ID)
                        .with(jwtWithRole("CLERK")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeNumber").value(EMPLOYEE_NUMBER));

        verify(employeeService).getById(EMPLOYEE_ID);
    }

    @Test
    @DisplayName("Happy Path: Should resolve the current employee from the token subject")
    void shouldResolveCurrentEmployeeFromSubject() throws Exception {
        // Arrange
        given(employeeService.getCurrent(IDENTITY_SUBJECT_ID)).willReturn(employeeView());

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/me")
                        .with(jwtWithRole("LOAN_OFFICER")))
                .andExpect(status().isOk());

        verify(employeeService).getCurrent(IDENTITY_SUBJECT_ID);
    }

    @Test
    @DisplayName("Error Case: Should forbid a clerk from registering an employee")
    void shouldForbidRegistrationByClerk() throws Exception {
        // Arrange
        EmployeeRegistrationDto request = registrationDto();

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .with(jwtWithRole("CLERK"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(employeeService, never()).register(any());
    }

    @Test
    @DisplayName("Happy Path: Should allow an administrator to register an employee")
    void shouldAllowRegistrationByAdministrator() throws Exception {
        // Arrange
        EmployeeRegistrationDto request = registrationDto();
        given(employeeService.register(request)).willReturn(employeeView());

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(employeeService).register(request);
    }
}