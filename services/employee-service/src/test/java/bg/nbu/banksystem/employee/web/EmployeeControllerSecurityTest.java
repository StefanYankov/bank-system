package bg.nbu.banksystem.employee.web;

import bg.nbu.banksystem.employee.application.dto.EmployeeRegistrationDto;
import bg.nbu.banksystem.employee.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies the Employee API security and CORS policies.
 *
 * <p>
 * Token decoding, Keycloak role mapping, and the shape of 401/403 problem
 * responses are covered by {@code SecurityConfigTest}.
 * </p>
 */
@WebMvcTest(EmployeeController.class)
@Import(SecurityConfig.class)
@DisplayName("EmployeeController security (MVC)")
class EmployeeControllerSecurityTest extends EmployeeControllerTestSupport {

    private static final String ANGULAR_DEVELOPMENT_ORIGIN =
            "http://localhost:4200";

    private static final String UNAPPROVED_ORIGIN =
            "http://localhost:4300";

    @Test
    @DisplayName("Happy Path: Should accept Employee API preflight from Angular development origin")
    void shouldAcceptPreflightFromAngularDevelopmentOrigin() throws Exception {

        mockMvc.perform(options(BASE_URL + "/me")
                        .header(
                                HttpHeaders.ORIGIN,
                                ANGULAR_DEVELOPMENT_ORIGIN
                        )
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                                "GET"
                        )
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "Authorization, Content-Type"
                        ))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        ANGULAR_DEVELOPMENT_ORIGIN
                ))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        containsString("GET")
                ))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        containsString("Authorization")
                ))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        containsString("Content-Type")
                ));

        verify(employeeService, never()).getCurrent(anyString());
    }

    @Test
    @DisplayName("Error Case: Should reject Employee API preflight from an unapproved origin")
    void shouldRejectPreflightFromUnapprovedOrigin() throws Exception {
        mockMvc.perform(options(BASE_URL + "/me")
                        .header(
                                HttpHeaders.ORIGIN,
                                UNAPPROVED_ORIGIN
                        )
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                                "GET"
                        )
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "Authorization, Content-Type"
                        ))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN
                ));

        verify(employeeService, never()).getCurrent(anyString());
    }

    @Test
    @DisplayName("Error Case: Should require authentication for the real current employee request")
    void shouldRequireAuthenticationForCurrentEmployeeRequest() throws Exception {
        mockMvc.perform(get(BASE_URL + "/me")
                        .header(
                                HttpHeaders.ORIGIN,
                                ANGULAR_DEVELOPMENT_ORIGIN
                        ))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        ANGULAR_DEVELOPMENT_ORIGIN
                ));

        verify(employeeService, never()).getCurrent(anyString());
    }

    @Test
    @DisplayName("Happy Path: Should allow a clerk to retrieve another employee")
    void shouldAllowStaffEmployeeLookup() throws Exception {
        // Arrange
        given(employeeService.getById(EMPLOYEE_ID))
                .willReturn(employeeView());

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/{id}", EMPLOYEE_ID)
                        .with(jwtWithRole("CLERK")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeNumber")
                        .value(EMPLOYEE_NUMBER));

        verify(employeeService).getById(EMPLOYEE_ID);
    }

    @Test
    @DisplayName("Happy Path: Should resolve the current employee from the token subject")
    void shouldResolveCurrentEmployeeFromSubject() throws Exception {
        // Arrange
        given(employeeService.getCurrent(IDENTITY_SUBJECT_ID))
                .willReturn(employeeView());

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

        given(employeeService.register(request))
                .willReturn(employeeView());

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(employeeService).register(request);
    }
}