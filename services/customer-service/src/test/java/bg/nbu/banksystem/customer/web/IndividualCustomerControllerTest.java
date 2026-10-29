package bg.nbu.banksystem.customer.web;

import bg.nbu.banksystem.customer.application.dto.CustomerContactUpdateDto;
import bg.nbu.banksystem.customer.application.dto.EgnCorrectionDto;
import bg.nbu.banksystem.customer.application.dto.IndividualCustomerNameUpdateDto;
import bg.nbu.banksystem.customer.config.SecurityConfig;
import bg.nbu.banksystem.customer.domain.CustomerErrorCode;
import bg.nbu.banksystem.customer.domain.CustomerStatus;
import bg.nbu.banksystem.starter.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IndividualCustomerController.class)
@Import(SecurityConfig.class)
@DisplayName("IndividualCustomerController (MVC)")
class IndividualCustomerControllerTest extends CustomerControllerTestSupport {

    @Nested
    @DisplayName("POST /api/customers/individuals")
    class Register {

        @Test
        @DisplayName("Happy Path: Should return 201 with a Location header and the registered customer")
        void shouldRegister() throws Exception {
            // Arrange
            given(individualCustomerService.register(any())).willReturn(individualView(CustomerStatus.ACTIVE));

            // Act & Assert
            mockMvc.perform(post(INDIVIDUALS_URL).with(jwtWithRole("CLERK")).contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(individualRegistration(EGN))))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost" + INDIVIDUALS_URL + "/" + CUSTOMER_ID))
                    .andExpect(jsonPath("$.customerNumber").value(CUSTOMER_NUMBER))
                    .andExpect(jsonPath("$.residentialAddress.city").value("Sofia"));
        }

        @Test
        @DisplayName("Error Case: Should return 400 on the egn field for an EGN with a wrong check digit")
        void shouldRejectInvalidEgn() throws Exception {
            // Act & Assert
            mockMvc.perform(post(INDIVIDUALS_URL).with(jwtWithRole("CLERK")).contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(individualRegistration("6101047501"))))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[0].field").value("egn"));

