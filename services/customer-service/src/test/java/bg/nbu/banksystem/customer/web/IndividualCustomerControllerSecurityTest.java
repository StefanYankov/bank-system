package bg.nbu.banksystem.customer.web;

import bg.nbu.banksystem.customer.config.SecurityConfig;
import bg.nbu.banksystem.customer.domain.CustomerStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IndividualCustomerController.class)
@Import(SecurityConfig.class)
@DisplayName("IndividualCustomerController security (MVC)")
class IndividualCustomerControllerSecurityTest extends CustomerControllerTestSupport {

    @Test
    @DisplayName("Error Case: Should require authentication")
    void shouldRequireAuthentication() throws Exception {
        // Act & Assert
        mockMvc.perform(get(INDIVIDUALS_URL + "/{id}", CUSTOMER_ID)).andExpect(status().isUnauthorized());

        verify(individualCustomerService, never()).getById(any());
    }

    @Test
    @DisplayName("Error Case: Should forbid a token without a bank role")
    void shouldForbidTokenWithoutRole() throws Exception {
        // Act & Assert
        mockMvc.perform(get(INDIVIDUALS_URL + "/{id}", CUSTOMER_ID).with(jwt())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Happy Path: Should allow a loan officer to read a customer")
    void shouldAllowLoanOfficerRead() throws Exception {
        // Arrange
        given(individualCustomerService.getById(CUSTOMER_ID)).willReturn(individualView(CustomerStatus.ACTIVE));

        // Act & Assert
        mockMvc.perform(get(INDIVIDUALS_URL + "/{id}", CUSTOMER_ID).with(jwtWithRole("LOAN_OFFICER")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Error Case: Should forbid a loan officer from registering a customer")
    void shouldForbidLoanOfficerRegistration() throws Exception {
        // Act & Assert
        mockMvc.perform(post(INDIVIDUALS_URL).with(jwtWithRole("LOAN_OFFICER")).contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(individualRegistration(EGN)))).andExpect(status().isForbidden());

        verify(individualCustomerService, never()).register(any());
    }

    @Test
    @DisplayName("Error Case: Should forbid a loan officer from closing a customer")
    void shouldForbidLoanOfficerClose() throws Exception {
        // Act & Assert
        mockMvc.perform(post(INDIVIDUALS_URL + "/{id}/close", CUSTOMER_ID).with(jwtWithRole("LOAN_OFFICER")))
                .andExpect(status().isForbidden());

        verify(individualCustomerService, never()).close(any());
    }

    @Test
    @DisplayName("Happy Path: Should allow an administrator to close a customer")
    void shouldAllowAdminClose() throws Exception {
        // Arrange
        given(individualCustomerService.close(CUSTOMER_ID)).willReturn(individualView(CustomerStatus.CLOSED));

        // Act & Assert
        mockMvc.perform(post(INDIVIDUALS_URL + "/{id}/close", CUSTOMER_ID).with(jwtWithRole("ADMIN")))
                .andExpect(status().isOk());
    }
}