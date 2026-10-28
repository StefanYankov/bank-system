package bg.nbu.banksystem.customer.application;

import bg.nbu.banksystem.customer.application.dto.CustomerSummaryDto;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Application service for use cases shared by both Customer types.
 *
 * <p>
 * Type-specific details and changes are handled by {@link IndividualCustomerService} and
 * {@link LegalEntityCustomerService}.
 * </p>
 */
public interface CustomerService {

    /**
     * Searches Customers of both types.
     *
     * <p>
     * Every term in the query must match the Customer number (contains), the EGN or EIK (starts with), or a
     * person or company name (contains). A blank query returns every Customer. Results are sorted by Customer
     * number when the request has no sort.
     * </p>
     *
     * @param query    free-text search query, possibly {@code null} or blank
     * @param pageable paging and sorting information
     * @return a page of Customer summaries
     * @throws bg.nbu.banksystem.starter.error.DomainException if the query contains too many terms
     */
    Page<CustomerSummaryDto> search(@Nullable String query, Pageable pageable);

    /**
     * Retrieves the summary of a Customer of either type.
     *
     * @param id Customer identifier
     * @return the Customer summary
     * @throws bg.nbu.banksystem.starter.error.DomainException if no Customer exists for the identifier
     */
    CustomerSummaryDto getSummary(UUID id);
}