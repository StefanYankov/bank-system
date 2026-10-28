package bg.nbu.banksystem.customer.web;

import bg.nbu.banksystem.customer.config.SecurityConfig;
import bg.nbu.banksystem.customer.domain.CustomerErrorCode;
import bg.nbu.banksystem.starter.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import(SecurityConfig.class)
@DisplayName("CustomerController (MVC)")
class CustomerControllerTest extends CustomerControllerTestSupport {

    @Nested
    @DisplayName("GET /api/customers")
    class Search {

        @Test
        @DisplayName("Happy Path: Should return a stable paged response of summaries")
        void shouldReturnSummaryPage() throws Exception {
            // Arrange
            PageRequest pageable = PageRequest.of(0, 20);
            given(customerService.search("Gerasimoff", pageable)).willReturn(
                    new PageImpl<>(List.of(individualSummary()), pageable, 1));

            // Act & Assert
            mockMvc.perform(get(BASE_URL).with(jwtWithRole("CLERK")).param("query", "Gerasimoff").param("page", "0")
                            .param("size", "20")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].customerNumber").value(CUSTOMER_NUMBER))
                    .andExpect(jsonPath("$.content[0].customerType").value("INDIVIDUAL"))
                    .andExpect(jsonPath("$.content[0].identifier").value(EGN))
                    .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.last").value(true));
        }

        @Test
        @DisplayName("Happy Path: Should pass the requested sort to the service")
        void shouldPassSortToService() throws Exception {
            // Arrange
            PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            given(customerService.search(null, pageable)).willReturn(new PageImpl<>(List.of(), pageable, 0));

            // Act & Assert
            mockMvc.perform(get(BASE_URL).with(jwtWithRole("CLERK")).param("page", "0").param("size", "20")
                    .param("sort", "createdAt,desc")).andExpect(status().isOk());

            verify(customerService).search(null, pageable);
        }

        @Test
        @DisplayName("Error Case: Should return 400 for a query longer than the maximum")
        void shouldRejectTooLongQuery() throws Exception {
            // Act & Assert
            mockMvc.perform(get(BASE_URL).with(jwtWithRole("CLERK"))
                            .param("query", "a".repeat(CustomerController.SEARCH_QUERY_MAX_LENGTH + 1)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

            verify(customerService, never()).search(any(), any());
        }

        @Test
        @DisplayName("Error Case: Should return 400 with the error code for a disallowed sort property")
        void shouldReturnDisallowedSortError() throws Exception {
            // Arrange
            given(customerService.search(any(), any())).willThrow(
                    new DomainException(CustomerErrorCode.INVALID_SORT_PROPERTY, "detail"));

            // Act & Assert
            mockMvc.perform(get(BASE_URL).with(jwtWithRole("CLERK")).param("sort", "email"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_SORT_PROPERTY"));
        }
    }

    @Nested
    @DisplayName("GET /api/customers/{id}")
    class GetSummary {

        @Test
        @DisplayName("Happy Path: Should return the customer summary")
        void shouldReturnSummary() throws Exception {
            // Arrange
            given(customerService.getSummary(CUSTOMER_ID)).willReturn(individualSummary());

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/{id}", CUSTOMER_ID).with(jwtWithRole("LOAN_OFFICER")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(CUSTOMER_ID.toString()))
                    .andExpect(jsonPath("$.displayName").value("Serafim Ivanov Gerasimoff"));
        }

        @Test
        @DisplayName("Error Case: Should return 404 for an unknown customer")
        void shouldReturnNotFound() throws Exception {
            // Arrange
            given(customerService.getSummary(CUSTOMER_ID)).willThrow(
                    new DomainException(CustomerErrorCode.CUSTOMER_NOT_FOUND));

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/{id}", CUSTOMER_ID).with(jwtWithRole("CLERK")))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
        }
    }
}