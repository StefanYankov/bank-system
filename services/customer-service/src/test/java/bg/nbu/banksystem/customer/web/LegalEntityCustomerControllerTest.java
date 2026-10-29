package bg.nbu.banksystem.customer.web;

import bg.nbu.banksystem.customer.application.dto.CompanyNameUpdateDto;
import bg.nbu.banksystem.customer.application.dto.EikCorrectionDto;
import bg.nbu.banksystem.customer.application.dto.RepresentationEndDto;
import bg.nbu.banksystem.customer.application.dto.RepresentativeCorrectionDto;
import bg.nbu.banksystem.customer.config.SecurityConfig;
import bg.nbu.banksystem.customer.domain.CustomerErrorCode;
import bg.nbu.banksystem.customer.domain.CustomerStatus;
import bg.nbu.banksystem.customer.domain.RepresentativeRole;
import bg.nbu.banksystem.starter.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.time.LocalDate;

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

@WebMvcTest(LegalEntityCustomerController.class)
@Import(SecurityConfig.class)
@DisplayName("LegalEntityCustomerController (MVC)")
class LegalEntityCustomerControllerTest extends CustomerControllerTestSupport {

    private static final String REPRESENTATIVES_URL = LEGAL_ENTITIES_URL + "/{id}/representatives";
    private static final LocalDate VALID_TO = LocalDate.of(2026, 12, 31);

    @Nested
    @DisplayName("POST /api/customers/legal-entities")
    class Register {

        @Test
        @DisplayName("Happy Path: Should return 201 with a Location header and the registered customer")
        void shouldRegister() throws Exception {
            // Arrange
            given(legalEntityCustomerService.register(any())).willReturn(legalEntityView(CustomerStatus.ACTIVE));

            // Act & Assert
            mockMvc.perform(post(LEGAL_ENTITIES_URL).with(jwtWithRole("CLERK")).contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(legalEntityRegistration(EIK))))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost" + LEGAL_ENTITIES_URL + "/" + CUSTOMER_ID))
                    .andExpect(jsonPath("$.eik").value(EIK))
                    .andExpect(jsonPath("$.representatives[0].lastName").value("Doe"));
        }