            verify(individualCustomerService, never()).register(any());
        }

        @Test
        @DisplayName("Error Case: Should return 409 for an EGN that is already registered")
        void shouldReturnConflictForRegisteredEgn() throws Exception {
            // Arrange
            given(individualCustomerService.register(any())).willThrow(
                    new DomainException(CustomerErrorCode.CUSTOMER_EGN_ALREADY_REGISTERED));

            // Act & Assert
            mockMvc.perform(post(INDIVIDUALS_URL).with(jwtWithRole("CLERK")).contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(individualRegistration(EGN))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CUSTOMER_EGN_ALREADY_REGISTERED"));
        }
    }

    @Nested
    @DisplayName("GET /api/customers/individuals/{id}")
    class GetById {

        @Test
        @DisplayName("Happy Path: Should return the customer details")
        void shouldReturnDetails() throws Exception {
            // Arrange
            given(individualCustomerService.getById(CUSTOMER_ID)).willReturn(individualView(CustomerStatus.ACTIVE));

            // Act & Assert
            mockMvc.perform(get(INDIVIDUALS_URL + "/{id}", CUSTOMER_ID).with(jwtWithRole("CLERK")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.egn").value(EGN))
                    .andExpect(jsonPath("$.middleName").value("Ivanov"));
        }

        @Test
        @DisplayName("Error Case: Should return 404 for an unknown customer")
        void shouldReturnNotFound() throws Exception {
            // Arrange
            given(individualCustomerService.getById(CUSTOMER_ID)).willThrow(
                    new DomainException(CustomerErrorCode.CUSTOMER_NOT_FOUND));

            // Act & Assert
            mockMvc.perform(get(INDIVIDUALS_URL + "/{id}", CUSTOMER_ID).with(jwtWithRole("CLERK")))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("Profile changes")
    class ProfileChanges {

        @Test
        @DisplayName("Happy Path: Should update the name")
        void shouldUpdateName() throws Exception {
            // Arrange
            given(individualCustomerService.updateName(eq(CUSTOMER_ID), any())).willReturn(
                    individualView(CustomerStatus.ACTIVE));

            // Act & Assert
            mockMvc.perform(put(INDIVIDUALS_URL + "/{id}/name", CUSTOMER_ID).with(jwtWithRole("CLERK"))
                            .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(
                                    new IndividualCustomerNameUpdateDto("Serafim", null, "Gerasimoff"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Error Case: Should return 400 for a blank last name")
        void shouldRejectBlankLastName() throws Exception {
            // Act & Assert
            mockMvc.perform(put(INDIVIDUALS_URL + "/{id}/name", CUSTOMER_ID).with(jwtWithRole("CLERK"))
                            .contentType(MediaType.APPLICATION_JSON).content(
                                    jsonMapper.writeValueAsString(new IndividualCustomerNameUpdateDto("Serafim", null, "   "))))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("lastName"));

            verify(individualCustomerService, never()).updateName(any(), any());
        }

        @Test
        @DisplayName("Happy Path: Should replace the residential address")
        void shouldChangeResidentialAddress() throws Exception {
            // Arrange
            given(individualCustomerService.changeResidentialAddress(eq(CUSTOMER_ID), any())).willReturn(
                    individualView(CustomerStatus.ACTIVE));

            // Act & Assert
            mockMvc.perform(put(INDIVIDUALS_URL + "/{id}/residential-address", CUSTOMER_ID).with(jwtWithRole("CLERK"))
                            .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(address())))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Happy Path: Should update the contact information")
        void shouldUpdateContact() throws Exception {
            // Arrange
            given(individualCustomerService.updateContactInformation(eq(CUSTOMER_ID), any())).willReturn(
                    individualView(CustomerStatus.ACTIVE));

            // Act & Assert
            mockMvc.perform(put(INDIVIDUALS_URL + "/{id}/contact", CUSTOMER_ID).with(jwtWithRole("CLERK"))
                            .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(
                                    new CustomerContactUpdateDto("serafim.gerasimoff@example.com", "+359888123456"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Error Case: Should return 409 for a change to a closed customer")
        void shouldReturnConflictForClosedCustomer() throws Exception {
            // Arrange
            given(individualCustomerService.updateContactInformation(eq(CUSTOMER_ID), any())).willThrow(
                    new DomainException(CustomerErrorCode.CUSTOMER_CLOSED));

            // Act & Assert
            mockMvc.perform(put(INDIVIDUALS_URL + "/{id}/contact", CUSTOMER_ID).with(jwtWithRole("CLERK"))
                            .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(
                                    new CustomerContactUpdateDto("serafim.gerasimoff@example.com", "+359888123456"))))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CUSTOMER_CLOSED"));
        }
    }

    @Nested
    @DisplayName("PUT /api/customers/individuals/{id}/egn")
    class EgnCorrection {

        @Test
        @DisplayName("Happy Path: Should correct the EGN")
        void shouldCorrectEgn() throws Exception {
            // Arrange
            given(individualCustomerService.correctEgn(eq(CUSTOMER_ID), any())).willReturn(
                    individualView(CustomerStatus.ACTIVE));

            // Act & Assert
            mockMvc.perform(put(INDIVIDUALS_URL + "/{id}/egn", CUSTOMER_ID).with(jwtWithRole("CLERK"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(new EgnCorrectionDto(EGN)))).andExpect(status().isOk());
        }

        @Test
        @DisplayName("Error Case: Should return 400 for an invalid EGN")
        void shouldRejectInvalidEgn() throws Exception {
            // Act & Assert
            mockMvc.perform(put(INDIVIDUALS_URL + "/{id}/egn", CUSTOMER_ID).with(jwtWithRole("CLERK"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(new EgnCorrectionDto("6113047500"))))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("egn"));

            verify(individualCustomerService, never()).correctEgn(any(), any());
        }

        @Test
        @DisplayName("Error Case: Should return 409 for an EGN that belongs to another customer")
        void shouldReturnConflictForEgnOfAnotherCustomer() throws Exception {
            // Arrange
            given(individualCustomerService.correctEgn(eq(CUSTOMER_ID), any())).willThrow(
                    new DomainException(CustomerErrorCode.CUSTOMER_EGN_ALREADY_REGISTERED));

            // Act & Assert
            mockMvc.perform(put(INDIVIDUALS_URL + "/{id}/egn", CUSTOMER_ID).with(jwtWithRole("CLERK"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(new EgnCorrectionDto(EGN))))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("Happy Path: Should close the customer")
        void shouldClose() throws Exception {
            // Arrange
            given(individualCustomerService.close(CUSTOMER_ID)).willReturn(individualView(CustomerStatus.CLOSED));

            // Act & Assert
            mockMvc.perform(post(INDIVIDUALS_URL + "/{id}/close", CUSTOMER_ID).with(jwtWithRole("CLERK")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"));
        }

        @Test
        @DisplayName("Error Case: Should return 409 when reopening an active customer")
        void shouldReturnConflictForActiveCustomer() throws Exception {
            // Arrange
            given(individualCustomerService.reopen(CUSTOMER_ID)).willThrow(
                    new DomainException(CustomerErrorCode.CUSTOMER_ALREADY_ACTIVE));

            // Act & Assert
            mockMvc.perform(post(INDIVIDUALS_URL + "/{id}/reopen", CUSTOMER_ID).with(jwtWithRole("CLERK")))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CUSTOMER_ALREADY_ACTIVE"));
        }
    }
}