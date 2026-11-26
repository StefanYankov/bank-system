package bg.nbu.banksystem.account.integration.customer;

import bg.nbu.banksystem.account.domain.AccountErrorCode;
import bg.nbu.banksystem.starter.error.DomainException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerServiceClient circuit breaker (unit)")
class CustomerServiceClientCircuitBreakerTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a11");

    @Mock
    private CustomerServiceHttpClient httpClient;

    private CustomerServiceClient customerServiceClient;
    private CircuitBreaker circuitBreaker;

    private static void assertUnavailable(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(DomainException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(AccountErrorCode.CUSTOMER_SERVICE_UNAVAILABLE));
    }

    private static HttpServerErrorException serviceUnavailable() {
        return HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", HttpHeaders.EMPTY,
            new byte[0], StandardCharsets.UTF_8);
    }

    private static HttpClientErrorException notFound() {
        return HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0],
            StandardCharsets.UTF_8);
    }

    @BeforeEach
    void setUp() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED).slidingWindowSize(2)
            .minimumNumberOfCalls(2).failureRateThreshold(50).recordExceptions(HttpServerErrorException.class,
                org.springframework.web.client.ResourceAccessException.class, DomainException.class)
            .ignoreExceptions(HttpClientErrorException.class).build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        circuitBreaker = registry.circuitBreaker(CustomerServiceClient.CIRCUIT_BREAKER_NAME);
        customerServiceClient = new CustomerServiceClient(httpClient, registry);
    }

    @Test
    @DisplayName("Error Case: Should open the circuit after repeated technical failures")
    void shouldOpenCircuitAfterTechnicalFailures() {
        // Arrange
        when(httpClient.getCustomerById(CUSTOMER_ID)).thenThrow(serviceUnavailable()).thenThrow(serviceUnavailable());

        // Act & Assert
        assertUnavailable(() -> customerServiceClient.findById(CUSTOMER_ID));
        assertUnavailable(() -> customerServiceClient.findById(CUSTOMER_ID));
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // An open circuit fails fast and the raw HTTP client is not called again.
        assertUnavailable(() -> customerServiceClient.findById(CUSTOMER_ID));
        verify(httpClient, times(2)).getCustomerById(CUSTOMER_ID);
    }

    @Test
    @DisplayName("Edge Case: Should not count Customer 404 responses as circuit failures")
    void shouldIgnoreCustomerNotFoundResponses() {
        // Arrange
        when(httpClient.getCustomerById(CUSTOMER_ID)).thenThrow(notFound()).thenThrow(notFound()).thenThrow(notFound());

        // Act
        Optional<CustomerReference> first = customerServiceClient.findById(CUSTOMER_ID);
        Optional<CustomerReference> second = customerServiceClient.findById(CUSTOMER_ID);
        Optional<CustomerReference> third = customerServiceClient.findById(CUSTOMER_ID);

        // Assert
        assertThat(first).isEmpty();
        assertThat(second).isEmpty();
        assertThat(third).isEmpty();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
        verify(httpClient, times(3)).getCustomerById(CUSTOMER_ID);
    }

    @Test
    @DisplayName("Happy Path: Should keep the circuit closed for successful Customer responses")
    void shouldKeepCircuitClosedForSuccessfulResponse() {
        // Arrange
        CustomerReference customer = new CustomerReference(CUSTOMER_ID, CustomerStatus.ACTIVE);
        when(httpClient.getCustomerById(CUSTOMER_ID)).thenReturn(customer);

        // Act
        Optional<CustomerReference> result = customerServiceClient.findById(CUSTOMER_ID);

        // Assert
        assertThat(result).contains(customer);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
    }
}
