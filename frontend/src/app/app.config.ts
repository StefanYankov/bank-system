import {provideHttpClient, withInterceptors,} from '@angular/common/http';
import {ApplicationConfig, provideBrowserGlobalErrorListeners,} from '@angular/core';
import {provideRouter} from '@angular/router';
import {
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  includeBearerTokenInterceptor,
  provideKeycloak,
} from 'keycloak-angular';

import {routes} from './app.routes';
import {provideApi} from './generated/employee-api';
import {createApiUrlPattern} from './core/config/api-url-pattern';
import {RUNTIME_CONFIG, RuntimeConfig,} from './core/config/runtime-config';

/**
 * Creates the Angular application configuration from values loaded at runtime.
 *
 * @param runtimeConfig environment-specific frontend configuration
 * @returns providers used to bootstrap the Angular application
 */
export function createApplicationConfig(
  runtimeConfig: RuntimeConfig,
): ApplicationConfig {
  return {
    providers: [
      provideBrowserGlobalErrorListeners(),

      provideRouter(routes),

      provideHttpClient(
        withInterceptors([
          includeBearerTokenInterceptor,
        ]),
      ),

      provideApi(runtimeConfig.employeeApiUrl),

      {
        provide: RUNTIME_CONFIG,
        useValue: runtimeConfig,
      },

      {
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
