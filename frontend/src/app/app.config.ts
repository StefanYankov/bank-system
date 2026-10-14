import {ApplicationConfig, provideBrowserGlobalErrorListeners,} from '@angular/core';
import {provideRouter} from '@angular/router';
import {provideKeycloak} from 'keycloak-angular';

import {routes} from './app.routes';
import {APP_CONFIG, AppConfig,} from './core/config/app-config';

/**
 * Creates the Angular configuration from values loaded at runtime.
 *
 * @param runtimeConfig environment-specific frontend configuration
 * @returns providers used to start the Angular application
 */
export function createApplicationConfig(
  runtimeConfig: AppConfig,
): ApplicationConfig {
  return {
    providers: [
      provideBrowserGlobalErrorListeners(),
      provideRouter(routes),

      {
        provide: APP_CONFIG,
        useValue: runtimeConfig,
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
