package bg.nbu.banksystem.account.integration.customer;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.UUID;

/**
 * Raw HTTP contract exposed by Customer Service.
 */
@HttpExchange(accept = "application/json")
public interface CustomerServiceHttpClient {

    /**
     * Retrieves the Customer summary used by Account opening.
     *
     * @param customerId Customer Service identifier
     * @return current Customer reference
     */
    @GetExchange("/api/customers/{id}")
    CustomerReference getCustomerById(
        @PathVariable("id")
        UUID customerId);
}
