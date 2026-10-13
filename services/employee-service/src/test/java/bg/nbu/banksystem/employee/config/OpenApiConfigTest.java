package bg.nbu.banksystem.employee.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;

import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OpenApiConfig (unit)")
class OpenApiConfigTest {

    @Test
    @DisplayName("Happy Path: Should use the version from build properties")
    void shouldUseBuildVersion() {
        // Arrange
        OpenApiConfig config = new OpenApiConfig(Optional.of(buildProperties("1.2.3")));

        // Act
        OpenAPI openApi = config.employeeServiceOpenApi();

        // Assert
        assertThat(openApi.getInfo().getVersion()).isEqualTo("1.2.3");
    }

    @Test
    @DisplayName("Edge Case: Should fall back to 'unknown' when build info is absent")
    void shouldFallBackWhenBuildInfoMissing() {
        // Arrange
        OpenApiConfig config = new OpenApiConfig(Optional.empty());

        // Act
        OpenAPI openApi = config.employeeServiceOpenApi();

        // Assert
        assertThat(openApi.getInfo().getVersion()).isEqualTo(OpenApiConfig.UNKNOWN_VERSION);
    }

    @Test
    @DisplayName("Happy Path: Should define bearerAuth and apply it globally")
    void shouldDefineAndApplyBearerScheme() {
        // Arrange
        OpenApiConfig config = new OpenApiConfig(Optional.empty());

        // Act
        OpenAPI openApi = config.employeeServiceOpenApi();

        // Assert
        SecurityScheme scheme = openApi.getComponents()
                .getSecuritySchemes()
                .get(OpenApiConfig.BEARER_SCHEME_NAME);

        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("bearer");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
        assertThat(openApi.getSecurity())
                .anySatisfy(requirement -> assertThat(requirement).containsKey(OpenApiConfig.BEARER_SCHEME_NAME));
    }

    private static BuildProperties buildProperties(String version) {
        Properties source = new Properties();
        source.setProperty("version", version);
        return new BuildProperties(source);
    }
}