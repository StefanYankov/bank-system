package bg.nbu.banksystem.customer.web;

import bg.nbu.banksystem.customer.application.CustomerService;
import bg.nbu.banksystem.customer.application.dto.CustomerSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST API for searching Customers of both types and retrieving their summaries.
 *
 * <p>
 * Type-specific details and changes are served under {@code /api/customers/individuals} and
 * {@code /api/customers/legal-entities}.
 * </p>
 */
@RestController
@RequestMapping(value = "/api/customers", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@ApiStandardResponses
@Tag(name = "Customer API", description = "Endpoints for searching Customers and retrieving their summaries.")
public class CustomerController {

    static final int SEARCH_QUERY_MAX_LENGTH = 100;

    private final CustomerService customerService;

    /**
     * Searches Customers of both types.
     *
     * @param query    free-text search query; blank returns every Customer
     * @param pageable paging and sorting information
     * @return a stable paged representation of Customer summaries
     */
    @Operation(operationId = "searchCustomers", summary = "Search customers",
            description = "Every term must match the customer number, the start of the EGN or EIK, or a name. " + "Sortable by customerNumber, status and createdAt; the default is customerNumber ascending.")
    @ApiResponse(responseCode = "200", description = "Customers retrieved")
    @GetMapping
    @PreAuthorize("hasAnyRole('CLERK', 'LOAN_OFFICER', 'ADMIN')")
    public ResponseEntity<PagedResponse<CustomerSummaryDto>> searchCustomers(
            @Parameter(description = "Free-text search query", example = "Gerasimoff 6101")
            @RequestParam(required = false)
            @Size(max = SEARCH_QUERY_MAX_LENGTH)
            String query,
            @ParameterObject
            Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(customerService.search(query, pageable)));
    }

    /**
     * Retrieves the summary of a Customer of either type.
     *
     * @param id Customer identifier
     * @return the Customer summary
     */
    @Operation(operationId = "getCustomerSummary", summary = "Get a customer summary",
            description = "Retrieves the summary of a customer of either type, including its type.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Customer summary retrieved"), @ApiResponse(
            responseCode = "404", description = "Customer not found",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))})
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLERK', 'LOAN_OFFICER', 'ADMIN')")
    public ResponseEntity<CustomerSummaryDto> getCustomerSummary(
            @Parameter(description = "Technical customer UUID")
            @PathVariable
            UUID id) {
        return ResponseEntity.ok(customerService.getSummary(id));
    }
}