package bg.nbu.banksystem.customer.web;

import bg.nbu.banksystem.customer.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import(SecurityConfig.class)
@DisplayName("CustomerController security (MVC)")
class CustomerControllerSecurityTest extends CustomerControllerTestSupport {

    private static final String ANGULAR_DEVELOPMENT_ORIGIN = "http://localhost:4200";

    @Test
    @DisplayName("Error Case: Should require authentication for search")
    void shouldRequireAuthentication() throws Exception {
        // Act & Assert
        mockMvc.perform(get(BASE_URL)).andExpect(status().isUnauthorized());

        verify(customerService, never()).search(any(), any());
    }

    @Test
    @DisplayName("Error Case: Should forbid search for a token without a bank role")
    void shouldForbidTokenWithoutRole() throws Exception {
        // Act & Assert
        mockMvc.perform(get(BASE_URL).with(jwt())).andExpect(status().isForbidden());

        verify(customerService, never()).search(any(), any());
    }

    @Test
    @DisplayName("Happy Path: Should allow a loan officer to search")
    void shouldAllowLoanOfficerSearch() throws Exception {
        // Arrange
        given(customerService.search(any(), any())).willReturn(Page.empty());

        // Act & Assert
        mockMvc.perform(get(BASE_URL).with(jwtWithRole("LOAN_OFFICER"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Happy Path: Should accept a search preflight from the Angular development origin")
    void shouldAcceptPreflight() throws Exception {
        // Act & Assert
        mockMvc.perform(options(BASE_URL).header(HttpHeaders.ORIGIN, ANGULAR_DEVELOPMENT_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization")).andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ANGULAR_DEVELOPMENT_ORIGIN));
    }
}