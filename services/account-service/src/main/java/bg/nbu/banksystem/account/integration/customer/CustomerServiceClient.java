package bg.nbu.banksystem.account.integration.customer;

import bg.nbu.banksystem.starter.error.DomainException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Optional;
import java.util.UUID;

import static bg.nbu.banksystem.account.domain.AccountErrorCode.CUSTOMER_SERVICE_UNAVAILABLE;

/**
 * Provides Account Service with resilient access to current Customer information.
 */
@Component
@RequiredArgsConstructor
public class CustomerServiceClient {

    static final String CIRCUIT_BREAKER_NAME = "customer-service";

    private final CustomerServiceHttpClient httpClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    private static CustomerReference validateResponse(UUID customerId, CustomerReference customer) {
        if (customer == null || customer.id() == null || customer.status() == null || !customerId.equals(
            customer.id())) {
            throw new DomainException(CUSTOMER_SERVICE_UNAVAILABLE);
        }

        return customer;
    }

    /**
     * Finds the current Customer information required for Account opening.
     *
     * @param customerId Customer Service identifier
     * @return the Customer when found, otherwise empty
     */
    public Optional<CustomerReference> findById(UUID customerId) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);

        try {
            CustomerReference customer = circuitBreaker.executeSupplier(
                () -> validateResponse(customerId, httpClient.getCustomerById(customerId)));

            return Optional.of(customer);
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        } catch (CallNotPermittedException | HttpServerErrorException | ResourceAccessException exception) {
            throw new DomainException(CUSTOMER_SERVICE_UNAVAILABLE);
        }
    }
}