        @Test
        @DisplayName("Error Case: Should return 400 on the eik field for an EIK with a wrong check digit")
        void shouldRejectInvalidEik() throws Exception {
            // Act & Assert
            mockMvc.perform(post(LEGAL_ENTITIES_URL).with(jwtWithRole("CLERK")).contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(legalEntityRegistration("123456789"))))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("eik"));

            verify(legalEntityCustomerService, never()).register(any());
        }

        @Test
        @DisplayName("Error Case: Should return 409 for an EIK that is already registered")
        void shouldReturnConflictForRegisteredEik() throws Exception {
            // Arrange
            given(legalEntityCustomerService.register(any())).willThrow(
                    new DomainException(CustomerErrorCode.CUSTOMER_EIK_ALREADY_REGISTERED));

            // Act & Assert
            mockMvc.perform(post(LEGAL_ENTITIES_URL).with(jwtWithRole("CLERK")).contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(legalEntityRegistration(EIK))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CUSTOMER_EIK_ALREADY_REGISTERED"));
        }
    }

    @Nested
    @DisplayName("Customer details and changes")
    class CustomerChanges {

        @Test
        @DisplayName("Happy Path: Should return the customer with its representatives")
        void shouldReturnDetails() throws Exception {
            // Arrange
            given(legalEntityCustomerService.getById(CUSTOMER_ID)).willReturn(legalEntityView(CustomerStatus.ACTIVE));

            // Act & Assert
            mockMvc.perform(get(LEGAL_ENTITIES_URL + "/{id}", CUSTOMER_ID).with(jwtWithRole("CLERK")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.companyName").value("Starter Financial Services"))
                    .andExpect(jsonPath("$.representatives.length()").value(1));
        }

        @Test
        @DisplayName("Happy Path: Should update the company name")
        void shouldUpdateCompanyName() throws Exception {
            // Arrange
            given(legalEntityCustomerService.updateCompanyName(eq(CUSTOMER_ID), any())).willReturn(
                    legalEntityView(CustomerStatus.ACTIVE));

            // Act & Assert
            mockMvc.perform(put(LEGAL_ENTITIES_URL + "/{id}/company-name", CUSTOMER_ID).with(jwtWithRole("CLERK"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(new CompanyNameUpdateDto("Starter Holdings"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Error Case: Should return 400 on the eik field for an invalid EIK correction")
        void shouldRejectInvalidEikCorrection() throws Exception {
            // Act & Assert
            mockMvc.perform(put(LEGAL_ENTITIES_URL + "/{id}/eik", CUSTOMER_ID).with(jwtWithRole("CLERK"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(new EikCorrectionDto("1234567860000"))))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("eik"));

            verify(legalEntityCustomerService, never()).correctEik(any(), any());
        }

        @Test
        @DisplayName("Happy Path: Should close the customer")
        void shouldClose() throws Exception {
            // Arrange
            given(legalEntityCustomerService.close(CUSTOMER_ID)).willReturn(legalEntityView(CustomerStatus.CLOSED));

            // Act & Assert
            mockMvc.perform(post(LEGAL_ENTITIES_URL + "/{id}/close", CUSTOMER_ID).with(jwtWithRole("CLERK")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"));
        }
    }

    @Nested
    @DisplayName("Representatives")
    class Representatives {

        @Test
        @DisplayName("Happy Path: Should return 201 with a Location header pointing to the new representative")
        void shouldAddRepresentative() throws Exception {
            // Arrange
            given(legalEntityCustomerService.addRepresentative(eq(CUSTOMER_ID), any())).willReturn(
                    representativeView(null));

            // Act & Assert
            mockMvc.perform(post(REPRESENTATIVES_URL, CUSTOMER_ID).with(jwtWithRole("CLERK"))
                            .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(unlinkedAddition())))
                    .andExpect(status().isCreated()).andExpect(header().string("Location",
                            "http://localhost" + LEGAL_ENTITIES_URL + "/" + CUSTOMER_ID + "/representatives/" + REPRESENTATIVE_ID))
                    .andExpect(jsonPath("$.id").value(REPRESENTATIVE_ID.toString()));
        }

        @Test
        @DisplayName("Error Case: Should return 400 when names break the naming rule")
        void shouldReturnBadRequestForInvalidNames() throws Exception {
            // Arrange
            given(legalEntityCustomerService.addRepresentative(eq(CUSTOMER_ID), any())).willThrow(
                    new DomainException(CustomerErrorCode.INVALID_REPRESENTATIVE_NAMES, "detail"));

            // Act & Assert
            mockMvc.perform(post(REPRESENTATIVES_URL, CUSTOMER_ID).with(jwtWithRole("CLERK"))
                            .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(unlinkedAddition())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REPRESENTATIVE_NAMES"));
        }

        @Test
        @DisplayName("Error Case: Should return 409 when adding to a closed company")
        void shouldReturnConflictForClosedCompany() throws Exception {
            // Arrange
            given(legalEntityCustomerService.addRepresentative(eq(CUSTOMER_ID), any())).willThrow(
                    new DomainException(CustomerErrorCode.CUSTOMER_CLOSED));

            // Act & Assert
            mockMvc.perform(post(REPRESENTATIVES_URL, CUSTOMER_ID).with(jwtWithRole("CLERK"))
                            .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(unlinkedAddition())))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Happy Path: Should return one representative")
        void shouldGetRepresentative() throws Exception {
            // Arrange
            given(legalEntityCustomerService.getRepresentative(CUSTOMER_ID, REPRESENTATIVE_ID)).willReturn(
                    representativeView(null));

            // Act & Assert
            mockMvc.perform(get(REPRESENTATIVES_URL + "/{representativeId}", CUSTOMER_ID, REPRESENTATIVE_ID).with(
                    jwtWithRole("CLERK"))).andExpect(status().isOk()).andExpect(jsonPath("$.firstName").value("Jane"));
        }

        @Test
        @DisplayName("Error Case: Should return 404 for an unknown representative")
        void shouldReturnNotFoundForUnknownRepresentative() throws Exception {
            // Arrange
            given(legalEntityCustomerService.getRepresentative(CUSTOMER_ID, REPRESENTATIVE_ID)).willThrow(
                    new DomainException(CustomerErrorCode.REPRESENTATIVE_NOT_FOUND));

            // Act & Assert
            mockMvc.perform(get(REPRESENTATIVES_URL + "/{representativeId}", CUSTOMER_ID, REPRESENTATIVE_ID).with(
                            jwtWithRole("CLERK"))).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("REPRESENTATIVE_NOT_FOUND"));
        }

        @Test
        @DisplayName("Happy Path: Should end a representation")
        void shouldEndRepresentation() throws Exception {
            // Arrange
            given(legalEntityCustomerService.endRepresentation(eq(CUSTOMER_ID), eq(REPRESENTATIVE_ID),
                    any())).willReturn(representativeView(VALID_TO));

            // Act & Assert
            mockMvc.perform(post(REPRESENTATIVES_URL + "/{representativeId}/end", CUSTOMER_ID, REPRESENTATIVE_ID).with(
                                    jwtWithRole("CLERK")).contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(new RepresentationEndDto(VALID_TO))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.validTo").value("2026-12-31"));
        }

        @Test
        @DisplayName("Error Case: Should return 400 when the end date is missing")
        void shouldRejectMissingEndDate() throws Exception {
            // Act & Assert
            mockMvc.perform(post(REPRESENTATIVES_URL + "/{representativeId}/end", CUSTOMER_ID, REPRESENTATIVE_ID).with(
                            jwtWithRole("CLERK")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("validTo"));
        }

        @Test
        @DisplayName("Error Case: Should return 422 for a correction with the end date before the start date")
        void shouldReturnUnprocessableForInvalidPeriod() throws Exception {
            // Arrange
            given(legalEntityCustomerService.correctRepresentative(eq(CUSTOMER_ID), eq(REPRESENTATIVE_ID),
                    any())).willThrow(new DomainException(CustomerErrorCode.INVALID_REPRESENTATION_PERIOD));

            // Act & Assert
            mockMvc.perform(put(REPRESENTATIVES_URL + "/{representativeId}", CUSTOMER_ID, REPRESENTATIVE_ID).with(
                            jwtWithRole("CLERK")).contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(
                            new RepresentativeCorrectionDto(null, "Jane", null, "Doe", RepresentativeRole.LEGAL_REPRESENTATIVE,
                                    VALID_TO, VALID_FROM)))).andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("INVALID_REPRESENTATION_PERIOD"));
        }
    }
}