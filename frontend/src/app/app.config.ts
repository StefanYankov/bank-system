import {provideHttpClient, withInterceptors,} from '@angular/common/http';
import {ApplicationConfig, provideBrowserGlobalErrorListeners,} from '@angular/core';
import {provideRouter} from '@angular/router';
import {
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  includeBearerTokenInterceptor,
  provideKeycloak,
} from 'keycloak-angular';

import {routes} from './app.routes';
import {RUNTIME_CONFIG, RuntimeConfig,} from './core/config/runtime-config';
import {createApiUrlPattern} from './core/config/api-url-pattern';

/**
 * Creates Angular providers from the runtime configuration.
 *
 * @param runtimeConfig environment-specific frontend configuration
 * @returns providers used to start the Angular application
 */
export function createApplicationConfig(
  runtimeConfig: RuntimeConfig,
): ApplicationConfig {
  return {
    providers: [
      provideBrowserGlobalErrorListeners(),
      provideRouter(routes),

      // [GUIDE] Registers Angular HttpClient and runs outgoing requests through
      // [GUIDE] the Keycloak bearer-token interceptor.
      provideHttpClient(
        withInterceptors([
          includeBearerTokenInterceptor,
        ]),
      ),

      {
        // [GUIDE] Make the validated runtime configuration available through
        // [GUIDE] Angular dependency injection.
        provide: RUNTIME_CONFIG,
        useValue: runtimeConfig,
      },

      {
        // [GUIDE] The interceptor adds a token only when the request URL
        // [GUIDE] matches the configured Employee Service base URL.
        provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
        useValue: [
          {
            urlPattern: createApiUrlPattern(
              runtimeConfig.employeeApiUrl,
            ),
          },
        ],
      },

      provideKeycloak({
        config: runtimeConfig.keycloak,
        initOptions: {
          onLoad: 'check-sso',
          pkceMethod: 'S256',
          silentCheckSsoRedirectUri: new URL(
            'silent-check-sso.html',
            document.baseURI,
          ).toString(),
        },
      }),
    ],
  };
}
