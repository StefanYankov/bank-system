package bg.nbu.banksystem.employee.web;

import bg.nbu.banksystem.employee.application.EmployeeService;
import bg.nbu.banksystem.employee.application.dto.EmployeeRegistrationDto;
import bg.nbu.banksystem.employee.application.dto.EmployeeViewDto;
import bg.nbu.banksystem.employee.domain.EmployeePosition;
import bg.nbu.banksystem.employee.domain.EmployeeStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Shared fixtures and HTTP-test infrastructure for Employee controller tests.
 */
abstract class EmployeeControllerTestSupport {

    protected static final String BASE_URL = "/api/employees";

    protected static final UUID EMPLOYEE_ID =
            UUID.fromString("01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a10");

    protected static final String EMPLOYEE_NUMBER = "EMP-000001";
    protected static final String IDENTITY_SUBJECT_ID = "subject-001";
    protected static final String FIRST_NAME = "Serafim";
    protected static final String LAST_NAME = "Gerasimoff";
    protected static final String WORK_EMAIL =
            "serafim.gerasimoff@example.com";

    protected static final LocalDate EMPLOYMENT_START_DATE =
            LocalDate.of(2026, 10, 1);

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JsonMapper jsonMapper;

    @MockitoBean
    protected EmployeeService employeeService;

    protected static EmployeeRegistrationDto registrationDto() {
        return new EmployeeRegistrationDto(
                IDENTITY_SUBJECT_ID,
                FIRST_NAME,
                LAST_NAME,
                WORK_EMAIL,
                EmployeePosition.CLERK,
                EMPLOYMENT_START_DATE
        );
    }

    protected static EmployeeViewDto employeeView() {
        return new EmployeeViewDto(
                EMPLOYEE_ID,
                EMPLOYEE_NUMBER,
                FIRST_NAME,
                LAST_NAME,
                WORK_EMAIL,
                EmployeePosition.CLERK,
                EmployeeStatus.ACTIVE,
                EMPLOYMENT_START_DATE,
                null
        );
    }

    protected static JwtRequestPostProcessor jwtWithRole(String role) {
        return jwt()
                .jwt(token -> token.subject(IDENTITY_SUBJECT_ID))
                .authorities(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );
    }

    protected static JwtRequestPostProcessor adminJwt() {
        return jwtWithRole("ADMIN");
    }
}