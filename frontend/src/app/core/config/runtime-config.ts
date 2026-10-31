import {InjectionToken} from '@angular/core';

/**
 * Runtime Keycloak settings required by the browser application.
 */
export interface KeycloakRuntimeConfig {
  readonly url: string;
  readonly realm: string;
  readonly clientId: string;
}

/**
 * Environment-dependent frontend configuration loaded before Angular starts.
 */
export interface RuntimeConfig {
  readonly keycloak: KeycloakRuntimeConfig;
  readonly employeeApiUrl: string;
  readonly customerApiUrl: string;
}

/**
 * Injection token used by Angular services that need runtime configuration.
 */
export const RUNTIME_CONFIG = new InjectionToken<RuntimeConfig>(
  'Application runtime configuration',
);
