import {InjectionToken} from '@angular/core';

/**
 * Runtime Keycloak settings required by the browser application.
 */
export interface KeycloakConfig {
  readonly url: string;
  readonly realm: string;
  readonly clientId: string;
}

/**
 * Environment-dependent frontend configuration loaded before Angular starts.
 */
export interface AppConfig {
  readonly keycloak: KeycloakConfig;
  readonly employeeApiUrl: string;
}

/**
 * Injection token used by Angular services that need runtime configuration.
 */
export const APP_CONFIG = new InjectionToken<AppConfig>(
  'Application runtime configuration',
);
