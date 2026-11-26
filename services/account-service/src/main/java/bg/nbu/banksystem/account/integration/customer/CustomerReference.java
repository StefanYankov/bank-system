package bg.nbu.banksystem.account.integration.customer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Minimal Customer information required by Account-opening rules.
 *
 * @param id     Customer Service identifier
 * @param status current Customer lifecycle status
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomerReference(
    UUID id,
    CustomerStatus status
) {
}
