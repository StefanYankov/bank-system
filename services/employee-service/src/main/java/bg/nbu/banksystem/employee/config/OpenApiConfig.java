package bg.nbu.banksystem.employee.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * OpenAPI metadata for Employee Service.
 *
 * <p>
 * Defines the bearer-token security scheme and applies it to every operation. The
 * documented version is read from the Gradle-generated build info, falling back to
 * {@code unknown} when that information is absent.
 * </p>
 */
@Configuration
public class OpenApiConfig {

    static final String BEARER_SCHEME_NAME = "bearerAuth";
    static final String UNKNOWN_VERSION = "unknown";

    private final Optional<BuildProperties> buildProperties;

    /**
     * @param buildProperties build information, absent when build-info.properties was not generated
     */
    public OpenApiConfig(Optional<BuildProperties> buildProperties) {
        this.buildProperties = buildProperties;
    }

    /**
     * @return the OpenAPI definition for Employee Service
     */
    @Bean
    public OpenAPI employeeServiceOpenApi() {
        String version = buildProperties
                .map(BuildProperties::getVersion)
                .orElse(UNKNOWN_VERSION);

        return new OpenAPI()
                .info(new Info()
                        .title("Employee Service API")
                        .version(version)
                        .description("Registration, retrieval and lifecycle management of bank employees."))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}