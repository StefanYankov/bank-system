package bg.nbu.banksystem.customer.application.dto;

import bg.nbu.banksystem.customer.domain.CustomerConstraints;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contact information shared by both Customer types.
 *
 * @param email Customer email
 * @param phone Customer phone
 */
public record CustomerContactUpdateDto(
        @Schema(description = "Customer email.", example = "serafim.gerasimoff@example.com")
        @NotBlank
        @Email
        @Size(max = CustomerConstraints.EMAIL_MAX_LENGTH)
        String email,

        @Schema(description = "Customer phone.", example = "+359888123456")
        @NotBlank
        @Size(max = CustomerConstraints.PHONE_MAX_LENGTH)
        String phone
) {
}