package bg.nbu.banksystem.employee.web;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documents error responses shared by Employee Service REST endpoints.
 *
 * <p>
 * This annotation describes the runtime responses produced by the shared
 * error-handling starter. It does not implement exception handling.
 * </p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
        @ApiResponse(
                responseCode = "400",
                description = "Request validation failed or the request is malformed",
                content = @Content(
                        mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class)
                )
        ),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication is missing or invalid",
                content = @Content(
                        mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class)
                )
        ),
        @ApiResponse(
                responseCode = "403",
                description = "The authenticated caller does not have sufficient permission",
                content = @Content(
                        mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class)
                )
        ),
        @ApiResponse(
                responseCode = "500",
                description = "An unexpected server error occurred",
                content = @Content(
                        mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ProblemDetail.class)
                )
        )
})
public @interface ApiStandardResponses {
}