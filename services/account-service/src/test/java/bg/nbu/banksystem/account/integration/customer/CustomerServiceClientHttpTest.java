package bg.nbu.banksystem.account.integration.customer;

import bg.nbu.banksystem.account.domain.AccountErrorCode;
import bg.nbu.banksystem.starter.error.DomainException;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.service.HttpServiceClientPropertiesAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.service.HttpServiceClientAutoConfiguration;
import org.springframework.boot.restclient.test.autoconfigure.AutoConfigureMockRestServiceServer;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RestClientTest(
    properties = {"spring.http.serviceclient.customer-service.connect-timeout=500ms", "spring.http.serviceclient.customer-service.read-timeout=150ms"})
@AutoConfigureMockRestServiceServer(enabled = false)
@Import(
    {CustomerClientConfig.class, CustomerServiceClient.class, CustomerServiceClientHttpTest.TestCircuitBreakerConfig.class})
@ImportAutoConfiguration({HttpServiceClientPropertiesAutoConfiguration.class, HttpServiceClientAutoConfiguration.class})
@DisplayName("CustomerServiceClient HTTP integration")
class CustomerServiceClientHttpTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("01923b7c-8f6d-7a12-9d1e-4b2f8c3e5a11");

    private static final String TOKEN_VALUE = "employee-access-token";
    private static final String CUSTOMER_PATH = "/api/customers/" + CUSTOMER_ID;
    private static final WireMockServer CUSTOMER_SERVICE = startCustomerService();

    @Autowired
    private CustomerServiceClient customerServiceClient;

    private static WireMockServer startCustomerService() {
        WireMockServer server = new WireMockServer(wireMockConfig().dynamicPort());
        server.start();
        return server;
    }

    @DynamicPropertySource
    static void registerCustomerServiceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.http.serviceclient.customer-service.base-url", CUSTOMER_SERVICE::baseUrl);
    }

    @AfterAll
    static void stopCustomerService() {
        CUSTOMER_SERVICE.stop();
    }

    private static void assertCustomerServiceUnavailable(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(DomainException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(AccountErrorCode.CUSTOMER_SERVICE_UNAVAILABLE));
    }

    private static void stubCustomerResponse(int status, String customerStatus) {
        CUSTOMER_SERVICE.stubFor(get(CUSTOMER_PATH).willReturn(
            aResponse().withStatus(status).withHeader("Content-Type", "application/json").withBody("""
                {
                  "id": "%s",
                  "status": "%s"
                }
                """.formatted(CUSTOMER_ID, customerStatus))));
    }

    private static void authenticateEmployee() {
        Instant issuedAt = Instant.parse("2026-11-09T20:00:00Z");
        Jwt jwt = new Jwt(TOKEN_VALUE, issuedAt, issuedAt.plusSeconds(300), Map.of("alg", "none"),
            Map.of("sub", "employee-keycloak-subject"));

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        CUSTOMER_SERVICE.resetAll();
    }

    @Test
    @DisplayName("Happy Path: Should request and map an active Customer with the employee bearer token")
    void shouldRetrieveActiveCustomerWithBearerToken() {
        // Arrange
        authenticateEmployee();
        CUSTOMER_SERVICE.stubFor(get(CUSTOMER_PATH).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("""
                {
                  "id": "%s",
                  "customerNumber": "CUS-000001",
                  "customerType": "INDIVIDUAL",
                  "displayName": "Serafim Gerasimoff",
                  "identifier": "6101047500",
                  "status": "ACTIVE"
                }
                """.formatted(CUSTOMER_ID))));

        // Act
        Optional<CustomerReference> result = customerServiceClient.findById(CUSTOMER_ID);

        // Assert
        assertThat(result).contains(new CustomerReference(CUSTOMER_ID, CustomerStatus.ACTIVE));
        CUSTOMER_SERVICE.verify(
            getRequestedFor(urlEqualTo(CUSTOMER_PATH)).withHeader("Authorization", equalTo("Bearer " + TOKEN_VALUE))
                .withHeader("Accept", equalTo("application/json")));
    }

    @Test
    @DisplayName("Happy Path: Should map a closed Customer response")
    void shouldRetrieveClosedCustomer() {
        // Arrange
        authenticateEmployee();
        stubCustomerResponse(200, "CLOSED");

        // Act
        Optional<CustomerReference> result = customerServiceClient.findById(CUSTOMER_ID);

        // Assert
        assertThat(result).contains(new CustomerReference(CUSTOMER_ID, CustomerStatus.CLOSED));
    }

    @Test
    @DisplayName("Edge Case: Should return empty when Customer Service reports 404")
    void shouldReturnEmptyForUnknownCustomer() {
        // Arrange
        authenticateEmployee();
        CUSTOMER_SERVICE.stubFor(get(CUSTOMER_PATH).willReturn(aResponse().withStatus(404)));

        // Act
        Optional<CustomerReference> result = customerServiceClient.findById(CUSTOMER_ID);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Error Case: Should map Customer Service 503 to unavailable")
    void shouldMapServerFailureToUnavailable() {
        // Arrange
        authenticateEmployee();
        CUSTOMER_SERVICE.stubFor(get(CUSTOMER_PATH).willReturn(aResponse().withStatus(503)));

        // Act & Assert
        assertCustomerServiceUnavailable(() -> customerServiceClient.findById(CUSTOMER_ID));
    }

    @Test
    @DisplayName("Error Case: Should map a response exceeding the read timeout to unavailable")
    void shouldMapReadTimeoutToUnavailable() {
        // Arrange
        authenticateEmployee();
        CUSTOMER_SERVICE.stubFor(get(CUSTOMER_PATH).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "application/json").withFixedDelay(350).withBody("""
                {
                  "id": "%s",
                  "status": "ACTIVE"
                }
                """.formatted(CUSTOMER_ID))));

        // Act & Assert
        assertCustomerServiceUnavailable(() -> customerServiceClient.findById(CUSTOMER_ID));
    }

    @Test
    @DisplayName("Error Case: Should reject Customer lookup without an authenticated employee JWT")
    void shouldRejectLookupWithoutEmployeeJwt() {
        // Arrange
        stubCustomerResponse(200, "ACTIVE");

        // Act & Assert
        assertThatThrownBy(() -> customerServiceClient.findById(CUSTOMER_ID)).isInstanceOf(
            AuthenticationCredentialsNotFoundException.class).hasMessageContaining("authenticated employee JWT");

        assertThat(CUSTOMER_SERVICE.getAllServeEvents()).isEmpty();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestCircuitBreakerConfig {

        @Bean
        CircuitBreakerRegistry circuitBreakerRegistry() {
            return CircuitBreakerRegistry.ofDefaults();
        }
    }
}
