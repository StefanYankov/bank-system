package bg.nbu.banksystem.customer.web;

import bg.nbu.banksystem.customer.config.SecurityConfig;
import bg.nbu.banksystem.customer.domain.CustomerStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LegalEntityCustomerController.class)
@Import(SecurityConfig.class)
@DisplayName("LegalEntityCustomerController security (MVC)")
class LegalEntityCustomerControllerSecurityTest extends CustomerControllerTestSupport {

    private static final String REPRESENTATIVES_URL = LEGAL_ENTITIES_URL + "/{id}/representatives";

    @Test
    @DisplayName("Error Case: Should require authentication")
    void shouldRequireAuthentication() throws Exception {
        // Act & Assert
        mockMvc.perform(get(LEGAL_ENTITIES_URL + "/{id}", CUSTOMER_ID)).andExpect(status().isUnauthorized());

        verify(legalEntityCustomerService, never()).getById(any());
    }

    @Test
    @DisplayName("Error Case: Should forbid a token without a bank role")
    void shouldForbidTokenWithoutRole() throws Exception {
        // Act & Assert
        mockMvc.perform(get(LEGAL_ENTITIES_URL + "/{id}", CUSTOMER_ID).with(jwt())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Happy Path: Should allow a loan officer to read a representative")
    void shouldAllowLoanOfficerReadRepresentative() throws Exception {
        // Arrange
        given(legalEntityCustomerService.getRepresentative(CUSTOMER_ID, REPRESENTATIVE_ID)).willReturn(
                representativeView(null));

        // Act & Assert
        mockMvc.perform(get(REPRESENTATIVES_URL + "/{representativeId}", CUSTOMER_ID, REPRESENTATIVE_ID).with(
                jwtWithRole("LOAN_OFFICER"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Error Case: Should forbid a loan officer from registering a customer")
    void shouldForbidLoanOfficerRegistration() throws Exception {
        // Act & Assert
        mockMvc.perform(
                        post(LEGAL_ENTITIES_URL).with(jwtWithRole("LOAN_OFFICER")).contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(legalEntityRegistration(EIK))))
                .andExpect(status().isForbidden());

        verify(legalEntityCustomerService, never()).register(any());
    }

    @Test
    @DisplayName("Error Case: Should forbid a loan officer from adding a representative")
    void shouldForbidLoanOfficerAddRepresentative() throws Exception {
        // Act & Assert
        mockMvc.perform(post(REPRESENTATIVES_URL, CUSTOMER_ID).with(jwtWithRole("LOAN_OFFICER"))
                        .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(unlinkedAddition())))
                .andExpect(status().isForbidden());

        verify(legalEntityCustomerService, never()).addRepresentative(any(), any());
    }

    @Test
    @DisplayName("Happy Path: Should allow a clerk to add a representative")
    void shouldAllowClerkAddRepresentative() throws Exception {
        // Arrange
        given(legalEntityCustomerService.addRepresentative(eq(CUSTOMER_ID), any())).willReturn(
                representativeView(null));

        // Act & Assert
        mockMvc.perform(post(REPRESENTATIVES_URL, CUSTOMER_ID).with(jwtWithRole("CLERK"))
                        .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(unlinkedAddition())))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Happy Path: Should allow an administrator to close a customer")
    void shouldAllowAdminClose() throws Exception {
        // Arrange
        given(legalEntityCustomerService.close(CUSTOMER_ID)).willReturn(legalEntityView(CustomerStatus.CLOSED));

        // Act & Assert
        mockMvc.perform(post(LEGAL_ENTITIES_URL + "/{id}/close", CUSTOMER_ID).with(jwtWithRole("ADMIN")))
                .andExpect(status().isOk());
    }
}