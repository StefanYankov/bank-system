package bg.nbu.banksystem.account.integration.customer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * Registers and secures the outbound Customer HTTP service client.
 */
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "customer-service", types = CustomerServiceHttpClient.class)
public class CustomerClientConfig {

    @Bean
    RestClientHttpServiceGroupConfigurer customerTokenRelayConfigurer() {
        return groups -> groups.filterByName("customer-service")
            .forEachClient((group, builder) -> builder.requestInterceptor((request, body, execution) -> {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                    || !jwtAuthentication.isAuthenticated()) {
                    throw new AuthenticationCredentialsNotFoundException(
                        "An authenticated employee JWT is required for Customer lookup");
                }

                request.getHeaders().setBearerAuth(jwtAuthentication.getToken().getTokenValue());
                return execution.execute(request, body);
            }));
    }
}
